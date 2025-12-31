package com.pitstop.notificacao.controller;

import com.pitstop.notificacao.service.TempFileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller para servir arquivos temporarios.
 *
 * <p>Endpoint publico (sem autenticacao) para permitir que servicos
 * externos (WhatsApp, Telegram) acessem os arquivos.</p>
 *
 * @author PitStop Team
 */
@RestController
@RequestMapping("/api/public/files")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Arquivos Temporarios", description = "Endpoints para arquivos temporarios")
public class TempFileController {

    private final TempFileService tempFileService;

    /**
     * Retorna um arquivo temporario pelo token.
     *
     * @param token Token do arquivo
     * @return Arquivo ou 404 se nao encontrado
     */
    @GetMapping("/{token}")
    @Operation(summary = "Obter arquivo temporario", description = "Retorna um arquivo temporario pelo token")
    public ResponseEntity<byte[]> getFile(@PathVariable String token) {
        var file = tempFileService.retrieve(token);

        if (file == null) {
            log.debug("Arquivo temporario nao encontrado ou expirado: {}", token);
            return ResponseEntity.notFound().build();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(file.contentType()));
        headers.setContentDispositionFormData("attachment", file.fileName());
        headers.setContentLength(file.content().length);

        log.debug("Servindo arquivo temporario: {} ({} bytes)", file.fileName(), file.content().length);

        return new ResponseEntity<>(file.content(), headers, HttpStatus.OK);
    }

    /**
     * Verifica se um arquivo existe.
     *
     * @param token Token do arquivo
     * @return 200 se existe, 404 se nao
     */
    @GetMapping("/{token}/exists")
    @Operation(summary = "Verificar existencia", description = "Verifica se um arquivo temporario existe")
    public ResponseEntity<Void> checkExists(@PathVariable String token) {
        if (tempFileService.exists(token)) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
}
