package com.mejbri.pfe.netopssynchro.config;

import com.mejbri.pfe.netopssynchro.entity.*;
import com.mejbri.pfe.netopssynchro.repository.*;
import com.mejbri.pfe.netopssynchro.repository.UserRepository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppLocationRepository locationRepo;
    private final DemandeRepository demandeRepo;
    private final DemandeActionRepository actionRepo;
    private final TechnicianLocationRepository techLocRepo;

    @Override
    public void run(ApplicationArguments args) {
        seedUsers();
        seedLocations();
        seedDemandes();
    }


    private void seedUsers() {
        if (userRepository.existsByUsername("admin")) return;

        User admin = User.builder()
                .username("admin")
                .password(passwordEncoder.encode("admin1234"))
                .email("admin@netops.int")
                .role(Role.ADMIN)
                .enabled(true)
                .build();

        User consultant = User.builder()
                .username("cons")
                .password(passwordEncoder.encode("cons1234"))
                .email("consultant1@netops.int")
                .role(Role.CONSULTANT)
                .enabled(true)
                .build();

        User tech1 = User.builder()
                .username("tech.tunis")
                .password(passwordEncoder.encode("tech1234"))
                .email("tech.tunis@netops.int")
                .role(Role.TECHNICIAN)
                .enabled(true)
                .build();

        User tech2 = User.builder()
                .username("tech.sousse")
                .password(passwordEncoder.encode("tech1234"))
                .email("tech.sousse@netops.int")
                .role(Role.TECHNICIAN)
                .enabled(true)
                .build();

        User tech3 = User.builder()
                .username("tech.sfax")
                .password(passwordEncoder.encode("tech1234"))
                .email("tech.sfax@netops.int")
                .role(Role.TECHNICIAN)
                .enabled(true)
                .build();


        User techMonastir = User.builder()
                .username("tech.monastir")
                .password(passwordEncoder.encode("tech1234"))
                .email("tech.monastir@netops.int")
                .role(Role.TECHNICIAN)
                .enabled(true)
                .build();


        userRepository.saveAll(List.of(admin, consultant, tech1, tech2, tech3, techMonastir));
        System.out.println(">>> Users seeded");
    }


    private void seedLocations() {
        if (locationRepo.count() > 0) return;

        List<AppLocation> locations = List.of(
                // city centers with bounding boxes
                AppLocation.builder().name("Tunis Centre").city(City.TUNIS)
                        .type(LocationType.CITY_CENTER).latitude(36.8065).longitude(10.1815)
                        .minLat(36.70).maxLat(36.95).minLng(10.10).maxLng(10.35).active(true).build(),

                AppLocation.builder().name("Sousse Centre").city(City.SOUSSE)
                        .type(LocationType.CITY_CENTER).latitude(35.8256).longitude(10.6369)
                        .minLat(35.70).maxLat(35.95).minLng(10.55).maxLng(10.75).active(true).build(),

                AppLocation.builder().name("Kairouan Centre").city(City.KAIROUAN)
                        .type(LocationType.CITY_CENTER).latitude(35.6784).longitude(10.0963)
                        .minLat(35.60).maxLat(35.75).minLng(10.05).maxLng(10.20).active(true).build(),

                AppLocation.builder().name("Monastir Centre").city(City.MONASTIR)
                        .type(LocationType.CITY_CENTER).latitude(35.7643).longitude(10.8113)
                        .minLat(35.70).maxLat(35.82).minLng(10.75).maxLng(10.90).active(true).build(),

                AppLocation.builder().name("Sfax Centre").city(City.SFAX)
                        .type(LocationType.CITY_CENTER).latitude(34.7406).longitude(10.7603)
                        .minLat(34.65).maxLat(34.85).minLng(10.65).maxLng(10.85).active(true).build(),

                // stocks
                AppLocation.builder().name("Stock Tunis Nord").description("Dépôt matériel nord Tunis")
                        .city(City.TUNIS).type(LocationType.STOCK).latitude(36.8580).longitude(10.1950).active(true).build(),
                AppLocation.builder().name("Stock Tunis Sud").description("Dépôt matériel sud Tunis")
                        .city(City.TUNIS).type(LocationType.STOCK).latitude(36.7720).longitude(10.1730).active(true).build(),
                AppLocation.builder().name("Stock Sousse").description("Dépôt matériel Sousse")
                        .city(City.SOUSSE).type(LocationType.STOCK).latitude(35.8150).longitude(10.6250).active(true).build(),
                AppLocation.builder().name("Stock Sfax").description("Dépôt matériel Sfax")
                        .city(City.SFAX).type(LocationType.STOCK).latitude(34.7350).longitude(10.7500).active(true).build(),
                AppLocation.builder().name("Stock Monastir").description("Dépôt matériel Monastir")
                        .city(City.MONASTIR).type(LocationType.STOCK).latitude(35.7700).longitude(10.8200).active(true).build(),
                AppLocation.builder().name("Stock Kairouan").description("Dépôt matériel Kairouan")
                        .city(City.KAIROUAN).type(LocationType.STOCK).latitude(35.6750).longitude(10.0920).active(true).build()
        );

        locationRepo.saveAll(locations);
        System.out.println(">>> Locations seeded");
    }

    private void seedDemandes() {
        if (demandeRepo.count() > 0) return;

        User consultant = userRepository.findByUsername("cons").orElse(null);
        User techTunis = userRepository.findByUsername("tech.tunis").orElse(null);
        User techSousse = userRepository.findByUsername("tech.sousse").orElse(null);
        User techSfax = userRepository.findByUsername("tech.sfax").orElse(null);

        if (consultant == null) return;

        List<Demande> demandes = List.of(

                Demande.builder()
                        .title("Panne réseau totale - Siège Tunis")
                        .description("Interruption complète du réseau LAN. Tous les postes sont hors ligne. Impact critique sur la production.")
                        .priority(DemandePriority.CRITICAL)
                        .status(DemandeStatus.IN_PROGRESS)
                        .clientName("Société Tunisienne d'Informatique")
                        .clientContact("+216 71 123 456")
                        .clientLocation("Avenue Habib Bourguiba, Tunis")
                        .latitude(36.8180).longitude(10.1810)
                        .technician(techTunis)
                        .createdBy(consultant)
                        .build(),

                Demande.builder()
                        .title("Maintenance serveurs production - Sousse")
                        .description("Maintenance préventive planifiée des serveurs de production. Mise à jour firmware et vérification RAID.")
                        .priority(DemandePriority.MEDIUM)
                        .status(DemandeStatus.IN_PROGRESS)
                        .clientName("TechCorp Sousse")
                        .clientContact("+216 73 456 789")
                        .clientLocation("Rue de la République, Sousse")
                        .latitude(35.8290).longitude(10.6380)
                        .technician(techSousse)
                        .createdBy(consultant)
                        .build(),

                Demande.builder()
                        .title("Installation firewall nouvelle génération - Sfax")
                        .description("Installation et configuration d'un firewall FortiGate 600E. Remplacement de l'ancien équipement.")
                        .priority(DemandePriority.HIGH)
                        .status(DemandeStatus.NEW)
                        .clientName("Industries Sfaxiennes SA")
                        .clientContact("+216 74 789 012")
                        .clientLocation("Zone Industrielle Poudrière, Sfax")
                        .latitude(34.7480).longitude(10.7580)
                        .createdBy(consultant)
                        .build(),

                Demande.builder()
                        .title("Recâblage réseau complet - Monastir")
                        .description("Infrastructure réseau vieillissante à remplacer entièrement. Câblage Cat6A et patch panels.")
                        .priority(DemandePriority.LOW)
                        .status(DemandeStatus.NEW)
                        .clientName("Hôtel Marina Palace Monastir")
                        .clientContact("+216 73 111 222")
                        .clientLocation("Route de la Falaise, Monastir")
                        .latitude(35.7660).longitude(10.8140)
                        .createdBy(consultant)
                        .build(),

                Demande.builder()
                        .title("Dépannage VPN site-à-site - Tunis")
                        .description("Tunnel VPN IPSec entre le siège et les agences régionales non fonctionnel depuis 48h.")
                        .priority(DemandePriority.HIGH)
                        .status(DemandeStatus.RESOLVED)
                        .clientName("Cabinet Conseil & Audit Tunis")
                        .clientContact("+216 71 999 000")
                        .clientLocation("Les Berges du Lac 2, Tunis")
                        .latitude(36.8320).longitude(10.2280)
                        .technician(techTunis)
                        .createdBy(consultant)
                        .build(),

                Demande.builder()
                        .title("Déploiement switches HP Aruba - Kairouan")
                        .description("Remplacement de 12 switches end-of-life par des HP Aruba 2930F. Configuration VLAN à mettre en place.")
                        .priority(DemandePriority.MEDIUM)
                        .status(DemandeStatus.NEW)
                        .clientName("Banque Régionale Kairouan")
                        .clientContact("+216 77 334 455")
                        .clientLocation("Avenue de la République, Kairouan")
                        .latitude(35.6820).longitude(10.1010)
                        .createdBy(consultant)
                        .build(),

                Demande.builder()
                        .title("Panne onduleur salle serveurs - Sfax")
                        .description("Onduleur APC Smart-UPS en défaut. Alarme batterie active. Risque de coupure serveurs.")
                        .priority(DemandePriority.CRITICAL)
                        .status(DemandeStatus.IN_PROGRESS)
                        .clientName("Clinique El Amel Sfax")
                        .clientContact("+216 74 221 100")
                        .clientLocation("Boulevard des Martyrs, Sfax")
                        .latitude(34.7390).longitude(10.7650)
                        .technician(techSfax)
                        .createdBy(consultant)
                        .build(),

                Demande.builder()
                        .title("Configuration WiFi entreprise - Sousse")
                        .description("Déploiement solution WiFi Cisco Meraki MR sur 3 étages. SSIDs séparés pour employés et invités.")
                        .priority(DemandePriority.MEDIUM)
                        .status(DemandeStatus.CLOSED)
                        .clientName("Centre Commercial Sousse Mall")
                        .clientContact("+216 73 667 788")
                        .clientLocation("Route Nationale 1, Sousse")
                        .latitude(35.8100).longitude(10.6430)
                        .technician(techSousse)
                        .createdBy(consultant)
                        .build(),

                Demande.builder()
                        .title("Audit sécurité réseau - Tunis")
                        .description("Audit complet de l'infrastructure réseau. Test de pénétration, analyse des vulnérabilités, rapport de conformité.")
                        .priority(DemandePriority.LOW)
                        .status(DemandeStatus.NEW)
                        .clientName("Assurances Maghreb Tunis")
                        .clientContact("+216 71 445 566")
                        .clientLocation("Rue Alain Savary, Tunis")
                        .latitude(36.8050).longitude(10.1750)
                        .createdBy(consultant)
                        .build(),

                Demande.builder()
                        .title("Remplacement NAS défectueux - Monastir")
                        .description("NAS Synology RS3621xs+ en panne. Données inaccessibles. Récupération et migration vers nouveau NAS urgente.")
                        .priority(DemandePriority.HIGH)
                        .status(DemandeStatus.IN_PROGRESS)
                        .clientName("Studio Production Monastir")
                        .clientContact("+216 73 889 900")
                        .clientLocation("Zone Touristique, Monastir")
                        .latitude(35.7720).longitude(10.8060)
                        .technician(techSousse)
                        .createdBy(consultant)
                        .build()
        );

        List<Demande> saved = demandeRepo.saveAll(demandes);

        Demande d1 = saved.get(0);
        Demande d2 = saved.get(1);
        Demande d7 = saved.get(6);
        Demande d10 = saved.get(9);

        actionRepo.saveAll(List.of(
                DemandeAction.builder().demande(d1).status(DemandeActionStatus.TECHNICIAN_GOING_TO_SITE)
                        .note("Technicien en route depuis le dépôt nord").performedBy(techTunis).build(),
                DemandeAction.builder().demande(d1).status(DemandeActionStatus.TECHNICIAN_AT_SITE)
                        .note("Arrivée sur site, accueil par le responsable IT").performedBy(techTunis).build(),
                DemandeAction.builder().demande(d1).status(DemandeActionStatus.TECHNICIAN_GETTING_RESOURCES)
                        .note("Retour au stock pour récupérer switch de remplacement").performedBy(techTunis).build(),
                DemandeAction.builder().demande(d1).status(DemandeActionStatus.TECHNICIAN_FIXING_ISSUE)
                        .note("Remplacement du switch core défectueux en cours").performedBy(techTunis).build(),
                DemandeAction.builder().demande(d2).status(DemandeActionStatus.TECHNICIAN_GOING_TO_SITE)
                        .note("Départ de l'agence Sousse").performedBy(techSousse).build(),
                DemandeAction.builder().demande(d2).status(DemandeActionStatus.TECHNICIAN_AT_SITE)
                        .note("Présent en salle serveurs, début du diagnostic").performedBy(techSousse).build(),
                DemandeAction.builder().demande(d7).status(DemandeActionStatus.TECHNICIAN_GOING_TO_SITE)
                        .note("Intervention urgente, en route").performedBy(techSfax).build(),
                DemandeAction.builder().demande(d10).status(DemandeActionStatus.TECHNICIAN_GOING_TO_SITE)
                        .note("En route vers Monastir depuis Sousse").performedBy(techSousse).build(),
                DemandeAction.builder().demande(d10).status(DemandeActionStatus.TECHNICIAN_AT_SITE)
                        .note("Évaluation des dommages sur le NAS").performedBy(techSousse).build(),
                DemandeAction.builder().demande(d10).status(DemandeActionStatus.WAITING_FOR_PARTS)
                        .note("Commande du nouveau NAS en attente de livraison").performedBy(techSousse).build()
        ));

        Demande d5 = saved.get(4);
        actionRepo.saveAll(List.of(
                DemandeAction.builder().demande(d5).status(DemandeActionStatus.TECHNICIAN_GOING_TO_SITE)
                        .note("En route vers le client").performedBy(techTunis).build(),
                DemandeAction.builder().demande(d5).status(DemandeActionStatus.TECHNICIAN_AT_SITE)
                        .note("Diagnostic du tunnel VPN").performedBy(techTunis).build(),
                DemandeAction.builder().demande(d5).status(DemandeActionStatus.TECHNICIAN_FIXING_ISSUE)
                        .note("Reconfiguration des paramètres IKEv2").performedBy(techTunis).build(),
                DemandeAction.builder().demande(d5).status(DemandeActionStatus.ISSUE_RESOLVED)
                        .note("Tunnel VPN rétabli, tests de connectivité validés").performedBy(techTunis).build()
        ));

        techLocRepo.saveAll(List.of(
                TechnicianLocationHistory.builder()
                        .technician(techTunis)
                        .latitude(36.8350).longitude(10.2050)
                        .city(City.TUNIS).build(),

                TechnicianLocationHistory.builder()
                        .technician(techSousse)
                        .latitude(35.8180).longitude(10.6320)
                        .city(City.SOUSSE).build(),

                TechnicianLocationHistory.builder()
                        .technician(techSfax)
                        .latitude(34.7360).longitude(10.7610)
                        .city(City.SFAX).build()
        ));

        System.out.println(">>> Demandes, actions and technician locations seeded");
    }

}