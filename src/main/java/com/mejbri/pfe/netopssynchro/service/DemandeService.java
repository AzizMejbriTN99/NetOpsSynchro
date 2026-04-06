package com.mejbri.pfe.netopssynchro.service;

import com.mejbri.pfe.netopssynchro.dto.DemandeDTO;
import com.mejbri.pfe.netopssynchro.dto.DemandeRequest;
import com.mejbri.pfe.netopssynchro.dto.UserDTO;
import com.mejbri.pfe.netopssynchro.entity.*;
import com.mejbri.pfe.netopssynchro.repository.DemandeRepository;
import com.mejbri.pfe.netopssynchro.repository.TechnicianLocationRepository;
import com.mejbri.pfe.netopssynchro.repository.UserRepository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class DemandeService {

    private final DemandeRepository demandeRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final CityBoundsService cityBoundsService;
    private final TechnicianLocationRepository technicianLocationRepository;

    public List<DemandeDTO> getAll() {
        return demandeRepository.findAllByOrderByCreatedAtDesc()
                .stream().map(this::toDTO).toList();
    }

    public DemandeDTO getById(Long id) {
        return toDTO(findOrThrow(id));
    }

    public DemandeDTO create(DemandeRequest req, Authentication auth) {
        User creator = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Demande demande = Demande.builder()
                .title(req.getTitle())
                .description(req.getDescription())
                .priority(req.getPriority() != null ? req.getPriority() : DemandePriority.MEDIUM)
                .status(DemandeStatus.NEW)
                .clientName(req.getClientName())
                .clientContact(req.getClientContact())
                .clientLocation(req.getClientLocation())
                .createdBy(creator)
                .build();

        if (req.getTechnicianId() != null) {
            User tech = userRepository.findById(req.getTechnicianId())
                    .orElseThrow(() -> new RuntimeException("Technician not found"));
            demande.setTechnician(tech);
            notificationService.push(NotificationType.TASK_ASSIGNED, tech.getUsername());
        }

        return toDTO(demandeRepository.save(demande));
    }

    public DemandeDTO update(Long id, DemandeRequest req) {
        Demande demande = findOrThrow(id);

        demande.setTitle(req.getTitle());
        demande.setDescription(req.getDescription());
        demande.setPriority(req.getPriority());
        demande.setClientName(req.getClientName());
        demande.setClientContact(req.getClientContact());
        demande.setClientLocation(req.getClientLocation());
        if (req.getStatus() != null) demande.setStatus(req.getStatus());

        if (req.getTechnicianId() != null) {
            User tech = userRepository.findById(req.getTechnicianId())
                    .orElseThrow(() -> new RuntimeException("Technician not found"));

            City demandeCity = detectDemandeCity(demande);
            City techCity    = getTechnicianCity(tech);

            if (demandeCity != null && techCity != null && demandeCity != techCity) {
                throw new RuntimeException(
                        "Technician is in " + techCity + " but demande is in " + demandeCity +
                                ". Cross-city assignment is not allowed.");
            }

            boolean isNewAssignment = demande.getTechnician() == null
                    || !demande.getTechnician().getId().equals(req.getTechnicianId());
            demande.setTechnician(tech);
            if (isNewAssignment)
                notificationService.push(NotificationType.TASK_ASSIGNED, tech.getUsername());
        } else {
            demande.setTechnician(null);
        }

        return toDTO(demandeRepository.save(demande));
    }

    private City detectDemandeCity(Demande d) {
        if (d.getLatitude() != null && d.getLongitude() != null)
            return cityBoundsService.detect(d.getLatitude(), d.getLongitude()).orElse(null);
        return null;
    }

    private City getTechnicianCity(User tech) {
        return technicianLocationRepository
                .findTopByTechnicianIdOrderByRecordedAtDesc(tech.getId())
                .map(TechnicianLocationHistory::getCity)
                .orElse(null);
    }

    public DemandeDTO updateStatus(Long id, DemandeStatus status) {
        Demande demande = findOrThrow(id);
        demande.setStatus(status);
        return toDTO(demandeRepository.save(demande));
    }

    public void delete(Long id) {
        demandeRepository.deleteById(id);
    }

    public List<UserDTO> getTechnicians() {
        return userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.TECHNICIAN && u.isEnabled())
                .map(u -> {
                    UserDTO dto = new UserDTO();
                    dto.setId(u.getId());
                    dto.setUsername(u.getUsername());
                    dto.setEmail(u.getEmail());
                    dto.setRole(u.getRole());
                    dto.setEnabled(u.isEnabled());
                    return dto;
                }).toList();
    }

    public List<UserDTO> getTechniciansByCity(String city) {
        City c = city != null ? City.valueOf(city.toUpperCase()) : null;

        return userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.TECHNICIAN && u.isEnabled())
                .filter(u -> {
                    if (c == null) return true;
                    // check if technician's last known location is in this city
                    return technicianLocationRepository
                            .findTopByTechnicianIdOrderByRecordedAtDesc(u.getId())
                            .map(loc -> loc.getCity() == c)
                            .orElse(false);
                })
                .map(this::toUserDTO)
                .toList();
    }

    private Demande findOrThrow(Long id) {
        return demandeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Demande not found"));
    }

    private UserDTO toUserDTO(User u) {
        UserDTO dto = new UserDTO();
        dto.setId(u.getId());
        dto.setUsername(u.getUsername());
        dto.setEmail(u.getEmail());
        dto.setRole(u.getRole());
        dto.setEnabled(u.isEnabled());
        return dto;
    }

    private DemandeDTO toDTO(Demande d) {
        DemandeDTO dto = new DemandeDTO();
        dto.setId(d.getId());
        dto.setTitle(d.getTitle());
        dto.setDescription(d.getDescription());
        dto.setPriority(d.getPriority());
        dto.setStatus(d.getStatus());
        dto.setClientName(d.getClientName());
        dto.setClientContact(d.getClientContact());
        dto.setClientLocation(d.getClientLocation());
        dto.setCreatedAt(d.getCreatedAt());
        dto.setUpdatedAt(d.getUpdatedAt());
        if (d.getTechnician() != null) {
            dto.setTechnicianId(d.getTechnician().getId());
            dto.setTechnicianUsername(d.getTechnician().getUsername());
        }
        if (d.getCreatedBy() != null)
            dto.setCreatedByUsername(d.getCreatedBy().getUsername());
        return dto;
    }

    private static final String[] TITLES = {
            "Panne réseau", "Maintenance serveur", "Installation firewall",
            "Câblage réseau", "Dépannage VPN", "Configuration WiFi",
            "Audit sécurité", "Remplacement switch", "Panne onduleur",
            "Migration données", "Installation NAS", "Dépannage DNS"
    };

    private static final String[] CLIENTS = {
            "Société Tunisienne Tech", "Cabinet Conseil Digital", "Hôtel Grand Tunis",
            "Clinique Moderne", "Banque Régionale", "Assurances Nationales",
            "Université Centrale", "Centre Commercial", "Usine Industrielle", "Pharmacie Centrale"
    };

    private static final Map<String, double[][]> CITY_COORDS = Map.of(
            "TUNIS",    new double[][]{{36.81, 10.18}, {36.83, 10.22}, {36.79, 10.17}},
            "SOUSSE",   new double[][]{{35.82, 10.63}, {35.83, 10.64}, {35.81, 10.62}},
            "SFAX",     new double[][]{{34.74, 10.76}, {34.75, 10.77}, {34.73, 10.75}},
            "MONASTIR", new double[][]{{35.76, 10.81}, {35.77, 10.82}, {35.75, 10.80}},
            "KAIROUAN", new double[][]{{35.68, 10.09}, {35.69, 10.10}, {35.67, 10.08}}
    );

    public DemandeDTO generateRandom(String city, Authentication auth) {
        User creator = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Random rnd = new Random();
        String title  = TITLES[rnd.nextInt(TITLES.length)];
        String client = CLIENTS[rnd.nextInt(CLIENTS.length)];

        double[][] coords = CITY_COORDS.getOrDefault(city.toUpperCase(),
                CITY_COORDS.get("TUNIS"));
        double[] coord = coords[rnd.nextInt(coords.length)];

        DemandePriority[] priorities = DemandePriority.values();

        Demande demande = Demande.builder()
                .title(title + " - " + city)
                .description("Demande générée automatiquement pour simulation.")
                .priority(priorities[rnd.nextInt(priorities.length)])
                .status(DemandeStatus.NEW)
                .clientName(client + " " + city)
                .clientContact("+216 7" + rnd.nextInt(9) + " " +
                        (100000 + rnd.nextInt(899999)))
                .clientLocation(city + ", Tunisie")
                .latitude(coord[0] + (rnd.nextDouble() - 0.5) * 0.01)
                .longitude(coord[1] + (rnd.nextDouble() - 0.5) * 0.01)
                .createdBy(creator)
                .build();

        Demande saved = demandeRepository.save(demande);
        notificationService.push(NotificationType.TICKET_ASSIGNED,
                "Auto-generated for " + city);
        return toDTO(saved);
    }
}
