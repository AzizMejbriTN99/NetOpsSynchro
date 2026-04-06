package com.mejbri.pfe.netopssynchro.controller;

import com.mejbri.pfe.netopssynchro.dto.*;
import com.mejbri.pfe.netopssynchro.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/servers")
@PreAuthorize("hasRole('CONSULTANT')")
@RequiredArgsConstructor
public class ServerController {

    private final ServerManagementService managementService;
    private final ServerMonitorService    monitorService;

    // ── Server CRUD ──
    @GetMapping
    public ResponseEntity<List<ManagedServerDTO>> getAll() {
        return ResponseEntity.ok(managementService.getAllServers());
    }

    @PostMapping
    public ResponseEntity<ManagedServerDTO> create(@RequestBody ManagedServerDTO req) {
        return ResponseEntity.ok(managementService.createServer(req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        managementService.deleteServer(id);
        return ResponseEntity.ok(Map.of("message", "Server deleted"));
    }

    // ── Tomcat ──
    @PostMapping("/tomcat")
    public ResponseEntity<TomcatInstanceDTO> addTomcat(@RequestBody TomcatInstanceDTO req) {
        return ResponseEntity.ok(managementService.addTomcat(req));
    }

    @DeleteMapping("/tomcat/{id}")
    public ResponseEntity<?> deleteTomcat(@PathVariable Long id) {
        managementService.deleteTomcat(id);
        return ResponseEntity.ok(Map.of("message", "Tomcat instance deleted"));
    }

    @PostMapping("/tomcat/{id}/control")
    public ResponseEntity<?> controlTomcat(@PathVariable Long id,
                                           @RequestParam Long serverId,
                                           @RequestBody Map<String, String> body) {
        String result = monitorService.controlTomcat(id, serverId, body.get("action"));
        return ResponseEntity.ok(Map.of("output", result));
    }

    @GetMapping("/tomcat/{id}/logs/{filename}")
    public ResponseEntity<?> readLog(@PathVariable Long id,
                                     @PathVariable String filename,
                                     @RequestParam Long serverId) {
        return ResponseEntity.ok(Map.of("content",
                monitorService.readLog(id, filename, serverId)));
    }

    // ── Databases ──
    @PostMapping("/databases")
    public ResponseEntity<ManagedDatabaseDTO> addDatabase(@RequestBody ManagedDatabaseDTO req) {
        return ResponseEntity.ok(managementService.addDatabase(req));
    }

    @DeleteMapping("/databases/{id}")
    public ResponseEntity<?> deleteDatabase(@PathVariable Long id) {
        managementService.deleteDatabase(id);
        return ResponseEntity.ok(Map.of("message", "Database deleted"));
    }

    // ── Status ──
    @GetMapping("/status")
    public ResponseEntity<List<ServerStatusDTO>> getServerStatuses() {
        return ResponseEntity.ok(monitorService.getAllServerStatuses());
    }

    @GetMapping("/status/tomcat")
    public ResponseEntity<List<TomcatStatusDTO>> getTomcatStatuses() {
        return ResponseEntity.ok(monitorService.getAllTomcatStatuses());
    }

    @GetMapping("/status/databases")
    public ResponseEntity<List<DatabaseStatusDTO>> getDatabaseStatuses() {
        return ResponseEntity.ok(monitorService.getAllDatabaseStatuses());
    }

    @GetMapping("/databases")
    public ResponseEntity<List<ManagedDatabaseDTO>> getAllDatabases() {
        return ResponseEntity.ok(managementService.getAllDatabases());
    }

    @GetMapping("/databases/status")
    public ResponseEntity<List<DatabaseStatusDTO>> getAllDatabaseStatuses() {
        return ResponseEntity.ok(monitorService.getAllDatabaseStatuses());
    }
}
