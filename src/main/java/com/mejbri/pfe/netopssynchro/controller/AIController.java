package com.mejbri.pfe.netopssynchro.controller;

import com.mejbri.pfe.netopssynchro.dto.AssignmentResultDTO;
import com.mejbri.pfe.netopssynchro.dto.LogAnalysisResultDTO;
import com.mejbri.pfe.netopssynchro.service.AIAssignmentService;
import com.mejbri.pfe.netopssynchro.service.LogAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AIController {

    private final AIAssignmentService assignmentService;
    private final LogAnalysisService  logAnalysisService;

    @GetMapping("/assignment/suggest")
    @PreAuthorize("hasAnyRole('ADMIN','CONSULTANT')")
    public ResponseEntity<AssignmentResultDTO> suggestAssignments() {
        return ResponseEntity.ok(assignmentService.computeAndOptionallyApply(false));
    }

    @PostMapping("/assignment/apply")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AssignmentResultDTO> applyAssignments() {
        return ResponseEntity.ok(assignmentService.computeAndOptionallyApply(true));
    }

    @GetMapping("/logs")
    @PreAuthorize("hasAnyRole('ADMIN','CONSULTANT')")
    public ResponseEntity<List<LogAnalysisResultDTO>> analyseLogs() {
        return ResponseEntity.ok(logAnalysisService.analyseAllServers());
    }

    @GetMapping("/logs/{serverId}")
    @PreAuthorize("hasAnyRole('ADMIN','CONSULTANT')")
    public ResponseEntity<LogAnalysisResultDTO> analyseServer(@PathVariable Long serverId) {
        return ResponseEntity.ok(logAnalysisService.analyseServer(serverId));
    }
}
