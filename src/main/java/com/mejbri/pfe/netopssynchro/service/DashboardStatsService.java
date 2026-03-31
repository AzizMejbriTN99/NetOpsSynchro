package com.mejbri.pfe.netopssynchro.service;

import com.mejbri.pfe.netopssynchro.entity.DemandeStatus;
import com.mejbri.pfe.netopssynchro.repository.*;
import com.mejbri.pfe.netopssynchro.repository.UserRepository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardStatsService {

    private final DemandeRepository demandeRepo;
    private final LoginEventRepository loginEventRepo;
    private final UserRepository userRepo;
    private final TechnicianLocationRepository techLocRepo;

    public Map<String, Object> getConsultantStats() {
        long total      = demandeRepo.count();
        long newD       = demandeRepo.findByStatus(DemandeStatus.NEW).size();
        long inProgress = demandeRepo.findByStatus(DemandeStatus.IN_PROGRESS).size();
        long resolved   = demandeRepo.findByStatus(DemandeStatus.RESOLVED).size();
        long closed     = demandeRepo.findByStatus(DemandeStatus.CLOSED).size();

        return Map.of(
                "total",      total,
                "new",        newD,
                "inProgress", inProgress,
                "resolved",   resolved,
                "closed",     closed
        );
    }

    public Map<String, Object> getAdminStats() {
        LocalDateTime todayStart = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime last30min  = LocalDateTime.now().minusMinutes(30);

        long dailyLogins    = loginEventRepo.countByLoginAtAfter(todayStart);
        long activeUsers    = loginEventRepo.countDistinctUsersAfter(last30min);
        long totalUsers     = userRepo.count();
        long totalDemandes  = demandeRepo.count();
        long newDemandes    = demandeRepo.findByStatus(DemandeStatus.NEW).size();
        long inProgress     = demandeRepo.findByStatus(DemandeStatus.IN_PROGRESS).size();
        long resolved       = demandeRepo.findByStatus(DemandeStatus.RESOLVED).size();

        return Map.of(
                "dailyLogins",   dailyLogins,
                "activeUsers",   activeUsers,
                "totalUsers",    totalUsers,
                "totalDemandes", totalDemandes,
                "newDemandes",   newDemandes,
                "inProgress",    inProgress,
                "resolved",      resolved
        );
    }
}
