package com.mejbri.pfe.netopssynchro.repository;

import com.mejbri.pfe.netopssynchro.entity.LoginEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface LoginEventRepository extends JpaRepository<LoginEvent, Long> {
    long countByLoginAtAfter(LocalDateTime since);

    @Query("SELECT COUNT(DISTINCT e.username) FROM LoginEvent e WHERE e.loginAt > :since")
    long countDistinctUsersAfter(LocalDateTime since);

    @Query("SELECT FUNCTION('DAY', e.loginAt) as day, COUNT(e) as count " +
            "FROM LoginEvent e WHERE e.loginAt BETWEEN :from AND :to " +
            "GROUP BY FUNCTION('DAY', e.loginAt) ORDER BY FUNCTION('DAY', e.loginAt)")
    List<Object[]> countByDay(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

}