package com.mejbri.pfe.netopssynchro.scheduler;

import com.mejbri.pfe.netopssynchro.service.LogAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LogAnalysisScheduler {

    private final LogAnalysisService logAnalysisService;

    /**
     * Runs every 30 minutes by default.
     * Override with: log.analysis.cron=0 *\/30 * * * *  in application.properties
     */
    @Scheduled(cron = "${log.analysis.cron:0 */30 * * * *}")
    public void scheduledLogAnalysis() {
        log.info("Scheduled log analysis started");
        try {
            logAnalysisService.analyseAllServers();
        } catch (Exception e) {
            log.error("Scheduled log analysis failed: {}", e.getMessage());
        }
        log.info("Scheduled log analysis completed");
    }
}
