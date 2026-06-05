package com.mejbri.pfe.netopssynchro.service;

import com.mejbri.pfe.netopssynchro.dto.*;
import com.mejbri.pfe.netopssynchro.entity.*;
import com.mejbri.pfe.netopssynchro.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.*;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServerMonitorService {

    private final ManagedServerRepository serverRepo;
    private final TomcatInstanceRepository tomcatRepo;
    private final ManagedDatabaseRepository dbRepo;
    private final SshService sshService;
    private final EncryptionService encryptionService;

    // in-memory status cache — updated every 30s
    private final Map<Long, ServerStatusDTO>   serverStatus   = new LinkedHashMap<>();
    private final Map<Long, TomcatStatusDTO>   tomcatStatus   = new LinkedHashMap<>();
    private final Map<Long, DatabaseStatusDTO> databaseStatus = new LinkedHashMap<>();

    @Scheduled(fixedDelay = 30000)
    public void refreshAll() {
        serverRepo.findByActiveTrue().forEach(this::refreshServer);

        // Also check databases that have no server attached
        dbRepo.findAll().stream()
                .filter(db -> db.getServer() == null)
                .forEach(db -> {
                    boolean connected = testDbConnection(db);
                    databaseStatus.put(db.getId(), DatabaseStatusDTO.builder()
                            .databaseId(db.getId())
                            .name(db.getName())
                            .type(db.getType())
                            .serverId(null)
                            .connected(connected)
                            .build());
                });
    }

    private void refreshServer(ManagedServer server) {
        boolean sshOk = sshService.testConnection(
                server.getHost(), server.getPort(),
                server.getUsername(), server.getEncryptedPassword());

        serverStatus.put(server.getId(), ServerStatusDTO.builder()
                .serverId(server.getId())
                .name(server.getName())
                .host(server.getHost())
                .sshReachable(sshOk)
                .build());

        if (!sshOk) return;

        // refresh tomcat instances
        tomcatRepo.findByServerId(server.getId()).forEach(tc -> {
            String result = sshService.execute(
                    server.getHost(), server.getPort(),
                    server.getUsername(), server.getEncryptedPassword(),
                    "ps aux | grep " + tc.getCatalinaHome() + " | grep -v grep | wc -l");
            boolean running = result.trim().equals("1");

            // list webapps
            String webapps = sshService.execute(
                    server.getHost(), server.getPort(),
                    server.getUsername(), server.getEncryptedPassword(),
                    "ls " + tc.getWebappsPath());

            // list log files
            String logs = sshService.execute(
                    server.getHost(), server.getPort(),
                    server.getUsername(), server.getEncryptedPassword(),
                    "ls -t " + tc.getLogsPath() + " | head -20");

            tomcatStatus.put(tc.getId(), TomcatStatusDTO.builder()
                    .instanceId(tc.getId())
                    .name(tc.getName())
                    .serverId(server.getId())
                    .running(running)
                    .webapps(Arrays.stream(webapps.split("\n"))
                            .filter(s -> !s.isBlank()).toList())
                    .logFiles(Arrays.stream(logs.split("\n"))
                            .filter(s -> !s.isBlank()).toList())
                    .build());
        });

        // refresh databases
        dbRepo.findByServerId(server.getId()).forEach(db -> {
            boolean connected = testDbConnection(db);
            databaseStatus.put(db.getId(), DatabaseStatusDTO.builder()
                    .databaseId(db.getId())
                    .name(db.getName())
                    .type(db.getType())
                    .serverId(server.getId())
                    .connected(connected)
                    .build());
        });
    }

    private boolean testDbConnection(ManagedDatabase db) {
        try {
            String rawUrl = encryptionService.decrypt(db.getEncryptedConnectionString());
            String user   = db.getDbUsername();
            String pass   = encryptionService.decrypt(db.getEncryptedDbPassword());

            // Auto-construct proper JDBC URL if user entered a bare host:port/db string
            String url = buildJdbcUrl(db.getType(), rawUrl);

            String driver = switch (db.getType()) {
                case MYSQL      -> "com.mysql.cj.jdbc.Driver";
                case POSTGRESQL -> "org.postgresql.Driver";
                case ORACLE     -> "oracle.jdbc.OracleDriver";
                case MSSQL      -> "com.microsoft.sqlserver.jdbc.SQLServerDriver";
                case MONGODB    -> null;
            };
            if (driver == null) {
                log.debug("No JDBC driver for type {} ({})", db.getType(), db.getName());
                return false;
            }

            Class.forName(driver);
            try (Connection c = DriverManager.getConnection(url, user, pass)) {
                boolean ok = c.isValid(3);
                log.debug("DB [{}] connection test: {}", db.getName(), ok ? "OK" : "INVALID");
                return ok;
            }
        } catch (Exception e) {
            log.warn("DB [{}] connection test failed: {}", db.getName(), e.getMessage());
            return false;
        }
    }

    /**
     * Accepts bare host:port/db strings and turns them into proper JDBC URLs.
     * If the string already starts with "jdbc:" it is returned as-is.
     */
    private String buildJdbcUrl(DatabaseType type, String raw) {
        if (raw == null) return "";
        if (raw.startsWith("jdbc:")) return raw;

        return switch (type) {
            case MYSQL      -> "jdbc:mysql://"      + raw;
            case POSTGRESQL -> "jdbc:postgresql://" + raw;
            case ORACLE     -> "jdbc:oracle:thin:@" + raw;
            case MSSQL      -> "jdbc:sqlserver://"  + raw;
            default         -> raw;
        };
    }

    // ── Public accessors ──
    public List<ServerStatusDTO>   getAllServerStatuses()   { return new ArrayList<>(serverStatus.values()); }
    public List<TomcatStatusDTO>   getAllTomcatStatuses()   { return new ArrayList<>(tomcatStatus.values()); }
    public List<DatabaseStatusDTO> getAllDatabaseStatuses() { return new ArrayList<>(databaseStatus.values()); }

    public String readLog(Long tomcatId, String filename, Long serverId) {
        ManagedServer server = serverRepo.findById(serverId).orElseThrow();
        TomcatInstance tc    = tomcatRepo.findById(tomcatId).orElseThrow();
        return sshService.execute(server.getHost(), server.getPort(),
                server.getUsername(), server.getEncryptedPassword(),
                "tail -200 " + tc.getLogsPath() + "/" + filename);
    }

    public String controlTomcat(Long tomcatId, Long serverId, String action) {
        ManagedServer server = serverRepo.findById(serverId).orElseThrow();
        TomcatInstance tc    = tomcatRepo.findById(tomcatId).orElseThrow();
        String script = action.equals("start") ? tc.getStartScript() : tc.getStopScript();
        return sshService.execute(server.getHost(), server.getPort(),
                server.getUsername(), server.getEncryptedPassword(),
                "bash " + script);
    }
}