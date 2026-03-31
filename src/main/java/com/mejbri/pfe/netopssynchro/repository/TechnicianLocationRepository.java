package com.mejbri.pfe.netopssynchro.repository;


import com.mejbri.pfe.netopssynchro.entity.TechnicianLocationHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface TechnicianLocationRepository
        extends JpaRepository<TechnicianLocationHistory, Long> {


    @Query("""
        SELECT t FROM TechnicianLocationHistory t
        WHERE t.recordedAt = (
            SELECT MAX(t2.recordedAt)
            FROM TechnicianLocationHistory t2
            WHERE t2.technician.id = t.technician.id
        )
    """)
    List<TechnicianLocationHistory> findLatestPerTechnician();

    @Query("""
        SELECT t FROM TechnicianLocationHistory t
        WHERE t.technician.id = :technicianId
        ORDER BY t.recordedAt DESC
    """)
    List<TechnicianLocationHistory> findHistoryByTechnicianId(Long technicianId);

    Optional<TechnicianLocationHistory> findTopByTechnicianIdOrderByRecordedAtDesc(Long technicianId);
}