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
            String url  = encryptionService.decrypt(db.getEncryptedConnectionString());
            String user = db.getDbUsername();
            String pass = encryptionService.decrypt(db.getEncryptedDbPassword());
            String driver = switch (db.getType()) {
                case MYSQL      -> "com.mysql.cj.jdbc.Driver";
                case POSTGRESQL -> "org.postgresql.Driver";
                case ORACLE     -> "oracle.jdbc.OracleDriver";
                case MSSQL      -> "com.microsoft.sqlserver.jdbc.SQLServerDriver";
                case MONGODB    -> null; // handled differently
            };
            if (driver == null) return false;
            Class.forName(driver);
            try (Connection c = DriverManager.getConnection(url, user, pass)) {
                return c.isValid(3);
            }
        } catch (Exception e) {
            return false;
        }
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
