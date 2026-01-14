package com.pitstop.anexo.controller;

import com.pitstop.anexo.domain.CategoriaAnexo;
import com.pitstop.anexo.domain.EntidadeTipo;
import com.pitstop.anexo.dto.AlterarVisibilidadeRequest;
import com.pitstop.anexo.dto.AnexoResponse;
import com.pitstop.anexo.dto.AnexoUploadRequest;
import com.pitstop.anexo.dto.QuotaResponse;
import com.pitstop.anexo.service.AnexoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * Controller REST para gerenciamento de anexos.
 *
 * <p>Endpoints:</p>
 * <ul>
 *   <li>POST /api/anexos/upload - Upload de arquivo</li>
 *   <li>GET /api/anexos/{id}/download - Download de arquivo</li>
 *   <li>GET /api/anexos/entidade/{tipo}/{id} - Lista anexos de uma entidade</li>
 *   <li>GET /api/anexos/{id} - Busca anexo por ID</li>
 *   <li>DELETE /api/anexos/{id} - Deleta anexo</li>
 *   <li>GET /api/anexos/quota - Informações de quota</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/anexos")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Anexos", description = "Gerenciamento de anexos (imagens e documentos)")
public class AnexoController {

    private final AnexoService anexoService;

    @Operation(summary = "Upload de arquivo", description = "Faz upload de uma imagem ou documento para uma entidade")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Upload realizado com sucesso"),
        @ApiResponse(responseCode = "400", description = "Arquivo inválido"),
        @ApiResponse(responseCode = "403", description = "Plano não permite anexos"),
        @ApiResponse(responseCode = "413", description = "Quota excedida"),
        @ApiResponse(responseCode = "415", description = "Tipo de arquivo não suportado")
    })
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE', 'MECANICO')")
    public ResponseEntity<AnexoResponse> upload(
            @Parameter(description = "Arquivo a ser enviado (max 5MB)")
            @RequestPart("file") MultipartFile file,

            @Parameter(description = "Tipo da entidade (ORDEM_SERVICO, CLIENTE, PECA)")
            @RequestParam("entidadeTipo") EntidadeTipo entidadeTipo,

            @Parameter(description = "ID da entidade")
            @RequestParam("entidadeId") UUID entidadeId,

            @Parameter(description = "Categoria do anexo")
            @RequestParam(value = "categoria", required = false) CategoriaAnexo categoria,

            @Parameter(description = "Descrição opcional")
            @RequestParam(value = "descricao", required = false) String descricao
    ) {
        AnexoUploadRequest request = new AnexoUploadRequest(entidadeTipo, entidadeId, categoria, descricao);
        AnexoResponse response = anexoService.upload(file, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Download de arquivo", description = "Faz download de um anexo")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Arquivo retornado"),
        @ApiResponse(responseCode = "404", description = "Anexo não encontrado")
    })
    @GetMapping("/{id}/download")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE', 'MECANICO')")
    public ResponseEntity<Resource> download(
            @Parameter(description = "ID do anexo")
            @PathVariable UUID id
    ) {
        Resource resource = anexoService.download(id);
        String nomeOriginal = anexoService.getNomeOriginal(id);
        String mimeType = anexoService.getMimeType(id);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(mimeType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nomeOriginal + "\"")
                .body(resource);
    }

    @Operation(summary = "Visualizar arquivo", description = "Retorna o arquivo para visualização inline")
    @GetMapping("/{id}/view")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE', 'MECANICO')")
    public ResponseEntity<Resource> view(
            @Parameter(description = "ID do anexo")
            @PathVariable UUID id
    ) {
        Resource resource = anexoService.download(id);
        String mimeType = anexoService.getMimeType(id);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(mimeType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .body(resource);
    }

    @Operation(summary = "Lista anexos de uma entidade", description = "Retorna todos os anexos de uma entidade específica")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de anexos")
    })
    @GetMapping("/entidade/{entidadeTipo}/{entidadeId}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE', 'MECANICO')")
    public ResponseEntity<List<AnexoResponse>> listarPorEntidade(
            @Parameter(description = "Tipo da entidade")
            @PathVariable EntidadeTipo entidadeTipo,

            @Parameter(description = "ID da entidade")
            @PathVariable UUID entidadeId
    ) {
        List<AnexoResponse> anexos = anexoService.listarPorEntidade(entidadeTipo, entidadeId);
        return ResponseEntity.ok(anexos);
    }

    @Operation(summary = "Busca anexo por ID", description = "Retorna os metadados de um anexo")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Anexo encontrado"),
        @ApiResponse(responseCode = "404", description = "Anexo não encontrado")
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE', 'MECANICO')")
    public ResponseEntity<AnexoResponse> buscarPorId(
            @Parameter(description = "ID do anexo")
            @PathVariable UUID id
    ) {
        AnexoResponse anexo = anexoService.buscarPorId(id);
        return ResponseEntity.ok(anexo);
    }

    @Operation(summary = "Deleta um anexo", description = "Remove um anexo (soft delete)")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Anexo deletado"),
        @ApiResponse(responseCode = "404", description = "Anexo não encontrado")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE')")
    public ResponseEntity<Void> deletar(
            @Parameter(description = "ID do anexo")
            @PathVariable UUID id
    ) {
        anexoService.deletar(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Informações de quota", description = "Retorna informações de uso de storage da oficina")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Informações de quota")
    })
    @GetMapping("/quota")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE')")
    public ResponseEntity<QuotaResponse> getQuota() {
        QuotaResponse quota = anexoService.getQuota();
        return ResponseEntity.ok(quota);
    }

    @Operation(summary = "Conta anexos de uma entidade", description = "Retorna a quantidade de anexos de uma entidade")
    @GetMapping("/entidade/{entidadeTipo}/{entidadeId}/count")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE', 'MECANICO')")
    public ResponseEntity<Long> contarPorEntidade(
            @PathVariable EntidadeTipo entidadeTipo,
            @PathVariable UUID entidadeId
    ) {
        long count = anexoService.contarPorEntidade(entidadeTipo, entidadeId);
        return ResponseEntity.ok(count);
    }

    @Operation(summary = "Altera visibilidade para cliente",
               description = "Marca ou desmarca um anexo como visível para o cliente na página de aprovação de orçamento")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Visibilidade alterada"),
        @ApiResponse(responseCode = "404", description = "Anexo não encontrado")
    })
    @PatchMapping("/{id}/visibilidade")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE')")
    public ResponseEntity<AnexoResponse> alterarVisibilidade(
            @Parameter(description = "ID do anexo")
            @PathVariable UUID id,

            @Valid @RequestBody AlterarVisibilidadeRequest request
    ) {
        AnexoResponse anexo = anexoService.alterarVisibilidade(id, request.visivelParaCliente());
        return ResponseEntity.ok(anexo);
    }
}
