package com.mejbri.pfe.netopssynchro.service;

import com.mejbri.pfe.netopssynchro.dto.DemandeActionDTO;
import com.mejbri.pfe.netopssynchro.entity.*;
import com.mejbri.pfe.netopssynchro.repository.*;
import com.mejbri.pfe.netopssynchro.repository.UserRepository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DemandeActionService {

    private final DemandeActionRepository actionRepo;
    private final DemandeRepository demandeRepo;
    private final UserRepository userRepo;

    public List<DemandeActionDTO> getTimeline(Long demandeId) {
        return actionRepo.findByDemandeIdOrderByPerformedAtAsc(demandeId)
                .stream().map(this::toDTO).toList();
    }

    public DemandeActionDTO addAction(Long demandeId, DemandeActionStatus status,
                                      String note, Authentication auth) {
        Demande demande = demandeRepo.findById(demandeId)
                .orElseThrow(() -> new RuntimeException("Demande not found"));
        User performer = userRepo.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        DemandeAction action = DemandeAction.builder()
                .demande(demande)
                .status(status)
                .note(note)
                .performedBy(performer)
                .build();

        return toDTO(actionRepo.save(action));
    }

    public Optional<DemandeActionDTO> getLatestAction(Long demandeId) {
        return actionRepo.findTopByDemandeIdOrderByPerformedAtDesc(demandeId)
                .map(this::toDTO);
    }

    private DemandeActionDTO toDTO(DemandeAction a) {
        DemandeActionDTO dto = new DemandeActionDTO();
        dto.setId(a.getId());
        dto.setStatus(a.getStatus());
        dto.setNote(a.getNote());
        dto.setPerformedAt(a.getPerformedAt());
        if (a.getPerformedBy() != null)
            dto.setPerformedBy(a.getPerformedBy().getUsername());
        return dto;
    }
}