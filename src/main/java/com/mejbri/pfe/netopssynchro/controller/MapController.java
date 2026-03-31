package com.mejbri.pfe.netopssynchro.controller;

import com.mejbri.pfe.netopssynchro.dto.*;
import com.mejbri.pfe.netopssynchro.entity.City;
import com.mejbri.pfe.netopssynchro.entity.LocationType;
import com.mejbri.pfe.netopssynchro.service.MapService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/map")
@RequiredArgsConstructor
public class MapController {

    private final MapService mapService;



    @GetMapping("/technicians")
    @PreAuthorize("hasAnyRole('CONSULTANT','ADMIN')")
    public ResponseEntity<List<TechnicianLocationDTO>> getTechnicianLocations(
            @RequestParam(required = false) City city) {
        return ResponseEntity.ok(mapService.getLatestTechnicianLocations(city));
    }

    @PostMapping("/technicians/location")
    @PreAuthorize("hasRole('TECHNICIAN')")
    public ResponseEntity<?> updateLocation(@RequestBody LocationUpdateRequest req,
                                            Authentication auth) {

        mapService.updateTechnicianLocationByUsername(auth.getName(),
                req.getLatitude(), req.getLongitude());
        return ResponseEntity.ok(Map.of("message", "Location updated"));
    }

    @GetMapping("/demandes")
    @PreAuthorize("hasAnyRole('CONSULTANT','ADMIN')")
    public ResponseEntity<List<DemandeMapDTO>> getDemandeLocations(
            @RequestParam(required = false) City city) {
        return ResponseEntity.ok(mapService.getDemandeLocations(city));
    }

    @GetMapping("/locations")
    @PreAuthorize("hasAnyRole('CONSULTANT','ADMIN')")
    public ResponseEntity<List<AppLocationDTO>> getLocations(
            @RequestParam(required = false) City city,
            @RequestParam(required = false) LocationType type) {
        return ResponseEntity.ok(mapService.getLocations(city, type));
    }

    @PostMapping("/locations")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AppLocationDTO> createLocation(@RequestBody AppLocationDTO req) {
        return ResponseEntity.ok(mapService.createLocation(req));
    }

    @DeleteMapping("/locations/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteLocation(@PathVariable Long id) {
        mapService.deleteLocation(id);
        return ResponseEntity.ok(Map.of("message", "Location deleted"));
    }

}
