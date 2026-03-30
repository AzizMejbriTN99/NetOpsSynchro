package com.mejbri.pfe.netopssynchro.controller;

import com.mejbri.pfe.netopssynchro.dto.DemandeDTO;
import com.mejbri.pfe.netopssynchro.dto.DemandeRequest;
import com.mejbri.pfe.netopssynchro.dto.UserDTO;
import com.mejbri.pfe.netopssynchro.entity.DemandeStatus;
import com.mejbri.pfe.netopssynchro.service.DemandeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/consultant")
@PreAuthorize("hasRole('CONSULTANT')")
@RequiredArgsConstructor
public class DemandeController {

    private final DemandeService demandeService;

    @GetMapping("/demandes")
    public ResponseEntity<List<DemandeDTO>> getAll() {
        return ResponseEntity.ok(demandeService.getAll());
    }

    @GetMapping("/demandes/{id}")
    public ResponseEntity<DemandeDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(demandeService.getById(id));
    }

    @PostMapping("/demandes")
    public ResponseEntity<DemandeDTO> create(@RequestBody DemandeRequest req,
                                             Authentication auth) {
        return ResponseEntity.ok(demandeService.create(req, auth));
    }

    @PutMapping("/demandes/{id}")
    public ResponseEntity<DemandeDTO> update(@PathVariable Long id,
                                             @RequestBody DemandeRequest req) {
        return ResponseEntity.ok(demandeService.update(id, req));
    }

    @PatchMapping("/demandes/{id}/status")
    public ResponseEntity<DemandeDTO> updateStatus(@PathVariable Long id,
                                                   @RequestBody Map<String, String> body) {
        DemandeStatus status = DemandeStatus.valueOf(body.get("status"));
        return ResponseEntity.ok(demandeService.updateStatus(id, status));
    }

    @DeleteMapping("/demandes/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        demandeService.delete(id);
        return ResponseEntity.ok(Map.of("message", "Demande deleted"));
    }

    @GetMapping("/technicians")
    public ResponseEntity<List<UserDTO>> getTechnicians() {
        return ResponseEntity.ok(demandeService.getTechnicians());
    }
}
