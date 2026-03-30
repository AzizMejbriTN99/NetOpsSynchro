package com.mejbri.pfe.netopssynchro.service;

import com.mejbri.pfe.netopssynchro.dto.DemandeDTO;
import com.mejbri.pfe.netopssynchro.dto.DemandeRequest;
import com.mejbri.pfe.netopssynchro.dto.UserDTO;
import com.mejbri.pfe.netopssynchro.entity.*;
import com.mejbri.pfe.netopssynchro.repository.DemandeRepository;
import com.mejbri.pfe.netopssynchro.repository.UserRepository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DemandeService {

    private final DemandeRepository demandeRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public List<DemandeDTO> getAll() {
        return demandeRepository.findAllByOrderByCreatedAtDesc()
                .stream().map(this::toDTO).toList();
    }

    public DemandeDTO getById(Long id) {
        return toDTO(findOrThrow(id));
    }

    public DemandeDTO create(DemandeRequest req, Authentication auth) {
        User creator = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Demande demande = Demande.builder()
                .title(req.getTitle())
                .description(req.getDescription())
                .priority(req.getPriority() != null ? req.getPriority() : DemandePriority.MEDIUM)
                .status(DemandeStatus.NEW)
                .clientName(req.getClientName())
                .clientContact(req.getClientContact())
                .clientLocation(req.getClientLocation())
                .createdBy(creator)
                .build();

        if (req.getTechnicianId() != null) {
            User tech = userRepository.findById(req.getTechnicianId())
                    .orElseThrow(() -> new RuntimeException("Technician not found"));
            demande.setTechnician(tech);
            notificationService.push(NotificationType.TASK_ASSIGNED, tech.getUsername());
        }

        return toDTO(demandeRepository.save(demande));
    }

    public DemandeDTO update(Long id, DemandeRequest req) {
        Demande demande = findOrThrow(id);

        demande.setTitle(req.getTitle());
        demande.setDescription(req.getDescription());
        demande.setPriority(req.getPriority());
        demande.setClientName(req.getClientName());
        demande.setClientContact(req.getClientContact());
        demande.setClientLocation(req.getClientLocation());

        if (req.getStatus() != null) demande.setStatus(req.getStatus());

        if (req.getTechnicianId() != null) {
            User tech = userRepository.findById(req.getTechnicianId())
                    .orElseThrow(() -> new RuntimeException("Technician not found"));
            boolean isNewAssignment = demande.getTechnician() == null
                    || !demande.getTechnician().getId().equals(req.getTechnicianId());
            demande.setTechnician(tech);
            if (isNewAssignment)
                notificationService.push(NotificationType.TASK_ASSIGNED, tech.getUsername());
        } else {
            demande.setTechnician(null);
        }

        return toDTO(demandeRepository.save(demande));
    }

    public DemandeDTO updateStatus(Long id, DemandeStatus status) {
        Demande demande = findOrThrow(id);
        demande.setStatus(status);
        return toDTO(demandeRepository.save(demande));
    }

    public void delete(Long id) {
        demandeRepository.deleteById(id);
    }

    public List<UserDTO> getTechnicians() {
        return userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.TECHNICIAN && u.isEnabled())
                .map(u -> {
                    UserDTO dto = new UserDTO();
                    dto.setId(u.getId());
                    dto.setUsername(u.getUsername());
                    dto.setEmail(u.getEmail());
                    dto.setRole(u.getRole());
                    dto.setEnabled(u.isEnabled());
                    return dto;
                }).toList();
    }

    private Demande findOrThrow(Long id) {
        return demandeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Demande not found"));
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
        dto.setCreatedAt(d.getCreatedAt());
        dto.setUpdatedAt(d.getUpdatedAt());
        if (d.getTechnician() != null) {
            dto.setTechnicianId(d.getTechnician().getId());
            dto.setTechnicianUsername(d.getTechnician().getUsername());
        }
        if (d.getCreatedBy() != null)
            dto.setCreatedByUsername(d.getCreatedBy().getUsername());
        return dto;
    }
}
