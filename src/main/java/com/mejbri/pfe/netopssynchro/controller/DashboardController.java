package com.mejbri.pfe.netopssynchro.controller;

import com.mejbri.pfe.netopssynchro.service.DashboardStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
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

    @GetMapping("/admin/monthly")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> adminMonthly(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(statsService.getAdminMonthlyStats(from, to));
    }

    @GetMapping("/consultant/monthly")
    @PreAuthorize("hasRole('CONSULTANT')")
    public ResponseEntity<Map<String, Object>> consultantMonthly(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(statsService.getConsultantMonthlyStats(from, to));
    }
}
