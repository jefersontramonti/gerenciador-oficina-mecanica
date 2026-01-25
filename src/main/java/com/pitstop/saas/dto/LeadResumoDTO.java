package com.pitstop.saas.dto;

import com.pitstop.saas.domain.StatusLead;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Summarized Lead DTO for list views.
 *
 * Excludes observacoes for performance.
 *
 * @author PitStop Team
 */
public record LeadResumoDTO(
    UUID id,
    String nome,
    String email,
    String whatsapp,
    String origem,
    StatusLead status,
    LocalDateTime createdAt
) {
}
