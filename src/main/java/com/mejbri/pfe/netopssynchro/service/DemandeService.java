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

@Service
@RequiredArgsConstructor
public class DemandeService {

    private final DemandeRepository            demandeRepository;
    private final UserRepository               userRepository;
    private final NotificationService          notificationService;
    private final PushNotificationService      pushService;
    private final CityBoundsService            cityBoundsService;
    private final TechnicianLocationRepository technicianLocationRepository;

    // ── Queries ───────────────────────────────────────────────────────────────

    public List<DemandeDTO> getAll() {
        return demandeRepository.findAllByOrderByCreatedAtDesc()
                .stream().map(this::toDTO).toList();
    }

    public DemandeDTO getById(Long id) {
        return toDTO(findOrThrow(id));
    }

    // ── Create ────────────────────────────────────────────────────────────────

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
                .latitude(req.getLatitude())
                .longitude(req.getLongitude())
                .createdBy(creator)
                .build();

        if (req.getTechnicianId() != null) {
            User tech = userRepository.findById(req.getTechnicianId())
                    .orElseThrow(() -> new RuntimeException("Technician not found"));
            enforceCity(demande, tech);
            demande.setTechnician(tech);
            notifyAssigned(tech, demande.getTitle());
        }

        return toDTO(demandeRepository.save(demande));
    }

    // ── Update ────────────────────────────────────────────────────────────────

    public DemandeDTO update(Long id, DemandeRequest req) {
        Demande demande = findOrThrow(id);

        demande.setTitle(req.getTitle());
        demande.setDescription(req.getDescription());
        demande.setPriority(req.getPriority());
        demande.setClientName(req.getClientName());
        demande.setClientContact(req.getClientContact());
        demande.setClientLocation(req.getClientLocation());
        if (req.getLatitude()  != null) demande.setLatitude(req.getLatitude());
        if (req.getLongitude() != null) demande.setLongitude(req.getLongitude());
        if (req.getStatus()    != null) demande.setStatus(req.getStatus());

        if (req.getTechnicianId() != null) {
            User tech = userRepository.findById(req.getTechnicianId())
                    .orElseThrow(() -> new RuntimeException("Technician not found"));
            enforceCity(demande, tech);
            boolean newAssignment = demande.getTechnician() == null
                    || !demande.getTechnician().getId().equals(req.getTechnicianId());
            demande.setTechnician(tech);
            if (newAssignment) notifyAssigned(tech, demande.getTitle());
        } else {
            demande.setTechnician(null);
        }

        return toDTO(demandeRepository.save(demande));
    }

    // ── City enforcement ──────────────────────────────────────────────────────

    /**
     * Throws if the technician's last known city does not match the demande's city.
     * Passes silently if either side has no city information yet (no GPS recorded,
     * or demande has no coordinates), because we can't block work on incomplete data.
     */
    private void enforceCity(Demande demande, User tech) {
        City demandeCity = resolveDemandeCity(demande);
        City techCity    = resolveTechnicianCity(tech);

        if (demandeCity == null || techCity == null) {
            // Not enough location data — allow but log
            return;
        }

        if (demandeCity != techCity) {
            throw new RuntimeException(
                    "Cannot assign: technician " + tech.getUsername() +
                    " is currently in " + techCity +
                    " but this task is located in " + demandeCity + ".");
        }
    }

    private City resolveDemandeCity(Demande d) {
        if (d.getLatitude() == null || d.getLongitude() == null) return null;
        return cityBoundsService.detect(d.getLatitude(), d.getLongitude()).orElse(null);
    }

    private City resolveTechnicianCity(User tech) {
        return technicianLocationRepository
                .findTopByTechnicianIdOrderByRecordedAtDesc(tech.getId())
                .map(TechnicianLocationHistory::getCity)
                .orElse(null);
    }

    // ── Misc ─────────────────────────────────────────────────────────────────

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
                .map(this::toUserDTO)
                .toList();
    }

    public List<UserDTO> getTechniciansByCity(String city) {
        City c = city != null ? City.valueOf(city.toUpperCase()) : null;
        return userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.TECHNICIAN && u.isEnabled())
                .filter(u -> {
                    if (c == null) return true;
                    return technicianLocationRepository
                            .findTopByTechnicianIdOrderByRecordedAtDesc(u.getId())
                            .map(loc -> loc.getCity() == c)
                            .orElse(false);
                })
                .map(this::toUserDTO)
                .toList();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    // ── Random demande generator ──────────────────────────────────────────────

    private static final java.util.Random RNG = new java.util.Random();

    private static final String[][] TEMPLATES = {
        {"Réseau instable - routeur en panne",       "Perte de connectivité signalée. Diagnostic et remplacement du routeur nécessaire."},
        {"Coupure Internet client",                   "Le client rapporte une perte d'accès à Internet depuis ce matin. Vérifier la ligne et les équipements."},
        {"Panne switch backbone",                     "Un switch de cœur de réseau est tombé. Impact sur plusieurs postes de travail."},
        {"Configuration VPN entreprise",             "Mise en place d'un tunnel VPN site-à-site pour interconnecter deux agences."},
        {"Maintenance préventive serveurs",           "Vérification RAID, mise à jour firmware, nettoyage des logs sur les serveurs de production."},
        {"Remplacement NAS défectueux",               "NAS Synology inaccessible. Récupération des données et migration vers nouveau matériel."},
        {"Installation firewall nouvelle agence",     "Déploiement et configuration d'un pare-feu Fortinet pour la nouvelle agence."},
        {"Déploiement WiFi Meraki 3 étages",         "Extension de la couverture WiFi avec des bornes Cisco Meraki sur 3 niveaux."},
        {"Audit sécurité réseau",                    "Analyse des vulnérabilités, scan de ports, rapport de conformité demandé."},
        {"Câblage réseau nouvelle salle serveurs",   "Installation et brassage de câbles Cat6A dans la nouvelle baie serveurs."},
    };

    private static final String[] CLIENTS = {
        "TechCorp", "BancaDigitale", "Clinique Moderne", "Centre Commercial",
        "Hôtel Prestige", "Assurances Nationales", "Université Centrale",
        "Usine Industrielle", "Cabinet Conseil", "Pharmacie Centrale",
    };

    private static final String[] CONTACTS = {
        "+216 71 100 200", "+216 73 200 300", "+216 74 300 400",
        "+216 72 400 500", "+216 75 500 600",
    };

    private static final java.util.Map<String, double[]> CITY_COORDS = java.util.Map.of(
        "TUNIS",    new double[]{36.8190, 10.1658},
        "SOUSSE",   new double[]{35.8256, 10.6369},
        "SFAX",     new double[]{34.7406, 10.7603},
        "MONASTIR", new double[]{35.7643, 10.8113},
        "KAIROUAN", new double[]{35.6781, 10.0963}
    );

    public DemandeDTO generateRandom(String city, Authentication auth) {
        User creator = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String cityKey   = city.toUpperCase();
        double[] center  = CITY_COORDS.getOrDefault(cityKey, CITY_COORDS.get("TUNIS"));
        double   spread  = 0.025;
        double   lat     = center[0] + (RNG.nextDouble() - 0.5) * spread;
        double   lon     = center[1] + (RNG.nextDouble() - 0.5) * spread;

        String[] tpl    = TEMPLATES[RNG.nextInt(TEMPLATES.length)];
        String   client = CLIENTS[RNG.nextInt(CLIENTS.length)] + " " + cityKey;
        String   phone  = CONTACTS[RNG.nextInt(CONTACTS.length)];

        DemandePriority[] priorities = DemandePriority.values();

        Demande demande = Demande.builder()
                .title(tpl[0] + " — " + cityKey)
                .description(tpl[1])
                .priority(priorities[RNG.nextInt(priorities.length)])
                .status(DemandeStatus.NEW)
                .clientName(client)
                .clientContact(phone)
                .clientLocation(cityKey + ", Tunisie")
                .latitude(lat)
                .longitude(lon)
                .createdBy(creator)
                .build();

        return toDTO(demandeRepository.save(demande));
    }

    private void notifyAssigned(User tech, String title) {
        notificationService.push(NotificationType.TASK_ASSIGNED, tech.getUsername());
        pushService.sendToUser(tech, "New Task Assigned", title);
    }

    private Demande findOrThrow(Long id) {
        return demandeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Demande not found: " + id));
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
        dto.setLatitude(d.getLatitude());
        dto.setLongitude(d.getLongitude());
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
}
