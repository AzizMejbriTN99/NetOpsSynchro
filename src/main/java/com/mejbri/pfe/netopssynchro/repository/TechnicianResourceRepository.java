package com.mejbri.pfe.netopssynchro.repository;

import com.mejbri.pfe.netopssynchro.entity.TechnicianResource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TechnicianResourceRepository extends JpaRepository<TechnicianResource, Long> {
    List<TechnicianResource> findByTechnicianId(Long technicianId);
}
