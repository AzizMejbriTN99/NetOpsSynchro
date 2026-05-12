package com.mejbri.pfe.netopssynchro.repository;

import com.mejbri.pfe.netopssynchro.entity.Demande;
import com.mejbri.pfe.netopssynchro.entity.DemandePriority;
import com.mejbri.pfe.netopssynchro.entity.DemandeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface DemandeRepository extends JpaRepository<Demande, Long> {
    List<Demande> findAllByOrderByCreatedAtDesc();

    List<Demande> findByStatus(DemandeStatus status);

    List<Demande> findByTechnicianId(Long technicianId);

    List<Demande> findByPriority(DemandePriority priority);

    long countByStatusAndTechnicianIsNull(DemandeStatus status);

    @Query("SELECT d FROM Demande d WHERE d.createdAt BETWEEN :from AND :to")
    List<Demande> findByCreatedAtBetween(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query("SELECT d FROM Demande d WHERE d.status = :status AND d.createdAt BETWEEN :from AND :to")
    List<Demande> findByStatusAndCreatedAtBetween(
            @Param("status") DemandeStatus status,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query("SELECT d FROM Demande d WHERE d.priority = :priority AND d.createdAt BETWEEN :from AND :to")
    List<Demande> findByPriorityAndCreatedAtBetween(
            @Param("priority") DemandePriority priority,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    Page<Demande> findByTitleContainingIgnoreCaseOrClientNameContainingIgnoreCase(
            String title,
            String clientName,
            Pageable pageable
    );

    Page<Demande> findByStatus(
            DemandeStatus status,
            Pageable pageable
    );

    long countByStatus(DemandeStatus status);

    @Query("""
            SELECT d FROM Demande d
            WHERE
            LOWER(d.title) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(d.clientName) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(d.clientLocation) LIKE LOWER(CONCAT('%', :search, '%'))
            """)
    Page<Demande> findBySearch(
            @Param("search") String search,
            Pageable pageable
    );

    @Query("""
            SELECT d FROM Demande d
            WHERE d.status = :status
            AND (
            LOWER(d.title) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(d.clientName) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(d.clientLocation) LIKE LOWER(CONCAT('%', :search, '%'))
            )
            """)
    Page<Demande> findByStatusAndSearch(
            @Param("status") DemandeStatus status,
            @Param("search") String search,
            Pageable pageable
    );

}
