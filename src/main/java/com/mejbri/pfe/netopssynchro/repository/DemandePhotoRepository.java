package com.mejbri.pfe.netopssynchro.repository;

import com.mejbri.pfe.netopssynchro.entity.DemandePhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DemandePhotoRepository extends JpaRepository<DemandePhoto, Long> {
    List<DemandePhoto> findByDemandeId(Long demandeId);

}

