package com.pitstop.ia.controller;

import com.pitstop.ia.dto.DiagnosticoIARequest;
import com.pitstop.ia.dto.DiagnosticoIAResponse;
import com.pitstop.ia.service.DiagnosticoIAService;
import com.pitstop.ia.service.DiagnosticoIAService.EstatisticasUsoIA;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Controller para diagnóstico assistido por IA.
 * Permite gerar diagnósticos automotivos usando Claude.
 */
@RestController
@RequestMapping("/api/diagnostico-ia")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Diagnóstico IA", description = "Diagnóstico assistido por IA")
public class DiagnosticoIAController {

    private final DiagnosticoIAService diagnosticoService;

    /**
     * Gera diagnóstico para o problema relatado.
     */
    @PostMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE', 'MECANICO')")
    @Operation(summary = "Gerar diagnóstico", description = "Gera diagnóstico assistido por IA para o problema relatado")
    public ResponseEntity<DiagnosticoIAResponse> gerarDiagnostico(
            @Valid @RequestBody DiagnosticoIARequest request) {
        log.info("Requisição de diagnóstico IA para veículo: {}", request.veiculoId());
        DiagnosticoIAResponse diagnostico = diagnosticoService.gerarDiagnostico(request);
        return ResponseEntity.ok(diagnostico);
    }

    /**
     * Verifica se a IA está disponível para a oficina atual.
     */
    @GetMapping("/disponivel")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE', 'MECANICO')")
    @Operation(summary = "Verificar disponibilidade", description = "Verifica se a IA está configurada e disponível")
    public ResponseEntity<DisponibilidadeResponse> verificarDisponibilidade() {
        boolean disponivel = diagnosticoService.isIADisponivel();
        return ResponseEntity.ok(new DisponibilidadeResponse(disponivel));
    }

    /**
     * Retorna estatísticas de uso da IA.
     */
    @GetMapping("/estatisticas")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE')")
    @Operation(summary = "Estatísticas de uso", description = "Retorna estatísticas de uso da IA")
    public ResponseEntity<EstatisticasUsoIA> getEstatisticas() {
        EstatisticasUsoIA estatisticas = diagnosticoService.getEstatisticas();
        return ResponseEntity.ok(estatisticas);
    }

    /**
     * DTO para resposta de disponibilidade.
     */
    public record DisponibilidadeResponse(boolean disponivel) {}
}
