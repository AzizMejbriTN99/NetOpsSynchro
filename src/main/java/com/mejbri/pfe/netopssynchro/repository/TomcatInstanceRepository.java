package com.mejbri.pfe.netopssynchro.repository;

import com.mejbri.pfe.netopssynchro.entity.TomcatInstance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TomcatInstanceRepository extends JpaRepository<TomcatInstance, Long> {
    List<TomcatInstance> findByServerId(Long serverId);
}
