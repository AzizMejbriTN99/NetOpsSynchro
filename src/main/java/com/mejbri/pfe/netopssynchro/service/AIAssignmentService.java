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

    private final DemandeRepository              demandeRepo;
    private final UserRepository                 userRepo;
    private final TechnicianLocationRepository   locationRepo;
    private final TechnicianResourceRepository   resourceRepo;
    private final NotificationService            notificationService;
    private final PushNotificationService        pushService;

    @Value("${ai.depot.latitude:36.8190}")
    private double depotLat;

    @Value("${ai.depot.longitude:10.1658}")
    private double depotLon;

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Compute the globally optimal technician → demande assignment using the
     * Hungarian algorithm, then optionally apply it to the database.
     *
     * @param autoApply if true, actually persists the assignments and sends notifications
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

        // 2. Collect available technicians (enabled, no active IN_PROGRESS task)
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

        // 3. Build cost matrix [tech][demande] = estimated travel minutes
        int T = availableTechs.size();
        int D = openDemandes.size();
        double[][] cost = new double[T][D];

        // Pre-fetch latest locations and resource counts
        Map<Long, double[]> techPositions   = latestPositions(availableTechs);
        Map<Long, Integer>  techResourceCnt = resourceCounts(availableTechs);

        for (int t = 0; t < T; t++) {
            User tech = availableTechs.get(t);
            double[] pos = techPositions.getOrDefault(tech.getId(), new double[]{depotLat, depotLon});
            int rCount   = techResourceCnt.getOrDefault(tech.getId(), 0);

            for (int d = 0; d < D; d++) {
                Demande dem = openDemandes.get(d);
                cost[t][d] = estimatedCost(pos[0], pos[1], rCount,
                        dem.getLatitude(), dem.getLongitude(),
                        dem.getPriority());
            }
        }

        // 4. Run Hungarian algorithm for globally optimal assignment
        int[] assignment = HungarianAlgorithm.solve(cost);

        // 5. Build result candidates
        List<AssignmentCandidateDTO> candidates = new ArrayList<>();
        double totalTime = 0;

        for (int t = 0; t < T; t++) {
            int di = assignment[t];
            if (di < 0 || di >= D) continue; // no task for this tech

            User   tech   = availableTechs.get(t);
            Demande demande = openDemandes.get(di);
            double[]  pos   = techPositions.getOrDefault(tech.getId(), new double[]{depotLat, depotLon});
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
                    depotKm, directKm, minutes, demande.getPriority());

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

            // 6. Apply if requested
            if (autoApply) {
                demande.setTechnician(tech);
                demande.setStatus(DemandeStatus.NEW); // keeps NEW until tech starts it
                demandeRepo.save(demande);
                notificationService.push(NotificationType.TASK_ASSIGNED, tech.getUsername());
                pushService.sendToUser(tech,
                        "New Task Assigned",
                        "You have been assigned: " + demande.getTitle());
            }
        }

        String summary = String.format(
                "Optimally matched %d technician(s) to %d task(s). " +
                        "Total estimated travel time: %.1f min.",
                candidates.size(), openDemandes.size(), totalTime);

        return AssignmentResultDTO.builder()
                .assignments(candidates)
                .summary(summary)
                .totalOptimalTimeMinutes(totalTime)
                .autoApplied(autoApply)
                .build();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Cost = travel time in minutes.
     * Priority multiplier: CRITICAL tasks get a 0.7× cost so they are
     * preferred in the optimisation (penalises leaving them unassigned longer).
     */
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
                                  DemandePriority priority) {
        StringBuilder sb = new StringBuilder();
        sb.append(username).append(" selected by global optimisation. ");
        if (needsDepot)
            sb.append(String.format("Has no equipment logged → depot stop required (+%.1f km). ", depotKm));
        else
            sb.append(String.format("Carries %d resource item(s) → direct route. ", rCount));
        sb.append(String.format("%.1f km to site → ~%.0f min ETA. ", directKm, minutes));
        sb.append("Priority: ").append(priority).append(". ");
        sb.append("Assignment chosen because this pairing minimises total fleet travel time.");
        return sb.toString();
    }
}
