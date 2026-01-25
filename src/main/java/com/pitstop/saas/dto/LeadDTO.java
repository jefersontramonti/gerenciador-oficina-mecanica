package com.pitstop.saas.dto;

import com.pitstop.saas.domain.StatusLead;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Complete Lead DTO with all details.
 *
 * @author PitStop Team
 */
public record LeadDTO(
    UUID id,
    String nome,
    String email,
    String whatsapp,
    String origem,
    StatusLead status,
    String observacoes,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
