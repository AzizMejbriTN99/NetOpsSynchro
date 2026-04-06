package com.mejbri.pfe.netopssynchro.repository;

import com.mejbri.pfe.netopssynchro.entity.ManagedDatabase;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ManagedDatabaseRepository extends JpaRepository<ManagedDatabase, Long> {
    List<ManagedDatabase> findByServerId(Long serverId);
}
