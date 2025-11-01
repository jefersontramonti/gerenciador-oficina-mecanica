package com.pitstop.veiculo.controller;

import com.pitstop.veiculo.dto.*;
import com.pitstop.veiculo.service.VeiculoService;
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
 * Controller REST para gerenciamento de veículos.
 *
 * <p>Endpoints disponíveis:</p>
 * <ul>
 *   <li>POST /api/veiculos - Criar veículo</li>
 *   <li>GET /api/veiculos - Listar veículos (com filtros e paginação)</li>
 *   <li>GET /api/veiculos/{id} - Buscar por ID</li>
 *   <li>GET /api/veiculos/placa/{placa} - Buscar por placa</li>
 *   <li>GET /api/veiculos/cliente/{clienteId} - Listar veículos de um cliente</li>
 *   <li>PUT /api/veiculos/{id} - Atualizar veículo</li>
 *   <li>PATCH /api/veiculos/{id}/quilometragem - Atualizar quilometragem</li>
 *   <li>DELETE /api/veiculos/{id} - Remover veículo</li>
 *   <li>GET /api/veiculos/filtros/marcas - Listar marcas</li>
 *   <li>GET /api/veiculos/filtros/modelos - Listar modelos</li>
 *   <li>GET /api/veiculos/filtros/anos - Listar anos</li>
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
@RequestMapping("/api/veiculos")
@RequiredArgsConstructor
@Tag(name = "Veículos", description = "Gerenciamento de veículos vinculados a clientes")
public class VeiculoController {

    private final VeiculoService veiculoService;

    /**
     * Cria um novo veículo.
     *
     * @param request dados do veículo
     * @return veículo criado (HTTP 201)
     */
    @PostMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE')")
    @Operation(summary = "Criar novo veículo", description = "Cadastra um novo veículo vinculado a um cliente")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Veículo criado com sucesso"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos"),
        @ApiResponse(responseCode = "404", description = "Cliente não encontrado"),
        @ApiResponse(responseCode = "409", description = "Placa já cadastrada"),
        @ApiResponse(responseCode = "403", description = "Acesso negado")
    })
    public ResponseEntity<VeiculoResponseDTO> create(@Valid @RequestBody VeiculoRequestDTO request) {
        VeiculoResponseDTO response = veiculoService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Lista veículos com paginação e filtros opcionais.
     *
     * @param clienteId filtro de cliente (opcional)
     * @param placa filtro de placa parcial (opcional)
     * @param marca filtro de marca (opcional)
     * @param modelo filtro de modelo (opcional)
     * @param ano filtro de ano (opcional)
     * @param pageable configuração de paginação
     * @return página de veículos
     */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE', 'MECANICO')")
    @Operation(summary = "Listar veículos", description = "Lista veículos com suporte a filtros e paginação")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso"),
        @ApiResponse(responseCode = "403", description = "Acesso negado")
    })
    public ResponseEntity<Page<VeiculoResponseDTO>> findAll(
        @Parameter(description = "Filtro por ID do cliente") @RequestParam(required = false) UUID clienteId,
        @Parameter(description = "Filtro por placa (busca parcial)") @RequestParam(required = false) String placa,
        @Parameter(description = "Filtro por marca") @RequestParam(required = false) String marca,
        @Parameter(description = "Filtro por modelo") @RequestParam(required = false) String modelo,
        @Parameter(description = "Filtro por ano de fabricação") @RequestParam(required = false) Integer ano,
        @PageableDefault(size = 20, sort = "placa") Pageable pageable
    ) {
        Page<VeiculoResponseDTO> veiculos;

        // Se há filtros, usa busca com filtros; senão, lista todos
        if (clienteId != null || placa != null || marca != null || modelo != null || ano != null) {
            veiculos = veiculoService.findByFiltros(clienteId, placa, marca, modelo, ano, pageable);
        } else {
            veiculos = veiculoService.findAll(pageable);
        }

        return ResponseEntity.ok(veiculos);
    }

    /**
     * Busca veículo por ID.
     *
     * @param id identificador único do veículo
     * @return dados do veículo
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE', 'MECANICO')")
    @Operation(summary = "Buscar veículo por ID", description = "Retorna dados completos de um veículo específico")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Veículo encontrado"),
        @ApiResponse(responseCode = "404", description = "Veículo não encontrado"),
        @ApiResponse(responseCode = "403", description = "Acesso negado")
    })
    public ResponseEntity<VeiculoResponseDTO> findById(
        @Parameter(description = "ID do veículo") @PathVariable UUID id
    ) {
        VeiculoResponseDTO veiculo = veiculoService.findById(id);
        return ResponseEntity.ok(veiculo);
    }

    /**
     * Busca veículo por placa.
     *
     * @param placa placa do veículo (com ou sem hífen)
     * @return dados do veículo
     */
    @GetMapping("/placa/{placa}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE', 'MECANICO')")
    @Operation(summary = "Buscar veículo por placa", description = "Retorna veículo com placa específica")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Veículo encontrado"),
        @ApiResponse(responseCode = "404", description = "Veículo não encontrado"),
        @ApiResponse(responseCode = "403", description = "Acesso negado")
    })
    public ResponseEntity<VeiculoResponseDTO> findByPlaca(
        @Parameter(description = "Placa do veículo (ex: ABC1234 ou ABC-1234)") @PathVariable String placa
    ) {
        VeiculoResponseDTO veiculo = veiculoService.findByPlaca(placa);
        return ResponseEntity.ok(veiculo);
    }

    /**
     * Lista veículos de um cliente específico.
     *
     * @param clienteId ID do cliente
     * @param pageable configuração de paginação
     * @return página de veículos do cliente
     */
    @GetMapping("/cliente/{clienteId}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE', 'MECANICO')")
    @Operation(summary = "Listar veículos de um cliente", description = "Retorna todos os veículos vinculados a um cliente")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso"),
        @ApiResponse(responseCode = "404", description = "Cliente não encontrado"),
        @ApiResponse(responseCode = "403", description = "Acesso negado")
    })
    public ResponseEntity<Page<VeiculoResponseDTO>> findByClienteId(
        @Parameter(description = "ID do cliente") @PathVariable UUID clienteId,
        @PageableDefault(size = 20, sort = "placa") Pageable pageable
    ) {
        Page<VeiculoResponseDTO> veiculos = veiculoService.findByClienteId(clienteId, pageable);
        return ResponseEntity.ok(veiculos);
    }

    /**
     * Atualiza dados de um veículo.
     *
     * @param id identificador do veículo
     * @param request dados atualizados
     * @return veículo atualizado
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE')")
    @Operation(summary = "Atualizar veículo", description = "Atualiza dados de um veículo existente (placa e cliente não podem ser alterados)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Veículo atualizado com sucesso"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos"),
        @ApiResponse(responseCode = "404", description = "Veículo não encontrado"),
        @ApiResponse(responseCode = "403", description = "Acesso negado")
    })
    public ResponseEntity<VeiculoResponseDTO> update(
        @Parameter(description = "ID do veículo") @PathVariable UUID id,
        @Valid @RequestBody VeiculoUpdateDTO request
    ) {
        VeiculoResponseDTO veiculo = veiculoService.update(id, request);
        return ResponseEntity.ok(veiculo);
    }

    /**
     * Atualiza apenas a quilometragem do veículo.
     *
     * @param id identificador do veículo
     * @param request nova quilometragem
     * @return veículo atualizado
     */
    @PatchMapping("/{id}/quilometragem")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE', 'MECANICO')")
    @Operation(summary = "Atualizar quilometragem", description = "Atualiza apenas a quilometragem do veículo (não permite redução)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Quilometragem atualizada com sucesso"),
        @ApiResponse(responseCode = "400", description = "Quilometragem inválida (menor que a atual)"),
        @ApiResponse(responseCode = "404", description = "Veículo não encontrado"),
        @ApiResponse(responseCode = "403", description = "Acesso negado")
    })
    public ResponseEntity<VeiculoResponseDTO> updateQuilometragem(
        @Parameter(description = "ID do veículo") @PathVariable UUID id,
        @Valid @RequestBody QuilometragemUpdateDTO request
    ) {
        VeiculoResponseDTO veiculo = veiculoService.updateQuilometragem(id, request);
        return ResponseEntity.ok(veiculo);
    }

    /**
     * Remove um veículo do sistema.
     *
     * @param id identificador do veículo
     * @return HTTP 204 (No Content)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE')")
    @Operation(summary = "Remover veículo", description = "Remove veículo do sistema (não permitido se houver ordens de serviço vinculadas)")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Veículo removido com sucesso"),
        @ApiResponse(responseCode = "404", description = "Veículo não encontrado"),
        @ApiResponse(responseCode = "409", description = "Veículo possui ordens de serviço vinculadas"),
        @ApiResponse(responseCode = "403", description = "Acesso negado")
    })
    public ResponseEntity<Void> delete(
        @Parameter(description = "ID do veículo") @PathVariable UUID id
    ) {
        veiculoService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Retorna lista de marcas únicas.
     *
     * @return lista de nomes de marcas
     */
    @GetMapping("/filtros/marcas")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE', 'MECANICO')")
    @Operation(summary = "Listar marcas", description = "Retorna lista de marcas presentes nos veículos cadastrados")
    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    public ResponseEntity<List<String>> findMarcas() {
        List<String> marcas = veiculoService.findMarcas();
        return ResponseEntity.ok(marcas);
    }

    /**
     * Retorna lista de modelos únicos.
     *
     * @return lista de nomes de modelos
     */
    @GetMapping("/filtros/modelos")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE', 'MECANICO')")
    @Operation(summary = "Listar modelos", description = "Retorna lista de modelos presentes nos veículos cadastrados")
    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    public ResponseEntity<List<String>> findModelos() {
        List<String> modelos = veiculoService.findModelos();
        return ResponseEntity.ok(modelos);
    }

    /**
     * Retorna lista de anos únicos.
     *
     * @return lista de anos
     */
    @GetMapping("/filtros/anos")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE', 'MECANICO')")
    @Operation(summary = "Listar anos", description = "Retorna lista de anos de fabricação presentes nos veículos cadastrados")
    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    public ResponseEntity<List<Integer>> findAnos() {
        List<Integer> anos = veiculoService.findAnos();
        return ResponseEntity.ok(anos);
    }

    /**
     * Retorna estatísticas de veículos de um cliente.
     *
     * @param clienteId ID do cliente
     * @return mapa com estatísticas
     */
    @GetMapping("/cliente/{clienteId}/estatisticas")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE')")
    @Operation(summary = "Estatísticas de veículos do cliente", description = "Retorna total de veículos vinculados ao cliente")
    @ApiResponse(responseCode = "200", description = "Estatísticas retornadas com sucesso")
    public ResponseEntity<Map<String, Long>> getEstatisticasCliente(
        @Parameter(description = "ID do cliente") @PathVariable UUID clienteId
    ) {
        long totalVeiculos = veiculoService.countByClienteId(clienteId);

        Map<String, Long> stats = Map.of(
            "totalVeiculos", totalVeiculos
        );

        return ResponseEntity.ok(stats);
    }
}
