package com.pitstop.saas.controller;

import com.pitstop.saas.dto.ComunicadoAlertDTO;
import com.pitstop.saas.dto.ComunicadoOficinaDTO;
import com.pitstop.saas.dto.ComunicadoOficinaDetailDTO;
import com.pitstop.saas.service.ComunicadoOficinaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller para oficinas visualizarem seus comunicados recebidos.
 * Diferente do ComunicadoController (SUPER_ADMIN), este é para usuários de oficinas.
 */
@RestController
@RequestMapping("/api/comunicados")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE', 'MECANICO', 'SUPER_ADMIN')")
public class ComunicadoOficinaController {

    private final ComunicadoOficinaService service;

    /**
     * Lista comunicados recebidos pela oficina
     */
    @GetMapping
    public ResponseEntity<Page<ComunicadoOficinaDTO>> listar(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(service.listarComunicados(page, size));
    }

    /**
     * Busca detalhes de um comunicado e marca como lido
     */
    @GetMapping("/{id}")
    public ResponseEntity<ComunicadoOficinaDetailDTO> buscar(@PathVariable UUID id) {
        return ResponseEntity.ok(service.buscarEMarcarComoLido(id));
    }

    /**
     * Confirma leitura de um comunicado
     */
    @PostMapping("/{id}/confirmar")
    public ResponseEntity<Void> confirmar(@PathVariable UUID id) {
        service.confirmarLeitura(id);
        return ResponseEntity.ok().build();
    }

    /**
     * Conta comunicados não lidos
     */
    @GetMapping("/nao-lidos/count")
    public ResponseEntity<Long> contarNaoLidos() {
        return ResponseEntity.ok(service.contarNaoLidos());
    }

    /**
     * Retorna dados para alerta no dashboard
     */
    @GetMapping("/alerta")
    public ResponseEntity<ComunicadoAlertDTO> getAlerta() {
        return ResponseEntity.ok(service.getAlertaDashboard());
    }

    /**
     * Retorna comunicados para exibir no login (modal obrigatório)
     */
    @GetMapping("/login")
    public ResponseEntity<List<ComunicadoOficinaDTO>> getComunicadosLogin() {
        return ResponseEntity.ok(service.getComunicadosParaLogin());
    }

    /**
     * Marca todos como lidos
     */
    @PostMapping("/marcar-todos-lidos")
    public ResponseEntity<Void> marcarTodosLidos() {
        service.marcarTodosComoLidos();
        return ResponseEntity.ok().build();
    }
}
