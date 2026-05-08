package com.mejbri.pfe.netopssynchro.scheduler;

import com.mejbri.pfe.netopssynchro.entity.*;
import com.mejbri.pfe.netopssynchro.repository.AppLocationRepository;
import com.mejbri.pfe.netopssynchro.repository.DemandeRepository;
import com.mejbri.pfe.netopssynchro.repository.UserRepository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class TaskGeneratorScheduler {

    private final DemandeRepository     demandeRepository;
    private final UserRepository        userRepository;
    private final AppLocationRepository locationRepository;

    private static final Random RNG = new Random();

    // ── Task data pools ───────────────────────────────────────────────────────

    private static final String[][] TEMPLATES = {
        {"Réseau instable - routeur en panne",           "Perte de connectivité signalée. Diagnostic et remplacement du routeur nécessaire."},
        {"Coupure Internet client",                       "Le client rapporte une perte d'accès à Internet depuis ce matin. Vérifier la ligne et les équipements."},
        {"Panne switch backbone",                         "Un switch de cœur de réseau est tombé. Impact sur plusieurs postes de travail."},
        {"Configuration VPN entreprise",                 "Mise en place d'un tunnel VPN site-à-site pour interconnecter deux agences."},
        {"Maintenance préventive serveurs",               "Vérification RAID, mise à jour firmware, nettoyage des logs sur les serveurs de production."},
        {"Remplacement NAS défectueux",                   "NAS Synology inaccessible. Récupération des données et migration vers nouveau matériel."},
        {"Installation firewall nouvelle agence",         "Déploiement et configuration d'un pare-feu Fortinet pour la nouvelle agence."},
        {"Déploiement WiFi Meraki 3 étages",             "Extension de la couverture WiFi avec des bornes Cisco Meraki sur 3 niveaux."},
        {"Audit sécurité réseau",                        "Analyse des vulnérabilités, scan de ports, rapport de conformité demandé."},
        {"Câblage réseau nouvelle salle serveurs",       "Installation et brassage de câbles Cat6A dans la nouvelle baie serveurs."},
        {"Dépannage DNS résolution échouée",             "Les postes clients ne résolvent plus les noms de domaine internes."},
        {"Remplacement onduleur salle machine",          "L'onduleur APC en fin de vie doit être remplacé avant la prochaine coupure EDF."},
        {"Migration VLAN nouvelle infrastructure",       "Reconfiguration des VLANs suite à une restructuration de l'architecture réseau."},
        {"Panne fibre optique inter-sites",              "La liaison fibre entre le siège et l'entrepôt est coupée. Intervention urgente."},
        {"Installation NVR vidéosurveillance",           "Mise en place d'un système de vidéosurveillance IP avec enregistrement centralisé."},
        {"Dépannage imprimante réseau",                  "L'imprimante réseau partagée est inaccessible depuis les postes du service RH."},
        {"Configuration QoS pour VoIP",                  "Priorisation du trafic voix sur IP pour garantir la qualité des appels."},
        {"Mise à jour firmware équipements réseau",      "Déploiement des dernières versions de firmware sur les switchs et routeurs."},
        {"Remplacement baie de brassage",                "La baie de brassage principale est saturée. Remplacement par un modèle 48 ports."},
        {"Diagnostic pannes intermittentes",             "Des coupures réseau aléatoires surviennent sans cause identifiée. Investigation requise."},
    };

    private static final String[] CLIENTS = {
        "TechCorp", "BancaDigitale", "Clinique Moderne", "Centre Commercial",
        "Hôtel Prestige", "Assurances Nationales", "Université Centrale",
        "Usine Industrielle", "Cabinet Conseil", "Pharmacie Centrale",
        "Studio Production", "Agence Immobilière", "Réseau GMS", "Data Center Pro",
        "Ministère Numérique",
    };

    private static final String[] CONTACTS = {
        "+216 71 100 200", "+216 73 200 300", "+216 74 300 400",
        "+216 72 400 500", "+216 75 500 600", "+216 76 600 700",
        "+216 70 700 800", "+216 71 800 900", "+216 73 900 100",
    };

    // ── Scheduler ─────────────────────────────────────────────────────────────

    /**
     * Generates one random demande every 2 minutes across all cities that have
     * registered AppLocation bounds in the database.
     *
     * Override the interval in application.properties:
     *   task.generator.cron=0 *\/2 * * * *
     */
    @Scheduled(cron = "${task.generator.cron:0 */2 * * * *}")
    public void generate() {
        try {
            Demande demande = buildDemande();
            demandeRepository.save(demande);
            log.info("Auto-generated task [{}] in city {}  →  {}",
                    demande.getId(), cityOf(demande), demande.getTitle());
        } catch (Exception e) {
            log.error("Task generator failed: {}", e.getMessage());
        }
    }

    // ── Builder ───────────────────────────────────────────────────────────────

    private Demande buildDemande() {
        // Pick a random city that has bounds registered in the DB
        List<AppLocation> centers = locationRepository.findByTypeAndActiveTrue(LocationType.CITY_CENTER);
        AppLocation center = centers.isEmpty()
                ? fallbackCenter()
                : centers.get(RNG.nextInt(centers.size()));

        double[] coords  = randomCoords(center);
        String[] tpl     = TEMPLATES[RNG.nextInt(TEMPLATES.length)];
        String   client  = CLIENTS[RNG.nextInt(CLIENTS.length)] + " " + cityLabel(center.getCity());
        String   contact = CONTACTS[RNG.nextInt(CONTACTS.length)];

        // Find a system/admin user to set as creator (use first admin found, or null)
        User creator = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.ADMIN && u.isEnabled())
                .findFirst()
                .orElse(null);

        return Demande.builder()
                .title(tpl[0] + " — " + cityLabel(center.getCity()))
                .description(tpl[1])
                .priority(randomPriority())
                .status(DemandeStatus.NEW)
                .clientName(client)
                .clientContact(contact)
                .clientLocation(cityLabel(center.getCity()) + ", Tunisie")
                .latitude(coords[0])
                .longitude(coords[1])
                .createdBy(creator)
                .build();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Adds a small random offset to the city center coordinates so tasks
     * don't all land on exactly the same point.
     * Spread radius ≈ ±3 km.
     */
    private double[] randomCoords(AppLocation center) {
        double spread = 0.025; // ~2.5 km in degrees
        double lat = center.getLatitude() + (RNG.nextDouble() - 0.5) * spread;
        double lon = center.getLongitude() + (RNG.nextDouble() - 0.5) * spread;
        return new double[]{lat, lon};
    }

    private DemandePriority randomPriority() {
        // Weighted: LOW 20%, MEDIUM 40%, HIGH 30%, CRITICAL 10%
        int r = RNG.nextInt(100);
        if (r < 20)  return DemandePriority.LOW;
        if (r < 60)  return DemandePriority.MEDIUM;
        if (r < 90)  return DemandePriority.HIGH;
        return DemandePriority.CRITICAL;
    }

    /** Hard-coded fallback if no AppLocation rows exist yet in the DB. */
    private AppLocation fallbackCenter() {
        AppLocation loc = new AppLocation();
        loc.setCity(City.TUNIS);
        loc.setLatitude(36.8190);
        loc.setLongitude(10.1658);
        return loc;
    }

    private String cityLabel(City city) {
        return switch (city) {
            case TUNIS    -> "Tunis";
            case SOUSSE   -> "Sousse";
            case SFAX     -> "Sfax";
            case MONASTIR -> "Monastir";
            case KAIROUAN -> "Kairouan";
        };
    }

    private String cityOf(Demande d) {
        if (d.getLatitude() == null) return "unknown";
        for (AppLocation loc : locationRepository.findByTypeAndActiveTrue(LocationType.CITY_CENTER)) {
            if (loc.getMinLat() == null) continue;
            if (d.getLatitude()  >= loc.getMinLat() && d.getLatitude()  <= loc.getMaxLat()
             && d.getLongitude() >= loc.getMinLng() && d.getLongitude() <= loc.getMaxLng())
                return loc.getCity().name();
        }
        return "?";
    }
}
