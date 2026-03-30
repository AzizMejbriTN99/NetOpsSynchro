package com.mejbri.pfe.netopssynchro.repository;

import com.mejbri.pfe.netopssynchro.entity.Demande;
import com.mejbri.pfe.netopssynchro.entity.DemandeStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DemandeRepository extends JpaRepository<Demande, Long> {
    List<Demande> findAllByOrderByCreatedAtDesc();
    List<Demande> findByStatus(DemandeStatus status);
    List<Demande> findByTechnicianId(Long technicianId);

}
