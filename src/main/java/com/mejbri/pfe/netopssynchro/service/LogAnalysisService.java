package com.mejbri.pfe.netopssynchro.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mejbri.pfe.netopssynchro.dto.LogAnalysisResultDTO;
import com.mejbri.pfe.netopssynchro.entity.ManagedServer;
import com.mejbri.pfe.netopssynchro.entity.Role;
import com.mejbri.pfe.netopssynchro.entity.User;
import com.mejbri.pfe.netopssynchro.entity.NotificationType;
import com.mejbri.pfe.netopssynchro.repository.ManagedServerRepository;
import com.mejbri.pfe.netopssynchro.repository.UserRepository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class LogAnalysisService {

    private final ManagedServerRepository serverRepo;
    private final SshService sshService;
    private final NotificationService notificationService;
    private final PushNotificationService pushService;
    private final UserRepository userRepo;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${anthropic.api.key:}")
    private String anthropicApiKey;

    private static final String CLAUDE_URL   = "https://api.anthropic.com/v1/messages";
    private static final String CLAUDE_MODEL = "claude-sonnet-4-20250514";

    // SSH commands tried in order — works on most Linux distros
    private static final String LOG_CMD =
            "tail -n 400 /var/log/syslog 2>/dev/null || " +
                    "tail -n 400 /var/log/messages 2>/dev/null || " +
                    "journalctl -n 400 --no-pager 2>/dev/null || " +
                    "echo 'NO_LOG_FOUND'";

    private static final String TOMCAT_LOG_CMD =
            "find /opt /var /srv -name 'catalina.out' 2>/dev/null | " +
                    "head -1 | xargs -I{} tail -n 200 {} 2>/dev/null || echo 'NO_TOMCAT_LOG'";

    public List<LogAnalysisResultDTO> analyseAllServers() {
        List<ManagedServer> servers = serverRepo.findAll().stream()
                .filter(ManagedServer::isActive)
                .toList();

        List<LogAnalysisResultDTO> results = new ArrayList<>();
        for (ManagedServer server : servers) {
            try {
                LogAnalysisResultDTO result = analyseServer(server);
                results.add(result);
                if (result.isHasErrors()) {
                    notifyConsultants(server, result);
                }
            } catch (Exception e) {
                log.error("Log analysis failed for server {}: {}", server.getName(), e.getMessage());
                results.add(LogAnalysisResultDTO.builder()
                        .serverId(server.getId())
                        .serverName(server.getName())
                        .hasErrors(false)
                        .errorSummary("Analysis failed: " + e.getMessage())
                        .build());
            }
        }
        return results;
    }

    public LogAnalysisResultDTO analyseServer(Long serverId) {
        ManagedServer server = serverRepo.findById(serverId)
                .orElseThrow(() -> new RuntimeException("Server not found: " + serverId));
        LogAnalysisResultDTO result = analyseServer(server);
        if (result.isHasErrors()) notifyConsultants(server, result);
        return result;
    }

    private LogAnalysisResultDTO analyseServer(ManagedServer server) {
        // 1. Pull logs via SSH
        String sysLog    = sshService.execute(server.getHost(), server.getPort(),
                server.getUsername(), server.getEncryptedPassword(), LOG_CMD);
        String tomcatLog = sshService.execute(server.getHost(), server.getPort(),
                server.getUsername(), server.getEncryptedPassword(), TOMCAT_LOG_CMD);

        String combinedLog = buildLogSnippet(sysLog, tomcatLog, server.getName());

        if (anthropicApiKey == null || anthropicApiKey.isBlank()) {
            log.warn("Anthropic API key not configured — using rule-based fallback for {}", server.getName());
            return ruleBasedAnalysis(server, combinedLog);
        }
        return claudeAnalysis(server, combinedLog);
    }

    private LogAnalysisResultDTO claudeAnalysis(ManagedServer server, String logs) {
        String prompt = buildPrompt(logs, server.getName());

        Map<String, Object> body = Map.of(
                "model",      CLAUDE_MODEL,
                "max_tokens", 1024,
                "messages",   List.of(Map.of("role", "user", "content", prompt))
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", anthropicApiKey);
        headers.set("anthropic-version", "2023-06-01");

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    CLAUDE_URL, new HttpEntity<>(body, headers), String.class);

            JsonNode root    = objectMapper.readTree(response.getBody());
            String   content = root.path("content").get(0).path("text").asText();

            // Claude is instructed to reply in JSON
            content = content.strip();
            if (content.startsWith("```")) {
                content = content.replaceAll("```json\\n?", "").replaceAll("```\\n?", "").strip();
            }

            JsonNode parsed = objectMapper.readTree(content);
            return LogAnalysisResultDTO.builder()
                    .serverId(server.getId())
                    .serverName(server.getName())
                    .hasErrors(parsed.path("hasErrors").asBoolean(false))
                    .errorSummary(parsed.path("errorSummary").asText(""))
                    .affectedServices(toStringList(parsed.path("affectedServices")))
                    .rootCauseSuggestion(parsed.path("rootCauseSuggestion").asText(""))
                    .recommendedAction(parsed.path("recommendedAction").asText(""))
                    .rawLogSnippet(logs.length() > 800 ? logs.substring(0, 800) + "…" : logs)
                    .build();

        } catch (Exception e) {
            log.error("Claude API call failed: {}", e.getMessage());
            return ruleBasedAnalysis(server, logs);
        }
    }

    /**
     * Fallback when Claude API is unavailable.
     * Scans for well-known error keywords and builds a simple report.
     */
    private LogAnalysisResultDTO ruleBasedAnalysis(ManagedServer server, String logs) {
        String lower = logs.toLowerCase();
        boolean hasError = lower.contains("error")   || lower.contains("exception")
                || lower.contains("critical") || lower.contains("fatal")
                || lower.contains("oom")      || lower.contains("out of memory")
                || lower.contains("failed")   || lower.contains("refused");

        List<String> affected = new ArrayList<>();
        if (lower.contains("tomcat") || lower.contains("catalina")) affected.add("Tomcat");
        if (lower.contains("mysql")  || lower.contains("mariadb"))  affected.add("MySQL/MariaDB");
        if (lower.contains("nginx")  || lower.contains("apache"))   affected.add("Web Server");
        if (lower.contains("ssh"))                                   affected.add("SSH Daemon");
        if (lower.contains("disk")   || lower.contains("no space"))  affected.add("Disk");
        if (lower.contains("oom")    || lower.contains("out of memory")) affected.add("JVM/Memory");

        return LogAnalysisResultDTO.builder()
                .serverId(server.getId())
                .serverName(server.getName())
                .hasErrors(hasError)
                .errorSummary(hasError
                        ? "Error keywords detected in system logs. AI analysis unavailable — manual review recommended."
                        : "No obvious error keywords found in recent logs.")
                .affectedServices(affected)
                .rootCauseSuggestion(hasError ? "Review full logs manually." : "")
                .recommendedAction(hasError ? "SSH into the server and inspect the log files directly." : "")
                .rawLogSnippet(logs.length() > 800 ? logs.substring(0, 800) + "…" : logs)
                .build();
    }

    // ── Notification ──────────────────────────────────────────────────────────

    private void notifyConsultants(ManagedServer server, LogAnalysisResultDTO result) {
        String msg = String.format("[%s] Error detected: %s", server.getName(), result.getErrorSummary());

        // In-app notification for all consultants
        List<User> consultants = userRepo.findAll().stream()
                .filter(u -> u.getRole() == Role.CONSULTANT && u.isEnabled())
                .toList();

        for (User consultant : consultants) {
            notificationService.pushCustom(
                    NotificationType.SERVER_ERROR_DETECTED,
                    consultant.getUsername(),
                    msg
            );
            pushService.sendToUser(consultant, "⚠️ Server Alert — " + server.getName(),
                    result.getErrorSummary());
        }
        log.warn("Server error detected on [{}]: {}", server.getName(), result.getErrorSummary());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String buildLogSnippet(String sysLog, String tomcatLog, String serverName) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== System Log (").append(serverName).append(") ===\n");
        sb.append(sysLog, 0, Math.min(sysLog.length(), 1500)).append("\n\n");
        if (!tomcatLog.contains("NO_TOMCAT_LOG")) {
            sb.append("=== Tomcat Log ===\n");
            sb.append(tomcatLog, 0, Math.min(tomcatLog.length(), 1000)).append("\n");
        }
        return sb.toString();
    }

    private String buildPrompt(String logs, String serverName) {
        return """
                You are a senior Linux/Java system administrator analysing server logs.
                Analyse the following log output from server "%s" and respond ONLY with
                a valid JSON object — no markdown, no explanation, just raw JSON.
                
                JSON schema:
                {
                  "hasErrors": boolean,
                  "errorSummary": "one-sentence plain-English summary of what went wrong, or 'No errors detected'",
                  "affectedServices": ["list", "of", "service", "names"],
                  "rootCauseSuggestion": "concise root cause in plain English",
                  "recommendedAction": "specific actionable step to fix the issue"
                }
                
                Logs:
                %s
                """.formatted(serverName, logs);
    }

    private List<String> toStringList(JsonNode node) {
        List<String> list = new ArrayList<>();
        if (node.isArray()) node.forEach(n -> list.add(n.asText()));
        return list;
    }
}
