package com.mejbri.pfe.netopssynchro.controller;

import com.mejbri.pfe.netopssynchro.service.DashboardStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardStatsService statsService;

    @GetMapping("/consultant")
    @PreAuthorize("hasRole('CONSULTANT')")
    public ResponseEntity<Map<String, Object>> consultantStats() {
        return ResponseEntity.ok(statsService.getConsultantStats());
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> adminStats() {
        return ResponseEntity.ok(statsService.getAdminStats());
    }
}
