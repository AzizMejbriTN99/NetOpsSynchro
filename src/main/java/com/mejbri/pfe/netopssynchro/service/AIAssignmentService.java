package com.mejbri.pfe.netopssynchro.service;

import com.mejbri.pfe.netopssynchro.dto.AssignmentCandidateDTO;
import com.mejbri.pfe.netopssynchro.dto.AssignmentResultDTO;
import com.mejbri.pfe.netopssynchro.entity.*;
import com.mejbri.pfe.netopssynchro.repository.DemandeRepository;
import com.mejbri.pfe.netopssynchro.repository.TechnicianLocationRepository;
import com.mejbri.pfe.netopssynchro.repository.TechnicianResourceRepository;
import com.mejbri.pfe.netopssynchro.repository.UserRepository.UserRepository;
import com.mejbri.pfe.netopssynchro.util.GeoUtils;
import com.mejbri.pfe.netopssynchro.util.HungarianAlgorithm;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIAssignmentService {

    private final DemandeRepository            demandeRepo;
    private final UserRepository               userRepo;
    private final TechnicianLocationRepository locationRepo;
    private final TechnicianResourceRepository resourceRepo;
    private final NotificationService          notificationService;
    private final PushNotificationService      pushService;
    private final CityBoundsService            cityBoundsService;

    @Value("${ai.depot.latitude:36.8190}")
    private double depotLat;

    @Value("${ai.depot.longitude:10.1658}")
    private double depotLon;

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Compute the globally optimal technician → demande assignment using the
     * Hungarian algorithm, filtered by city so each tech only gets tasks in
     * their own city. Optionally persists the assignments.
     *
     * City resolution order for technicians:
     *   1. User.city (profile field set by admin)
     *   2. Latest GPS location mapped via CityBoundsService
     *   3. Latest TechnicianLocationHistory.city
     *   4. null → treated as city-agnostic (eligible for any demande)
     */
    @Transactional
    public AssignmentResultDTO computeAndOptionallyApply(boolean autoApply) {

        // 1. Collect unassigned NEW demandes that have GPS coordinates
        List<Demande> openDemandes = demandeRepo.findByStatus(DemandeStatus.NEW)
                .stream()
                .filter(d -> d.getTechnician() == null)
                .filter(d -> d.getLatitude() != null && d.getLongitude() != null)
                .toList();

        if (openDemandes.isEmpty())
            return AssignmentResultDTO.builder()
                    .assignments(List.of())
                    .summary("No unassigned demandes with GPS coordinates found.")
                    .totalOptimalTimeMinutes(0)
                    .autoApplied(false)
                    .build();

        // 2. Collect available (not IN_PROGRESS) technicians
        Set<Long> busyTechnicianIds = demandeRepo.findByStatus(DemandeStatus.IN_PROGRESS)
                .stream()
                .filter(d -> d.getTechnician() != null)
                .map(d -> d.getTechnician().getId())
                .collect(Collectors.toSet());

        List<User> availableTechs = userRepo.findAll().stream()
                .filter(u -> u.getRole() == Role.TECHNICIAN && u.isEnabled())
                .filter(u -> !busyTechnicianIds.contains(u.getId()))
                .toList();

        if (availableTechs.isEmpty())
            return AssignmentResultDTO.builder()
                    .assignments(List.of())
                    .summary("No available technicians at this time.")
                    .totalOptimalTimeMinutes(0)
                    .autoApplied(false)
                    .build();

        // 3. Pre-fetch location data and resource counts
        Map<Long, double[]> techPositions   = latestPositions(availableTechs);
        Map<Long, Integer>  techResourceCnt = resourceCounts(availableTechs);
        Map<Long, City>     techCities      = resolveTechnicianCities(availableTechs, techPositions);

        // 4. Group demandes and techs by city
        Map<City, List<Demande>> demandesByCity = groupDemandesByCity(openDemandes);
        Map<City, List<User>>    techsByCity    = groupTechsByCity(availableTechs, techCities);

        // City-unknown techs are eligible for any city
        List<User> cityUnknownTechs = availableTechs.stream()
                .filter(u -> !techCities.containsKey(u.getId()))
                .toList();

        List<AssignmentCandidateDTO> candidates = new ArrayList<>();
        double totalTime = 0;

        // Track which techs have already been assigned (city-unknown techs can only be used once)
        Set<Long> assignedTechIds = new HashSet<>();

        // 5. Process each city independently
        for (Map.Entry<City, List<Demande>> entry : demandesByCity.entrySet()) {
            City city = entry.getKey();
            List<Demande> cityDemandes = entry.getValue();

            List<User> cityTechs = new ArrayList<>(techsByCity.getOrDefault(city, List.of()));
            // Add city-unknown techs that haven't been used yet
            for (User u : cityUnknownTechs) {
                if (!assignedTechIds.contains(u.getId())) cityTechs.add(u);
            }
            if (cityTechs.isEmpty()) continue;

            int T = cityTechs.size();
            int D = cityDemandes.size();
            double[][] cost = new double[T][D];

            for (int t = 0; t < T; t++) {
                User tech = cityTechs.get(t);
                double[] pos = techPositions.getOrDefault(tech.getId(), new double[]{depotLat, depotLon});
                int rCount   = techResourceCnt.getOrDefault(tech.getId(), 0);
                for (int d = 0; d < D; d++) {
                    Demande dem = cityDemandes.get(d);
                    cost[t][d] = estimatedCost(pos[0], pos[1], rCount,
                            dem.getLatitude(), dem.getLongitude(), dem.getPriority());
                }
            }

            int[] assignment = HungarianAlgorithm.solve(cost);

            for (int t = 0; t < T; t++) {
                int di = assignment[t];
                if (di < 0 || di >= D) continue;

                User    tech    = cityTechs.get(t);
                Demande demande = cityDemandes.get(di);
                double[] pos    = techPositions.getOrDefault(tech.getId(), new double[]{depotLat, depotLon});
                int rCount      = techResourceCnt.getOrDefault(tech.getId(), 0);

                boolean needsDepot = rCount == 0;
                double  depotKm    = needsDepot ? GeoUtils.distanceKm(pos[0], pos[1], depotLat, depotLon) : 0;
                double  startLat   = needsDepot ? depotLat : pos[0];
                double  startLon   = needsDepot ? depotLon : pos[1];
                double  directKm   = GeoUtils.distanceKm(startLat, startLon,
                        demande.getLatitude(), demande.getLongitude());
                double  totalKm    = depotKm + directKm;
                double  minutes    = GeoUtils.travelMinutes(totalKm);
                totalTime += minutes;

                String reasoning = buildReasoning(tech.getUsername(), rCount, needsDepot,
                        depotKm, directKm, minutes, demande.getPriority(), city);

                candidates.add(AssignmentCandidateDTO.builder()
                        .technicianId(tech.getId())
                        .technicianUsername(tech.getUsername())
                        .demandeId(demande.getId())
                        .demandeTitle(demande.getTitle())
                        .directDistanceKm(directKm)
                        .estimatedMinutes(minutes)
                        .needsDepotStop(needsDepot)
                        .depotDistanceKm(depotKm)
                        .totalDistanceKm(totalKm)
                        .reasoning(reasoning)
                        .build());

                assignedTechIds.add(tech.getId());

                if (autoApply) {
                    demande.setTechnician(tech);
                    demande.setStatus(DemandeStatus.NEW);
                    demandeRepo.save(demande);
                    notificationService.push(NotificationType.TASK_ASSIGNED, tech.getUsername());
                    pushService.sendToUser(tech,
                            "New Task Assigned",
                            "You have been assigned: " + demande.getTitle());
                    log.info("[AI] Assigned {} (city={}) → demande #{} ({})",
                            tech.getUsername(), city, demande.getId(), demande.getTitle());
                }
            }
        }

        String summary = String.format(
                "Optimally matched %d technician(s) to %d task(s) across %d city/cities. " +
                        "Total estimated travel time: %.1f min.",
                candidates.size(), openDemandes.size(), demandesByCity.size(), totalTime);

        return AssignmentResultDTO.builder()
                .assignments(candidates)
                .summary(summary)
                .totalOptimalTimeMinutes(totalTime)
                .autoApplied(autoApply)
                .build();
    }

    // ── City helpers ──────────────────────────────────────────────────────────

    /**
     * Resolves each technician's city:
     *   1. User.city (admin-set profile field) — preferred
     *   2. Latest GPS position via CityBoundsService
     *   3. TechnicianLocationHistory.city
     */
    private Map<Long, City> resolveTechnicianCities(List<User> techs,
                                                    Map<Long, double[]> positions) {
        Map<Long, City> map = new HashMap<>();
        for (User tech : techs) {
            City city = tech.getCity();
            if (city == null) {
                double[] pos = positions.get(tech.getId());
                if (pos != null) {
                    city = cityBoundsService.detect(pos[0], pos[1]).orElse(null);
                }
            }
            if (city == null) {
                city = locationRepo.findTopByTechnicianIdOrderByRecordedAtDesc(tech.getId())
                        .map(TechnicianLocationHistory::getCity)
                        .orElse(null);
            }
            if (city != null) map.put(tech.getId(), city);
        }
        return map;
    }

    private Map<City, List<Demande>> groupDemandesByCity(List<Demande> demandes) {
        Map<City, List<Demande>> map = new HashMap<>();
        for (Demande d : demandes) {
            cityBoundsService.detect(d.getLatitude(), d.getLongitude())
                    .ifPresent(city -> map.computeIfAbsent(city, k -> new ArrayList<>()).add(d));
        }
        return map;
    }

    private Map<City, List<User>> groupTechsByCity(List<User> techs,
                                                   Map<Long, City> techCities) {
        Map<City, List<User>> map = new HashMap<>();
        for (User tech : techs) {
            City city = techCities.get(tech.getId());
            if (city != null) map.computeIfAbsent(city, k -> new ArrayList<>()).add(tech);
        }
        return map;
    }

    // ── Cost estimation ───────────────────────────────────────────────────────

    private double estimatedCost(double techLat, double techLon, int resourceCount,
                                 double demLat, double demLon, DemandePriority priority) {
        boolean needsDepot = resourceCount == 0;
        double startLat    = needsDepot ? depotLat : techLat;
        double startLon    = needsDepot ? depotLon : techLon;
        double depotKm     = needsDepot ? GeoUtils.distanceKm(techLat, techLon, depotLat, depotLon) : 0;
        double directKm    = GeoUtils.distanceKm(startLat, startLon, demLat, demLon);
        double minutes     = GeoUtils.travelMinutes(depotKm + directKm);

        double multiplier = switch (priority) {
            case CRITICAL -> 0.60;
            case HIGH     -> 0.80;
            case MEDIUM   -> 1.00;
            case LOW      -> 1.20;
        };
        return minutes * multiplier;
    }

    private Map<Long, double[]> latestPositions(List<User> techs) {
        Map<Long, double[]> map = new HashMap<>();
        for (User t : techs) {
            locationRepo.findTopByTechnicianIdOrderByRecordedAtDesc(t.getId())
                    .ifPresent(loc -> map.put(t.getId(),
                            new double[]{loc.getLatitude(), loc.getLongitude()}));
        }
        return map;
    }

    private Map<Long, Integer> resourceCounts(List<User> techs) {
        Map<Long, Integer> map = new HashMap<>();
        for (User t : techs) {
            int cnt = resourceRepo.findByTechnicianId(t.getId()).size();
            map.put(t.getId(), cnt);
        }
        return map;
    }

    private String buildReasoning(String username, int rCount, boolean needsDepot,
                                  double depotKm, double directKm, double minutes,
                                  DemandePriority priority, City city) {
        StringBuilder sb = new StringBuilder();
        sb.append(username).append(" selected by global optimisation. ");
        sb.append("City match: ").append(city).append(". ");
        if (needsDepot)
            sb.append(String.format("Has no equipment → depot stop required (+%.1f km). ", depotKm));
        else
            sb.append(String.format("Carries %d resource item(s) → direct route. ", rCount));
        sb.append(String.format("%.1f km to site → ~%.0f min ETA. ", directKm, minutes));
        sb.append("Priority: ").append(priority).append(". ");
        sb.append("Assignment minimises total fleet travel time.");
        return sb.toString();
    }
}