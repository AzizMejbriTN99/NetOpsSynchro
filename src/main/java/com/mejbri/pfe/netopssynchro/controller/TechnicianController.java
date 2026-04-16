package com.mejbri.pfe.netopssynchro.controller;

import com.mejbri.pfe.netopssynchro.dto.*;
import com.mejbri.pfe.netopssynchro.entity.*;
import com.mejbri.pfe.netopssynchro.repository.*;
import com.mejbri.pfe.netopssynchro.repository.UserRepository.UserRepository;
import com.mejbri.pfe.netopssynchro.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

@RestController
@RequestMapping("/api/technician")
@PreAuthorize("hasRole('TECHNICIAN')")
@RequiredArgsConstructor
public class TechnicianController {

    private final DemandeRepository        demandeRepo;
    private final DemandeActionRepository  actionRepo;
    private final DemandePhotoRepository   photoRepo;
    private final TechnicianResourceRepository resourceRepo;
    private final UserRepository userRepo;
    private final MapService               mapService;
    private final DemandeActionService     actionService;
    private final NotificationService      notificationService;

    @Value("${file.upload.path}")
    private String uploadPath;

    // ── My assigned demandes ──────────────────────────────
    @GetMapping("/demandes")
    public ResponseEntity<List<DemandeDTO>> getMyDemandes(Authentication auth) {
        User tech = getUser(auth);
        List<Demande> demandes = demandeRepo.findByTechnicianId(tech.getId());
        return ResponseEntity.ok(demandes.stream().map(this::toDTO).toList());
    }

    @GetMapping("/demandes/{id}")
    public ResponseEntity<DemandeDTO> getDemande(@PathVariable Long id, Authentication auth) {
        Demande d = findAndVerify(id, auth);
        return ResponseEntity.ok(toDTO(d));
    }

    // ── Update demande status ─────────────────────────────
    @PatchMapping("/demandes/{id}/status")
    public ResponseEntity<DemandeDTO> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            Authentication auth) {
        Demande d = findAndVerify(id, auth);
        String statusStr = body.get("status");
        d.setStatus(DemandeStatus.valueOf(statusStr));
        demandeRepo.save(d);

        // push action to timeline
        DemandeActionStatus actionStatus = switch (statusStr) {
            case "IN_PROGRESS" -> DemandeActionStatus.TECHNICIAN_AT_SITE;
            case "RESOLVED"    -> DemandeActionStatus.ISSUE_RESOLVED;
            default            -> DemandeActionStatus.TECHNICIAN_AT_SITE;
        };
        actionService.addAction(id, actionStatus, body.getOrDefault("note", ""), auth);

        // notify consultant
        notificationService.push(NotificationType.TICKET_UPDATED,
                "Demande #" + id + " → " + statusStr);

        return ResponseEntity.ok(toDTO(d));
    }

    // ── Add action to timeline ────────────────────────────
    @PostMapping("/demandes/{id}/actions")
    public ResponseEntity<DemandeActionDTO> addAction(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            Authentication auth) {
        findAndVerify(id, auth);
        DemandeActionStatus status = DemandeActionStatus.valueOf(body.get("status"));
        String note = body.getOrDefault("note", "");
        return ResponseEntity.ok(actionService.addAction(id, status, note, auth));
    }

    @GetMapping("/demandes/{id}/timeline")
    public ResponseEntity<List<DemandeActionDTO>> getTimeline(@PathVariable Long id, Authentication auth) {
        findAndVerify(id, auth);
        return ResponseEntity.ok(actionService.getTimeline(id));
    }

    // ── Photos ────────────────────────────────────────────
    @PostMapping("/demandes/{id}/photos")
    public ResponseEntity<?> uploadPhoto(
            @PathVariable Long id,
            @RequestParam("photo") MultipartFile file,
            Authentication auth) throws IOException {
        Demande d = findAndVerify(id, auth);

        Path dir = Paths.get(uploadPath, String.valueOf(id));
        Files.createDirectories(dir);

        String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path dest = dir.resolve(filename);
        Files.copy(file.getInputStream(), dest, StandardCopyOption.REPLACE_EXISTING);

        DemandePhoto photo = DemandePhoto.builder()
                .demande(d)
                .filename(filename)
                .storagePath(dest.toString())
                .uploadedBy(auth.getName())
                .build();
        photoRepo.save(photo);

        return ResponseEntity.ok(Map.of("filename", filename, "photoId", photo.getId()));
    }

    @GetMapping("/demandes/{id}/photos")
    public ResponseEntity<List<Map<String, Object>>> getPhotos(@PathVariable Long id, Authentication auth) {
        findAndVerify(id, auth);
        return ResponseEntity.ok(photoRepo.findByDemandeId(id).stream()
                .map(p -> Map.<String, Object>of(
                        "id",       p.getId(),
                        "filename", p.getFilename(),
                        "uploadedBy", p.getUploadedBy(),
                        "uploadedAt", p.getUploadedAt().toString()
                )).toList());
    }

    @GetMapping("/photos/{photoId}/download")
    public ResponseEntity<Resource> downloadPhoto(@PathVariable Long photoId, Authentication auth) throws IOException {
        DemandePhoto photo = photoRepo.findById(photoId)
                .orElseThrow(() -> new RuntimeException("Photo not found"));
        Path path = Paths.get(photo.getStoragePath());
        Resource resource = new UrlResource(path.toUri());
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + photo.getFilename() + "\"")
                .body(resource);
    }

    // ── Location ──────────────────────────────────────────
    @PostMapping("/location")
    public ResponseEntity<?> updateLocation(
            @RequestBody LocationUpdateRequest req,
            Authentication auth) {
        mapService.updateTechnicianLocationByUsername(
                auth.getName(), req.getLatitude(), req.getLongitude());
        return ResponseEntity.ok(Map.of("message", "Location updated"));
    }

    // ── Resources ─────────────────────────────────────────
    @GetMapping("/resources")
    public ResponseEntity<List<Map<String, Object>>> getResources(Authentication auth) {
        User tech = getUser(auth);
        return ResponseEntity.ok(resourceRepo.findByTechnicianId(tech.getId()).stream()
                .map(r -> Map.<String, Object>of(
                        "id",           r.getId(),
                        "resourceName", r.getResourceName(),
                        "quantity",     r.getQuantity(),
                        "unit",         r.getUnit(),
                        "notes",        r.getNotes() != null ? r.getNotes() : ""
                )).toList());
    }

    @PostMapping("/resources")
    public ResponseEntity<?> addResource(
            @RequestBody Map<String, Object> body,
            Authentication auth) {
        User tech = getUser(auth);
        TechnicianResource res = TechnicianResource.builder()
                .technician(tech)
                .resourceName((String) body.get("resourceName"))
                .quantity((Integer) body.get("quantity"))
                .unit((String) body.get("unit"))
                .notes((String) body.getOrDefault("notes", ""))
                .build();
        resourceRepo.save(res);
        return ResponseEntity.ok(Map.of("message", "Resource added"));
    }

    @DeleteMapping("/resources/{id}")
    public ResponseEntity<?> deleteResource(@PathVariable Long id, Authentication auth) {
        User tech = getUser(auth);
        TechnicianResource res = resourceRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Resource not found"));
        if (!res.getTechnician().getId().equals(tech.getId()))
            return ResponseEntity.status(403).build();
        resourceRepo.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Resource deleted"));
    }

    // ── Helpers ───────────────────────────────────────────
    private User getUser(Authentication auth) {
        return userRepo.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private Demande findAndVerify(Long id, Authentication auth) {
        Demande d = demandeRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Demande not found"));
        if (!d.getTechnician().getUsername().equals(auth.getName()))
            throw new RuntimeException("Not your demande");
        return d;
    }

    private DemandeDTO toDTO(Demande d) {
        DemandeDTO dto = new DemandeDTO();
        dto.setId(d.getId());
        dto.setTitle(d.getTitle());
        dto.setDescription(d.getDescription());
        dto.setPriority(d.getPriority());
        dto.setStatus(d.getStatus());
        dto.setClientName(d.getClientName());
        dto.setClientContact(d.getClientContact());
        dto.setClientLocation(d.getClientLocation());
        dto.setLatitude(d.getLatitude());
        dto.setLongitude(d.getLongitude());
        dto.setCreatedAt(d.getCreatedAt());
        dto.setUpdatedAt(d.getUpdatedAt());
        if (d.getTechnician() != null)
            dto.setTechnicianUsername(d.getTechnician().getUsername());
        return dto;
    }
}
