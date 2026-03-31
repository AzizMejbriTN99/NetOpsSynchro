package com.mejbri.pfe.netopssynchro.repository;

import com.mejbri.pfe.netopssynchro.entity.DemandeAction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface DemandeActionRepository extends JpaRepository<DemandeAction, Long> {
    List<DemandeAction> findByDemandeIdOrderByPerformedAtAsc(Long demandeId);
    Optional<DemandeAction> findTopByDemandeIdOrderByPerformedAtDesc(Long demandeId);
}
