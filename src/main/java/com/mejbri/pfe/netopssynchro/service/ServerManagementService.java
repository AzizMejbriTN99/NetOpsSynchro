package com.mejbri.pfe.netopssynchro.service;

import com.mejbri.pfe.netopssynchro.dto.*;
import com.mejbri.pfe.netopssynchro.entity.*;
import com.mejbri.pfe.netopssynchro.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ServerManagementService {

    private final ManagedServerRepository   serverRepo;
    private final TomcatInstanceRepository  tomcatRepo;
    private final ManagedDatabaseRepository dbRepo;
    private final EncryptionService         encryptionService;

    // ── Servers ──
    public List<ManagedServerDTO> getAllServers() {
        return serverRepo.findAll().stream().map(this::toServerDTO).toList();
    }

    public ManagedServerDTO createServer(ManagedServerDTO req) {
        ManagedServer s = ManagedServer.builder()
                .name(req.getName())
                .host(req.getHost())
                .port(req.getPort() == 0 ? 22 : req.getPort())
                .username(req.getUsername())
                .encryptedPassword(encryptionService.encrypt(req.getPassword()))
                .city(req.getCity())
                .active(true)
                .build();
        return toServerDTO(serverRepo.save(s));
    }

    public void deleteServer(Long id) { serverRepo.deleteById(id); }

    // ── Tomcat ──
    public TomcatInstanceDTO addTomcat(TomcatInstanceDTO req) {
        ManagedServer server = serverRepo.findById(req.getServerId())
                .orElseThrow(() -> new RuntimeException("Server not found"));
        TomcatInstance tc = TomcatInstance.builder()
                .name(req.getName())
                .catalinaHome(req.getCatalinaHome())
                .webappsPath(req.getWebappsPath())
                .logsPath(req.getLogsPath())
                .startScript(req.getStartScript())
                .stopScript(req.getStopScript())
                .server(server)
                .build();
        return toTomcatDTO(tomcatRepo.save(tc));
    }

    public void deleteTomcat(Long id) { tomcatRepo.deleteById(id); }

    // ── Databases ──
    public ManagedDatabaseDTO addDatabase(ManagedDatabaseDTO req) {
        ManagedServer server = serverRepo.findById(req.getServerId())
                .orElseThrow(() -> new RuntimeException("Server not found"));
        ManagedDatabase db = ManagedDatabase.builder()
                .name(req.getName())
                .type(req.getType())
                .encryptedConnectionString(encryptionService.encrypt(req.getConnectionString()))
                .dbUsername(req.getDbUsername())
                .encryptedDbPassword(encryptionService.encrypt(req.getDbPassword()))
                .server(server)
                .build();
        return toDatabaseDTO(dbRepo.save(db));
    }

    public void deleteDatabase(Long id) { dbRepo.deleteById(id); }

    private ManagedServerDTO toServerDTO(ManagedServer s) {
        ManagedServerDTO dto = new ManagedServerDTO();
        dto.setId(s.getId());
        dto.setName(s.getName());
        dto.setHost(s.getHost());
        dto.setPort(s.getPort());
        dto.setUsername(s.getUsername());
        dto.setCity(s.getCity());
        dto.setActive(s.isActive());
        return dto;
    }

    public List<ManagedDatabaseDTO> getAllDatabases() {
        return dbRepo.findAll().stream().map(this::toDatabaseDTO).toList();
    }

    private TomcatInstanceDTO toTomcatDTO(TomcatInstance t) {
        TomcatInstanceDTO dto = new TomcatInstanceDTO();
        dto.setId(t.getId());
        dto.setName(t.getName());
        dto.setCatalinaHome(t.getCatalinaHome());
        dto.setWebappsPath(t.getWebappsPath());
        dto.setLogsPath(t.getLogsPath());
        dto.setStartScript(t.getStartScript());
        dto.setStopScript(t.getStopScript());
        dto.setServerId(t.getServer().getId());
        return dto;
    }

    private ManagedDatabaseDTO toDatabaseDTO(ManagedDatabase db) {
        ManagedDatabaseDTO dto = new ManagedDatabaseDTO();
        dto.setId(db.getId());
        dto.setName(db.getName());
        dto.setType(db.getType());
        dto.setDbUsername(db.getDbUsername());
        dto.setServerId(db.getServer() != null ? db.getServer().getId() : null);
        return dto;
    }
}
