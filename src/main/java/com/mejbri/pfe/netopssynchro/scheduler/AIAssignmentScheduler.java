package com.mejbri.pfe.netopssynchro.scheduler;

import com.mejbri.pfe.netopssynchro.dto.AssignmentResultDTO;
import com.mejbri.pfe.netopssynchro.service.AIAssignmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AIAssignmentScheduler {

    private final AIAssignmentService aiAssignmentService;

    @Scheduled(cron = "${ai.assignment.cron:0 */1 * * * *}")
    public void runAutoAssignment() {
        try {
            AssignmentResultDTO result = aiAssignmentService.computeAndOptionallyApply(true);

            if (result.getAssignments().isEmpty()) {
                log.debug("[AI Scheduler] No new assignments made — {}",
                        result.getSummary());
            } else {
                log.info("[AI Scheduler] Auto-assigned {} technician(s) → {} | {}",
                        result.getAssignments().size(),
                        result.getAssignments().stream()
                                .map(a -> a.getTechnicianUsername() + "→#" + a.getDemandeId())
                                .toList(),
                        result.getSummary());
            }
        } catch (Exception e) {
            log.error("[AI Scheduler] Assignment run failed: {}", e.getMessage(), e);
        }
    }
}