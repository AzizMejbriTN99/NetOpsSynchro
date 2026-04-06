package com.mejbri.pfe.netopssynchro.service;

import com.mejbri.pfe.netopssynchro.entity.Demande;
import com.mejbri.pfe.netopssynchro.entity.DemandePriority;
import com.mejbri.pfe.netopssynchro.entity.DemandeStatus;
import com.mejbri.pfe.netopssynchro.entity.Role;
import com.mejbri.pfe.netopssynchro.repository.*;
import com.mejbri.pfe.netopssynchro.repository.UserRepository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardStatsService {

    private final DemandeRepository demandeRepo;
    private final LoginEventRepository loginEventRepo;
    private final UserRepository userRepo;
    private final TechnicianLocationRepository techLocRepo;

    public Map<String, Object> getConsultantStats() {
        long total         = demandeRepo.count();
        long newD          = demandeRepo.findByStatus(DemandeStatus.NEW).size();
        long inProgress    = demandeRepo.findByStatus(DemandeStatus.IN_PROGRESS).size();
        long resolved      = demandeRepo.findByStatus(DemandeStatus.RESOLVED).size();
        long closed        = demandeRepo.findByStatus(DemandeStatus.CLOSED).size();
        long low           = demandeRepo.findByPriority(DemandePriority.LOW).size();
        long medium        = demandeRepo.findByPriority(DemandePriority.MEDIUM).size();
        long high          = demandeRepo.findByPriority(DemandePriority.HIGH).size();
        long critical      = demandeRepo.findByPriority(DemandePriority.CRITICAL).size();

        Map<String, Object> result = new HashMap<>();
        result.put("total",           total);
        result.put("new",             newD);
        result.put("inProgress",      inProgress);
        result.put("resolved",        resolved);
        result.put("closed",          closed);
        result.put("lowPriority",     low);
        result.put("mediumPriority",  medium);
        result.put("highPriority",    high);
        result.put("criticalPriority",critical);
        return result;
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
        long closed         = demandeRepo.findByStatus(DemandeStatus.CLOSED).size();

        long adminCount      = userRepo.countByRole(Role.ADMIN);
        long consultantCount = userRepo.countByRole(Role.CONSULTANT);
        long technicianCount = userRepo.countByRole(Role.TECHNICIAN);

        Map<String, Object> result = new HashMap<>();
        result.put("dailyLogins",    dailyLogins);
        result.put("activeUsers",    activeUsers);
        result.put("totalUsers",     totalUsers);
        result.put("totalDemandes",  totalDemandes);
        result.put("newDemandes",    newDemandes);
        result.put("inProgress",     inProgress);
        result.put("resolved",       resolved);
        result.put("closed",         closed);
        result.put("adminCount",     adminCount);
        result.put("consultantCount",consultantCount);
        result.put("technicianCount",technicianCount);
        return result;
    }

    public Map<String, Object> getAdminMonthlyStats(LocalDate from, LocalDate to) {
        LocalDateTime f = from.atStartOfDay();
        LocalDateTime t = to.plusDays(1).atStartOfDay();

        List<Demande> all      = demandeRepo.findByCreatedAtBetween(f, t);
        long newD       = all.stream().filter(d -> d.getStatus() == DemandeStatus.NEW).count();
        long inProgress = all.stream().filter(d -> d.getStatus() == DemandeStatus.IN_PROGRESS).count();
        long resolved   = all.stream().filter(d -> d.getStatus() == DemandeStatus.RESOLVED).count();
        long closed     = all.stream().filter(d -> d.getStatus() == DemandeStatus.CLOSED).count();
        long critical   = all.stream().filter(d -> d.getPriority() == DemandePriority.CRITICAL).count();
        long high       = all.stream().filter(d -> d.getPriority() == DemandePriority.HIGH).count();
        long medium     = all.stream().filter(d -> d.getPriority() == DemandePriority.MEDIUM).count();
        long low        = all.stream().filter(d -> d.getPriority() == DemandePriority.LOW).count();

        long logins     = loginEventRepo.countByLoginAtAfter(f);

        // daily login breakdown
        List<Object[]> dailyLogins = loginEventRepo.countByDay(f, t);
        Map<Integer, Long> loginByDay = new LinkedHashMap<>();
        dailyLogins.forEach(row -> loginByDay.put(
                ((Number) row[0]).intValue(), ((Number) row[1]).longValue()));

        Map<String, Object> result = new HashMap<>();
        result.put("total",       all.size());
        result.put("new",         newD);
        result.put("inProgress",  inProgress);
        result.put("resolved",    resolved);
        result.put("closed",      closed);
        result.put("critical",    critical);
        result.put("high",        high);
        result.put("medium",      medium);
        result.put("low",         low);
        result.put("logins",      logins);
        result.put("loginByDay",  loginByDay);
        return result;
    }

    public Map<String, Object> getConsultantMonthlyStats(LocalDate from, LocalDate to) {
        LocalDateTime f = from.atStartOfDay();
        LocalDateTime t = to.plusDays(1).atStartOfDay();

        List<Demande> all = demandeRepo.findByCreatedAtBetween(f, t);
        long newD       = all.stream().filter(d -> d.getStatus() == DemandeStatus.NEW).count();
        long inProgress = all.stream().filter(d -> d.getStatus() == DemandeStatus.IN_PROGRESS).count();
        long resolved   = all.stream().filter(d -> d.getStatus() == DemandeStatus.RESOLVED).count();
        long closed     = all.stream().filter(d -> d.getStatus() == DemandeStatus.CLOSED).count();
        long critical   = all.stream().filter(d -> d.getPriority() == DemandePriority.CRITICAL).count();
        long high       = all.stream().filter(d -> d.getPriority() == DemandePriority.HIGH).count();
        long medium     = all.stream().filter(d -> d.getPriority() == DemandePriority.MEDIUM).count();
        long low        = all.stream().filter(d -> d.getPriority() == DemandePriority.LOW).count();

        Map<String, Object> result = new HashMap<>();
        result.put("total",      all.size());
        result.put("new",        newD);
        result.put("inProgress", inProgress);
        result.put("resolved",   resolved);
        result.put("closed",     closed);
        result.put("critical",   critical);
        result.put("high",       high);
        result.put("medium",     medium);
        result.put("low",        low);
        return result;
    }

}
