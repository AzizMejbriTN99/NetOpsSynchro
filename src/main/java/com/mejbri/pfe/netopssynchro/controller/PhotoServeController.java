package com.mejbri.pfe.netopssynchro.controller;

import com.mejbri.pfe.netopssynchro.entity.DemandePhoto;
import com.mejbri.pfe.netopssynchro.repository.DemandePhotoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Serves photo blobs to ANY authenticated user regardless of role.
 * The consultant uploads photos; the technician (mobile) needs to view them.
 * Kept separate from DemandeController so the CONSULTANT role guard there
 * doesn't block technician access.
 */
@RestController
@RequestMapping("/api/photos")
@RequiredArgsConstructor
public class PhotoServeController {

    private final DemandePhotoRepository photoRepository;

    /**
     * GET /api/photos/{photoId}/file
     * Streams the blob for any authenticated role.
     * The caller only needs a valid JWT — no role restriction.
     */
    @GetMapping("/{photoId}/file")
    public ResponseEntity<byte[]> serve(@PathVariable Long photoId,
                                        Authentication authentication) {
        DemandePhoto photo = photoRepository.findById(photoId)
                .orElseThrow(() -> new RuntimeException("Photo not found"));

        byte[] data = photo.getData();
        if (data == null || data.length == 0)
            return ResponseEntity.noContent().build();

        String ct = photo.getContentType() != null
                ? photo.getContentType() : "application/octet-stream";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(ct))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + photo.getFilename() + "\"")
                .body(data);
    }
}