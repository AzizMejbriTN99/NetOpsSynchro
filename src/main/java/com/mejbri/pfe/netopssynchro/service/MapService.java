package com.mejbri.pfe.netopssynchro.service;

import com.mejbri.pfe.netopssynchro.dto.*;
import com.mejbri.pfe.netopssynchro.entity.*;
import com.mejbri.pfe.netopssynchro.repository.*;
import com.mejbri.pfe.netopssynchro.repository.UserRepository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MapService {

    private final AppLocationRepository locationRepo;
    private final TechnicianLocationRepository techLocRepo;
    private final DemandeRepository demandeRepo;
    private final DemandeActionRepository actionRepo;
    private final CityBoundsService cityBoundsService;
    private final GeocodeService geocodeService;
    private final UserRepository userRepository;

    public List<AppLocationDTO> getLocations(City city, LocationType type) {
        if (city != null && type != null)
            return locationRepo.findByCityAndTypeAndActiveTrue(city, type)
                    .stream().map(this::toLocationDTO).toList();
        if (city != null)
            return locationRepo.findByCityAndActiveTrue(city)
                    .stream().map(this::toLocationDTO).toList();
        if (type != null)
            return locationRepo.findByTypeAndActiveTrue(type)
                    .stream().map(this::toLocationDTO).toList();
        return locationRepo.findByActiveTrue()
                .stream().map(this::toLocationDTO).toList();
    }

    public AppLocationDTO createLocation(AppLocationDTO req) {
        AppLocation loc = AppLocation.builder()
                .name(req.getName())
                .description(req.getDescription())
                .city(req.getCity())
                .type(req.getType())
                .latitude(req.getLatitude())
                .longitude(req.getLongitude())
                .active(true)
                .build();
        return toLocationDTO(locationRepo.save(loc));
    }

    public void deleteLocation(Long id) {
        locationRepo.deleteById(id);
    }

    public List<TechnicianLocationDTO> getLatestTechnicianLocations(City city) {
        return techLocRepo.findLatestPerTechnician().stream()
                .filter(t -> city == null || t.getCity() == city)
                .map(this::toTechDTO).toList();
    }

    public List<DemandeMapDTO> getDemandeLocations(City city) {
        return demandeRepo.findAllByOrderByCreatedAtDesc().stream()
                .filter(d -> d.getStatus() != DemandeStatus.CLOSED)
                .map(d -> toDemandeMapDTO(d, city))
                .filter(d -> d != null)
                .toList();
    }

    public void updateTechnicianLocationByUsername(String username, double lat, double lng) {
        User tech = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Technician not found"));
        City city = cityBoundsService.detect(lat, lng).orElse(null);
        TechnicianLocationHistory loc = TechnicianLocationHistory.builder()
                .technician(tech)
                .latitude(lat)
                .longitude(lng)
                .city(city)
                .build();
        techLocRepo.save(loc);
    }

    private DemandeMapDTO toDemandeMapDTO(Demande d, City filterCity) {
        double lat, lng;

        if (d.getLatitude() != null && d.getLongitude() != null) {
            lat = d.getLatitude();
            lng = d.getLongitude();
        } else if (d.getClientLocation() != null && !d.getClientLocation().isBlank()) {
            double[] coords = geocodeService.geocode(d.getClientLocation());
            if (coords == null) return null;
            lat = coords[0];
            lng = coords[1];
        } else {
            return null;
        }

        City city = cityBoundsService.detect(lat, lng).orElse(null);
        if (filterCity != null && city != filterCity) return null;

        // get latest action
        String latestAction = actionRepo
                .findTopByDemandeIdOrderByPerformedAtDesc(d.getId())
                .map(a -> a.getStatus().name())
                .orElse(null);

        DemandeMapDTO dto = new DemandeMapDTO();
        dto.setId(d.getId());
        dto.setTitle(d.getTitle());
        dto.setClientName(d.getClientName());
        dto.setClientLocation(d.getClientLocation());
        dto.setStatus(d.getStatus());
        dto.setPriority(d.getPriority());
        dto.setLatitude(lat);
        dto.setLongitude(lng);
        dto.setLatestAction(latestAction);
        if (d.getTechnician() != null) {
            dto.setTechnicianId(d.getTechnician().getId());
            dto.setTechnicianUsername(d.getTechnician().getUsername());
        }
        return dto;
    }

    private AppLocationDTO toLocationDTO(AppLocation l) {
        AppLocationDTO dto = new AppLocationDTO();
        dto.setId(l.getId());
        dto.setName(l.getName());
        dto.setDescription(l.getDescription());
        dto.setCity(l.getCity());
        dto.setType(l.getType());
        dto.setLatitude(l.getLatitude());
        dto.setLongitude(l.getLongitude());
        dto.setActive(l.isActive());
        return dto;
    }

    private TechnicianLocationDTO toTechDTO(TechnicianLocationHistory t) {
        TechnicianLocationDTO dto = new TechnicianLocationDTO();
        dto.setTechnicianId(t.getTechnician().getId());
        dto.setUsername(t.getTechnician().getUsername());
        dto.setLatitude(t.getLatitude());
        dto.setLongitude(t.getLongitude());
        dto.setCity(t.getCity());
        dto.setRecordedAt(t.getRecordedAt());
        return dto;
    }
}