package com.pitstop.manutencaopreventiva.controller;

import com.pitstop.manutencaopreventiva.dto.AgendamentoPublicoDTO;
import com.pitstop.manutencaopreventiva.service.AgendamentoPublicService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller público para confirmação de agendamentos pelo cliente.
 * Não requer autenticação.
 */
@RestController
@RequestMapping("/api/public/agendamento")
@RequiredArgsConstructor
@Tag(name = "Agendamento Público", description = "Endpoints públicos para confirmação de agendamentos")
public class AgendamentoPublicController {

    private final AgendamentoPublicService agendamentoPublicService;

    @GetMapping("/{token}")
    @Operation(summary = "Buscar agendamento por token")
    public ResponseEntity<AgendamentoPublicoDTO> buscarPorToken(@PathVariable String token) {
        return ResponseEntity.ok(agendamentoPublicService.buscarPorToken(token));
    }

    @PostMapping("/{token}/confirmar")
    @Operation(summary = "Confirmar agendamento via token")
    public ResponseEntity<Map<String, Object>> confirmar(@PathVariable String token) {
        return ResponseEntity.ok(agendamentoPublicService.confirmar(token));
    }

    @PostMapping("/{token}/rejeitar")
    @Operation(summary = "Rejeitar agendamento via token")
    public ResponseEntity<Map<String, Object>> rejeitar(
            @PathVariable String token,
            @RequestBody(required = false) Map<String, String> body) {
        String motivo = body != null ? body.get("motivo") : null;
        return ResponseEntity.ok(agendamentoPublicService.rejeitar(token, motivo));
    }
}
