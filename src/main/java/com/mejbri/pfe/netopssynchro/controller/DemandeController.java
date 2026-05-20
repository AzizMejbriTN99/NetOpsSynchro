package com.mejbri.pfe.netopssynchro.controller;

import com.mejbri.pfe.netopssynchro.dto.DemandeDTO;
import com.mejbri.pfe.netopssynchro.dto.DemandeRequest;
import com.mejbri.pfe.netopssynchro.dto.UserDTO;
import com.mejbri.pfe.netopssynchro.entity.DemandePhoto;
import com.mejbri.pfe.netopssynchro.entity.DemandeStatus;
import com.mejbri.pfe.netopssynchro.repository.DemandePhotoRepository;
import com.mejbri.pfe.netopssynchro.service.DemandeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/consultant")
@PreAuthorize("hasRole('CONSULTANT')")
@RequiredArgsConstructor
public class DemandeController {

    private final DemandeService         demandeService;
    private final DemandePhotoRepository photoRepository;

    // ── Demande CRUD ──────────────────────────────────────────────────────────

    @GetMapping("/demandes")
    public ResponseEntity<Map<String, Object>> getDemandes(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(required = false) DemandeStatus status,
            Pageable pageable) {
        return ResponseEntity.ok(demandeService.getDemandes(search, status, pageable));
    }

    @GetMapping("/demandes/{id}")
    public ResponseEntity<DemandeDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(demandeService.getById(id));
    }

    @PostMapping("/demandes")
    public ResponseEntity<DemandeDTO> create(@RequestBody DemandeRequest req, Authentication auth) {
        return ResponseEntity.ok(demandeService.create(req, auth));
    }

    @PutMapping("/demandes/{id}")
    public ResponseEntity<DemandeDTO> update(@PathVariable Long id, @RequestBody DemandeRequest req) {
        return ResponseEntity.ok(demandeService.update(id, req));
    }

    @PatchMapping("/demandes/{id}/status")
    public ResponseEntity<DemandeDTO> updateStatus(@PathVariable Long id,
                                                   @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(
                demandeService.updateStatus(id, DemandeStatus.valueOf(body.get("status"))));
    }

    @DeleteMapping("/demandes/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        demandeService.delete(id);
        return ResponseEntity.ok(Map.of("message", "Demande deleted"));
    }

    @GetMapping("/technicians")
    public ResponseEntity<List<UserDTO>> getTechnicians(@RequestParam(required = false) String city) {
        return ResponseEntity.ok(demandeService.getTechniciansByCity(city));
    }

    @PostMapping("/demandes/generate")
    public ResponseEntity<DemandeDTO> generateRandom(@RequestParam String city, Authentication auth) {
        return ResponseEntity.ok(demandeService.generateRandom(city, auth));
    }

    // ── Photo endpoints ───────────────────────────────────────────────────────

    /** List metadata for all attachments on a demande (no binary data). */
    @GetMapping("/demandes/{id}/photos")
    public ResponseEntity<List<Map<String, Object>>> getPhotos(@PathVariable Long id) {
        List<Map<String, Object>> photos = photoRepository.findByDemandeId(id)
                .stream()
                .map(p -> Map.<String, Object>of(
                        "id",          p.getId(),
                        "filename",    p.getFilename(),
                        "contentType", p.getContentType() != null ? p.getContentType() : "application/octet-stream",
                        "uploadedBy",  p.getUploadedBy()  != null ? p.getUploadedBy()  : "",
                        "uploadedAt",  p.getUploadedAt()  != null ? p.getUploadedAt().toString() : ""
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(photos);
    }

    /** Upload a file — stored as a LONGBLOB in the database. */
    @PostMapping(value = "/demandes/{id}/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> uploadPhoto(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            Authentication auth) throws IOException {

        String originalFilename = StringUtils.cleanPath(
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "file");
        String ct = file.getContentType() != null ? file.getContentType() : "application/octet-stream";

        DemandePhoto photo = DemandePhoto.builder()
                .demande(demandeService.getRawDemande(id))
                .filename(originalFilename)
                .contentType(ct)
                .data(file.getBytes())
                .uploadedBy(auth.getName())
                .build();
        photoRepository.save(photo);

        return ResponseEntity.ok(Map.of(
                "id",       photo.getId(),
                "filename", originalFilename
        ));
    }

    /** Stream a photo's binary data directly from the database. */
    @GetMapping("/demandes/{demandeId}/photos/{photoId}/file")
    public ResponseEntity<byte[]> servePhoto(@PathVariable Long demandeId,
                                             @PathVariable Long photoId) {
        DemandePhoto photo = photoRepository.findById(photoId)
                .orElseThrow(() -> new RuntimeException("Photo not found"));
        String ct = photo.getContentType() != null ? photo.getContentType() : "application/octet-stream";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(ct))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + photo.getFilename() + "\"")
                .body(photo.getData());
    }

    /** Delete a photo record (and its blob) from the database. */
    @DeleteMapping("/demandes/{demandeId}/photos/{photoId}")
    public ResponseEntity<?> deletePhoto(@PathVariable Long demandeId,
                                         @PathVariable Long photoId) {
        if (!photoRepository.existsById(photoId))
            throw new RuntimeException("Photo not found");
        photoRepository.deleteById(photoId);
        return ResponseEntity.ok(Map.of("message", "Photo deleted"));
    }
}