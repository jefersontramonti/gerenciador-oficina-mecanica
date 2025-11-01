package com.pitstop.cliente.controller;

import com.pitstop.cliente.domain.TipoCliente;
import com.pitstop.cliente.dto.ClienteResponse;
import com.pitstop.cliente.dto.CreateClienteRequest;
import com.pitstop.cliente.dto.UpdateClienteRequest;
import com.pitstop.cliente.service.ClienteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Controller REST para gerenciamento de clientes.
 *
 * <p>Endpoints disponíveis:</p>
 * <ul>
 *   <li>POST /api/clientes - Criar cliente</li>
 *   <li>GET /api/clientes - Listar clientes (com filtros e paginação)</li>
 *   <li>GET /api/clientes/{id} - Buscar por ID</li>
 *   <li>GET /api/clientes/cpf-cnpj/{cpfCnpj} - Buscar por CPF/CNPJ</li>
 *   <li>PUT /api/clientes/{id} - Atualizar cliente</li>
 *   <li>DELETE /api/clientes/{id} - Desativar cliente (soft delete)</li>
 *   <li>PATCH /api/clientes/{id}/reativar - Reativar cliente</li>
 *   <li>GET /api/clientes/filtros/estados - Listar estados</li>
 *   <li>GET /api/clientes/filtros/cidades - Listar cidades</li>
 *   <li>GET /api/clientes/estatisticas - Estatísticas de clientes</li>
 * </ul>
 *
 * <p><strong>Permissões:</strong></p>
 * <ul>
 *   <li>ADMIN, GERENTE, ATENDENTE: todos os endpoints</li>
 *   <li>MECANICO: apenas leitura (GET)</li>
 * </ul>
 *
 * @author PitStop Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
@Tag(name = "Clientes", description = "Gerenciamento de clientes (pessoas físicas e jurídicas)")
public class ClienteController {

    private final ClienteService clienteService;

    /**
     * Cria um novo cliente.
     *
     * @param request dados do cliente
     * @return cliente criado (HTTP 201)
     */
    @PostMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE')")
    @Operation(summary = "Criar novo cliente", description = "Cadastra um novo cliente (pessoa física ou jurídica) no sistema")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Cliente criado com sucesso"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos"),
        @ApiResponse(responseCode = "409", description = "CPF/CNPJ já cadastrado"),
        @ApiResponse(responseCode = "403", description = "Acesso negado")
    })
    public ResponseEntity<ClienteResponse> create(@Valid @RequestBody CreateClienteRequest request) {
        ClienteResponse response = clienteService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Lista clientes com paginação e filtros opcionais.
     *
     * @param nome filtro de nome (opcional)
     * @param tipo filtro de tipo (opcional)
     * @param cidade filtro de cidade (opcional)
     * @param estado filtro de UF (opcional)
     * @param pageable configuração de paginação
     * @return página de clientes
     */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE', 'MECANICO')")
    @Operation(summary = "Listar clientes", description = "Lista clientes ativos com suporte a filtros e paginação")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso"),
        @ApiResponse(responseCode = "403", description = "Acesso negado")
    })
    public ResponseEntity<Page<ClienteResponse>> findAll(
        @Parameter(description = "Filtro por nome (busca parcial)") @RequestParam(required = false) String nome,
        @Parameter(description = "Filtro por tipo de cliente") @RequestParam(required = false) TipoCliente tipo,
        @Parameter(description = "Filtro por cidade") @RequestParam(required = false) String cidade,
        @Parameter(description = "Filtro por estado (UF)") @RequestParam(required = false) String estado,
        @PageableDefault(size = 20, sort = "nome") Pageable pageable
    ) {
        Page<ClienteResponse> clientes;

        // Se há filtros, usa busca com filtros; senão, lista todos
        if (nome != null || tipo != null || cidade != null || estado != null) {
            clientes = clienteService.findByFiltros(nome, tipo, cidade, estado, pageable);
        } else {
            clientes = clienteService.findAll(pageable);
        }

        return ResponseEntity.ok(clientes);
    }

    /**
     * Busca cliente por ID.
     *
     * @param id identificador único do cliente
     * @return dados do cliente
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE', 'MECANICO')")
    @Operation(summary = "Buscar cliente por ID", description = "Retorna dados completos de um cliente específico")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Cliente encontrado"),
        @ApiResponse(responseCode = "404", description = "Cliente não encontrado"),
        @ApiResponse(responseCode = "403", description = "Acesso negado")
    })
    public ResponseEntity<ClienteResponse> findById(
        @Parameter(description = "ID do cliente") @PathVariable UUID id
    ) {
        ClienteResponse cliente = clienteService.findById(id);
        return ResponseEntity.ok(cliente);
    }

    /**
     * Busca cliente por CPF/CNPJ.
     *
     * @param cpfCnpj CPF ou CNPJ formatado
     * @return dados do cliente
     */
    @GetMapping("/cpf-cnpj/{cpfCnpj}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE', 'MECANICO')")
    @Operation(summary = "Buscar cliente por CPF/CNPJ", description = "Retorna cliente com CPF/CNPJ específico")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Cliente encontrado"),
        @ApiResponse(responseCode = "404", description = "Cliente não encontrado"),
        @ApiResponse(responseCode = "403", description = "Acesso negado")
    })
    public ResponseEntity<ClienteResponse> findByCpfCnpj(
        @Parameter(description = "CPF (000.000.000-00) ou CNPJ (00.000.000/0000-00)") @PathVariable String cpfCnpj
    ) {
        ClienteResponse cliente = clienteService.findByCpfCnpj(cpfCnpj);
        return ResponseEntity.ok(cliente);
    }

    /**
     * Atualiza dados de um cliente.
     *
     * @param id identificador do cliente
     * @param request dados atualizados
     * @return cliente atualizado
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE')")
    @Operation(summary = "Atualizar cliente", description = "Atualiza dados de um cliente existente (CPF/CNPJ e tipo não podem ser alterados)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Cliente atualizado com sucesso"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos"),
        @ApiResponse(responseCode = "404", description = "Cliente não encontrado"),
        @ApiResponse(responseCode = "403", description = "Acesso negado")
    })
    public ResponseEntity<ClienteResponse> update(
        @Parameter(description = "ID do cliente") @PathVariable UUID id,
        @Valid @RequestBody UpdateClienteRequest request
    ) {
        ClienteResponse cliente = clienteService.update(id, request);
        return ResponseEntity.ok(cliente);
    }

    /**
     * Desativa um cliente (soft delete).
     *
     * @param id identificador do cliente
     * @return HTTP 204 (No Content)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE')")
    @Operation(summary = "Desativar cliente", description = "Marca cliente como inativo (soft delete)")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Cliente desativado com sucesso"),
        @ApiResponse(responseCode = "404", description = "Cliente não encontrado"),
        @ApiResponse(responseCode = "403", description = "Acesso negado")
    })
    public ResponseEntity<Void> delete(
        @Parameter(description = "ID do cliente") @PathVariable UUID id
    ) {
        clienteService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Reativa um cliente previamente desativado.
     *
     * @param id identificador do cliente
     * @return cliente reativado
     */
    @PatchMapping("/{id}/reativar")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE')")
    @Operation(summary = "Reativar cliente", description = "Reativa um cliente previamente desativado")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Cliente reativado com sucesso"),
        @ApiResponse(responseCode = "404", description = "Cliente não encontrado"),
        @ApiResponse(responseCode = "403", description = "Acesso negado")
    })
    public ResponseEntity<ClienteResponse> reativar(
        @Parameter(description = "ID do cliente") @PathVariable UUID id
    ) {
        ClienteResponse cliente = clienteService.reativar(id);
        return ResponseEntity.ok(cliente);
    }

    /**
     * Retorna lista de estados (UF) únicos.
     *
     * @return lista de siglas de estados
     */
    @GetMapping("/filtros/estados")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE', 'MECANICO')")
    @Operation(summary = "Listar estados", description = "Retorna lista de UFs presentes nos endereços de clientes")
    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    public ResponseEntity<List<String>> findEstados() {
        List<String> estados = clienteService.findEstados();
        return ResponseEntity.ok(estados);
    }

    /**
     * Retorna lista de cidades únicas.
     *
     * @return lista de nomes de cidades
     */
    @GetMapping("/filtros/cidades")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE', 'MECANICO')")
    @Operation(summary = "Listar cidades", description = "Retorna lista de cidades presentes nos endereços de clientes")
    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    public ResponseEntity<List<String>> findCidades() {
        List<String> cidades = clienteService.findCidades();
        return ResponseEntity.ok(cidades);
    }

    /**
     * Retorna estatísticas de clientes (totais por tipo).
     *
     * @return mapa com contadores
     */
    @GetMapping("/estatisticas")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE')")
    @Operation(summary = "Estatísticas de clientes", description = "Retorna totais de clientes por tipo")
    @ApiResponse(responseCode = "200", description = "Estatísticas retornadas com sucesso")
    public ResponseEntity<Map<String, Long>> getEstatisticas() {
        long totalPF = clienteService.countByTipo(TipoCliente.PESSOA_FISICA);
        long totalPJ = clienteService.countByTipo(TipoCliente.PESSOA_JURIDICA);

        Map<String, Long> stats = Map.of(
            "pessoasFisicas", totalPF,
            "pessoasJuridicas", totalPJ,
            "total", totalPF + totalPJ
        );

        return ResponseEntity.ok(stats);
    }
}
