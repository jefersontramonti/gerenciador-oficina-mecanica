package com.pitstop.oficina.controller;

import com.pitstop.oficina.dto.CreateOficinaRequest;
import com.pitstop.oficina.dto.OficinaResumoResponse;
import com.pitstop.oficina.dto.OficinaResponse;
import com.pitstop.oficina.dto.UpdateOficinaRequest;
import com.pitstop.oficina.service.OficinaService;
import com.pitstop.oficina.domain.PlanoAssinatura;
import com.pitstop.oficina.domain.StatusOficina;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller REST para gerenciamento de Oficinas.
 * Endpoints protegidos por autenticação JWT e RBAC.
 *
 * @author PitStop Team
 */
@Slf4j
@RestController
@RequestMapping("/api/oficinas")
@RequiredArgsConstructor
@Tag(name = "Oficinas", description = "Gerenciamento de oficinas mecânicas (SaaS)")
@SecurityRequirement(name = "bearer-jwt")
public class OficinaController {

    private final OficinaService oficinaService;

    /**
     * Cria uma nova oficina (onboarding).
     * Acesso: Apenas SUPER_ADMIN (ou público em endpoint separado /api/public/oficinas/register).
     *
     * @param request Dados da oficina
     * @return Oficina criada (201 Created)
     */
    @Operation(summary = "Criar nova oficina (onboarding)",
        description = "Cria uma nova oficina com período trial de 7 dias. Endpoint para Super Admin ou registro público.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Oficina criada com sucesso"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos"),
        @ApiResponse(responseCode = "409", description = "CNPJ já existe")
    })
    @PostMapping
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public ResponseEntity<OficinaResponse> create(@Valid @RequestBody CreateOficinaRequest request) {
        log.info("POST /api/oficinas - Criando nova oficina: {}", request.nome());

        OficinaResponse response = oficinaService.create(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Busca oficina por ID.
     * Acesso: ADMIN e GERENTE (da própria oficina) ou SUPER_ADMIN (qualquer oficina).
     *
     * @param id ID da oficina
     * @return Oficina encontrada (200 OK)
     */
    @Operation(summary = "Buscar oficina por ID",
        description = "Retorna dados completos da oficina")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Oficina encontrada"),
        @ApiResponse(responseCode = "404", description = "Oficina não encontrada")
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'SUPER_ADMIN')")
    public ResponseEntity<OficinaResponse> findById(
        @Parameter(description = "ID da oficina") @PathVariable UUID id
    ) {

        OficinaResponse response = oficinaService.findById(id);

        return ResponseEntity.ok(response);
    }

    /**
     * Busca oficina por CNPJ.
     *
     * @param cnpj CNPJ (apenas números)
     * @return Oficina encontrada (200 OK)
     */
    @Operation(summary = "Buscar oficina por CNPJ")
    @GetMapping("/cnpj/{cnpj}")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public ResponseEntity<OficinaResponse> findByCnpj(
        @Parameter(description = "CNPJ (14 dígitos)") @PathVariable String cnpj
    ) {

        OficinaResponse response = oficinaService.findByCnpj(cnpj);

        return ResponseEntity.ok(response);
    }

    /**
     * Lista todas as oficinas com paginação (resumo).
     * Acesso: Apenas SUPER_ADMIN.
     *
     * @param pageable Configuração de paginação
     * @return Página de oficinas (200 OK)
     */
    @Operation(summary = "Listar todas as oficinas",
        description = "Retorna lista paginada de todas as oficinas (resumo). Acesso restrito ao Super Admin.")
    @GetMapping
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public ResponseEntity<Page<OficinaResumoResponse>> findAll(
        @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<OficinaResumoResponse> response = oficinaService.findAll(pageable);

        return ResponseEntity.ok(response);
    }

    /**
     * Lista oficinas com filtros avançados (Super Admin).
     *
     * @param status Status (opcional)
     * @param plano Plano (opcional)
     * @param nome Nome/nome fantasia (busca parcial, opcional)
     * @param cnpj CNPJ (busca parcial, opcional)
     * @param pageable Configuração de paginação
     * @return Página de oficinas filtradas (200 OK)
     */
    @Operation(summary = "Listar oficinas com filtros",
        description = "Busca oficinas com filtros avançados. Todos os filtros são opcionais.")
    @GetMapping("/search")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public ResponseEntity<Page<OficinaResumoResponse>> search(
        @Parameter(description = "Status da oficina") @RequestParam(required = false) StatusOficina status,
        @Parameter(description = "Plano de assinatura") @RequestParam(required = false) PlanoAssinatura plano,
        @Parameter(description = "Nome ou nome fantasia (busca parcial)") @RequestParam(required = false) String nome,
        @Parameter(description = "CNPJ (busca parcial)") @RequestParam(required = false) String cnpj,
        @PageableDefault(size = 20, sort = "nome") Pageable pageable
    ) {
        Page<OficinaResumoResponse> response = oficinaService.findWithFilters(status, plano, nome, cnpj, pageable);

        return ResponseEntity.ok(response);
    }

    /**
     * Lista oficinas por status.
     *
     * @param status Status desejado
     * @param pageable Configuração de paginação
     * @return Página de oficinas (200 OK)
     */
    @Operation(summary = "Listar oficinas por status")
    @GetMapping("/status/{status}")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public ResponseEntity<Page<OficinaResumoResponse>> findByStatus(
        @Parameter(description = "Status") @PathVariable StatusOficina status,
        @PageableDefault(size = 20) Pageable pageable
    ) {

        Page<OficinaResumoResponse> response = oficinaService.findByStatus(status, pageable);

        return ResponseEntity.ok(response);
    }

    /**
     * Lista oficinas com vencimento próximo (7 dias).
     * Útil para enviar lembretes.
     *
     * @return Lista de oficinas (200 OK)
     */
    @Operation(summary = "Listar oficinas com vencimento próximo",
        description = "Retorna oficinas cujo plano vence nos próximos 7 dias")
    @GetMapping("/vencimento-proximo")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public ResponseEntity<List<OficinaResumoResponse>> findVencimentoProximo() {

        List<OficinaResumoResponse> response = oficinaService.findVencimentoProximo();

        return ResponseEntity.ok(response);
    }

    /**
     * Atualiza dados de uma oficina.
     * Apenas campos não-nulos são atualizados.
     *
     * @param id ID da oficina
     * @param request Dados a atualizar
     * @return Oficina atualizada (200 OK)
     */
    @Operation(summary = "Atualizar dados da oficina",
        description = "Atualiza dados da oficina. Apenas campos não-nulos são modificados.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Oficina atualizada"),
        @ApiResponse(responseCode = "404", description = "Oficina não encontrada")
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'SUPER_ADMIN')")
    public ResponseEntity<OficinaResponse> update(
        @PathVariable UUID id,
        @Valid @RequestBody UpdateOficinaRequest request
    ) {
        log.info("PUT /api/oficinas/{} - Atualizando oficina", id);

        OficinaResponse response = oficinaService.update(id, request);

        return ResponseEntity.ok(response);
    }

    /**
     * Suspende uma oficina (não pagamento).
     * Bloqueia acesso dos usuários.
     *
     * @param id ID da oficina
     * @return 204 No Content
     */
    @Operation(summary = "Suspender oficina",
        description = "Suspende oficina por não pagamento ou violação de termos. Bloqueia acesso dos usuários.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Oficina suspensa"),
        @ApiResponse(responseCode = "404", description = "Oficina não encontrada"),
        @ApiResponse(responseCode = "400", description = "Oficina já está suspensa")
    })
    @PutMapping("/{id}/suspend")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public ResponseEntity<Void> suspend(@PathVariable UUID id) {
        log.warn("PUT /api/oficinas/{}/suspend - Suspendendo oficina", id);

        oficinaService.suspend(id);

        return ResponseEntity.noContent().build();
    }

    /**
     * Reativa uma oficina suspensa.
     *
     * @param id ID da oficina
     * @return 204 No Content
     */
    @Operation(summary = "Reativar oficina",
        description = "Reativa oficina suspensa após regularização do pagamento")
    @PutMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public ResponseEntity<Void> activate(@PathVariable UUID id) {
        log.info("PUT /api/oficinas/{}/activate - Reativando oficina", id);

        oficinaService.activate(id);

        return ResponseEntity.noContent().build();
    }

    /**
     * Cancela uma oficina (pedido do cliente).
     * Operação irreversível.
     *
     * @param id ID da oficina
     * @return 204 No Content
     */
    @Operation(summary = "Cancelar oficina",
        description = "Cancela a assinatura da oficina. Operação irreversível.")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Void> cancel(@PathVariable UUID id) {
        log.warn("DELETE /api/oficinas/{} - Cancelando oficina", id);

        oficinaService.cancel(id);

        return ResponseEntity.noContent().build();
    }

    /**
     * Faz upgrade do plano de assinatura.
     *
     * @param id ID da oficina
     * @param novoPlano Novo plano (deve ser superior)
     * @return Oficina atualizada (200 OK)
     */
    @Operation(summary = "Fazer upgrade de plano",
        description = "Atualiza para um plano superior. Efeito imediato.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Plano atualizado"),
        @ApiResponse(responseCode = "400", description = "Novo plano não é superior ao atual")
    })
    @PutMapping("/{id}/upgrade")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<OficinaResponse> upgradePlan(
        @PathVariable UUID id,
        @Parameter(description = "Novo plano (PROFISSIONAL ou TURBINADO)")
        @RequestParam PlanoAssinatura novoPlano
    ) {
        log.info("PUT /api/oficinas/{}/upgrade - Novo plano: {}", id, novoPlano);

        OficinaResponse response = oficinaService.upgradePlan(id, novoPlano);

        return ResponseEntity.ok(response);
    }

    /**
     * Faz downgrade do plano de assinatura.
     * Aplica apenas na próxima renovação.
     *
     * @param id ID da oficina
     * @param novoPlano Novo plano (deve ser inferior)
     * @return Oficina atualizada (200 OK)
     */
    @Operation(summary = "Fazer downgrade de plano",
        description = "Agenda downgrade para o próximo ciclo de renovação")
    @PutMapping("/{id}/downgrade")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<OficinaResponse> downgradePlan(
        @PathVariable UUID id,
        @Parameter(description = "Novo plano (ECONOMICO ou PROFISSIONAL)")
        @RequestParam PlanoAssinatura novoPlano
    ) {
        log.info("PUT /api/oficinas/{}/downgrade - Novo plano: {}", id, novoPlano);

        OficinaResponse response = oficinaService.downgradePlan(id, novoPlano);

        return ResponseEntity.ok(response);
    }

    /**
     * Retorna métricas gerais (Super Admin Dashboard).
     *
     * @return Métricas SaaS
     */
    @Operation(summary = "Obter métricas SaaS",
        description = "Retorna métricas para dashboard do Super Admin (MRR, total de oficinas ativas, etc)")
    @GetMapping("/metrics")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public ResponseEntity<MetricsResponse> getMetrics() {

        long totalAtivas = oficinaService.countAtivas();
        Double mrr = oficinaService.calculateMRR();

        MetricsResponse metrics = new MetricsResponse(totalAtivas, mrr);

        return ResponseEntity.ok(metrics);
    }

    /**
     * DTO de resposta para métricas.
     */
    public record MetricsResponse(
        long totalOficinasAtivas,
        Double mrr // Monthly Recurring Revenue
    ) {
    }
}
