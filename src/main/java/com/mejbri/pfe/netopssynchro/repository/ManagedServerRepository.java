package com.mejbri.pfe.netopssynchro.repository;

import com.mejbri.pfe.netopssynchro.entity.ManagedServer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ManagedServerRepository extends JpaRepository<ManagedServer, Long> {
    List<ManagedServer> findByActiveTrue();

}
