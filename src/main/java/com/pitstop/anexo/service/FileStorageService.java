package com.pitstop.anexo.service;

import com.pitstop.anexo.domain.EntidadeTipo;
import com.pitstop.anexo.exception.AnexoException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Serviço de infraestrutura para armazenamento de arquivos no filesystem.
 *
 * <p>Estrutura de diretórios:</p>
 * <pre>
 * {base-path}/
 * ├── oficina-{id}/
 * │   ├── ordens-servico/
 * │   │   └── 2026/01/{uuid}.jpg
 * │   ├── clientes/
 * │   │   └── {uuid}.pdf
 * │   └── pecas/
 * │       └── {uuid}.jpg
 * </pre>
 */
@Service
@Slf4j
public class FileStorageService {

    @Value("${app.storage.base-path:./uploads}")
    private String basePath;

    private Path baseLocation;

    @PostConstruct
    public void init() {
        try {
            baseLocation = Paths.get(basePath).toAbsolutePath().normalize();
            Files.createDirectories(baseLocation);
            log.info("Storage inicializado em: {}", baseLocation);
        } catch (IOException e) {
            throw new AnexoException("Não foi possível criar o diretório de uploads: " + basePath, e);
        }
    }

    /**
     * Salva um arquivo no storage.
     *
     * @param file Arquivo a ser salvo
     * @param oficinaId ID da oficina (tenant)
     * @param entidadeTipo Tipo de entidade
     * @return Caminho relativo do arquivo salvo
     */
    public String salvarArquivo(MultipartFile file, UUID oficinaId, EntidadeTipo entidadeTipo) {
        try {
            if (file.isEmpty()) {
                throw new AnexoException("Arquivo vazio");
            }

            // Gera nome único para o arquivo
            String nomeArquivo = gerarNomeArquivo(file.getOriginalFilename());

            // Monta o caminho: oficina-{id}/{tipo}/{ano}/{mes}/{arquivo}
            String caminhoRelativo = montarCaminhoRelativo(oficinaId, entidadeTipo, nomeArquivo);
            Path caminhoCompleto = baseLocation.resolve(caminhoRelativo).normalize();

            // Garante que o caminho está dentro do base path (segurança)
            if (!caminhoCompleto.startsWith(baseLocation)) {
                throw new AnexoException("Tentativa de acesso a caminho não permitido");
            }

            // Cria diretórios se necessário
            Files.createDirectories(caminhoCompleto.getParent());

            // Salva o arquivo
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, caminhoCompleto, StandardCopyOption.REPLACE_EXISTING);
            }

            log.info("Arquivo salvo: {} ({} bytes)", caminhoRelativo, file.getSize());
            return caminhoRelativo;

        } catch (IOException e) {
            throw new AnexoException("Erro ao salvar arquivo: " + e.getMessage(), e);
        }
    }

    /**
     * Carrega um arquivo do storage.
     *
     * @param caminhoRelativo Caminho relativo do arquivo
     * @return Resource do arquivo
     */
    public Resource carregarArquivo(String caminhoRelativo) {
        try {
            Path caminho = baseLocation.resolve(caminhoRelativo).normalize();

            // Verifica segurança
            if (!caminho.startsWith(baseLocation)) {
                throw new AnexoException("Tentativa de acesso a caminho não permitido");
            }

            Resource resource = new UrlResource(caminho.toUri());

            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new AnexoException("Arquivo não encontrado: " + caminhoRelativo);
            }
        } catch (MalformedURLException e) {
            throw new AnexoException("Erro ao carregar arquivo: " + caminhoRelativo, e);
        }
    }

    /**
     * Deleta um arquivo do storage.
     *
     * @param caminhoRelativo Caminho relativo do arquivo
     * @return true se deletado com sucesso
     */
    public boolean deletarArquivo(String caminhoRelativo) {
        try {
            Path caminho = baseLocation.resolve(caminhoRelativo).normalize();

            // Verifica segurança
            if (!caminho.startsWith(baseLocation)) {
                throw new AnexoException("Tentativa de acesso a caminho não permitido");
            }

            boolean deletado = Files.deleteIfExists(caminho);

            if (deletado) {
                log.info("Arquivo deletado: {}", caminhoRelativo);
                // Tenta limpar diretórios vazios
                limparDiretoriosVazios(caminho.getParent());
            }

            return deletado;
        } catch (IOException e) {
            log.error("Erro ao deletar arquivo: {}", caminhoRelativo, e);
            return false;
        }
    }

    /**
     * Verifica se um arquivo existe.
     */
    public boolean arquivoExiste(String caminhoRelativo) {
        Path caminho = baseLocation.resolve(caminhoRelativo).normalize();
        return Files.exists(caminho);
    }

    /**
     * Retorna o caminho absoluto de um arquivo.
     */
    public Path getCaminhoAbsoluto(String caminhoRelativo) {
        return baseLocation.resolve(caminhoRelativo).normalize();
    }

    /**
     * Monta o caminho relativo do arquivo.
     * Formato: oficina-{id}/{tipo}/{ano}/{mes}/{arquivo}
     */
    private String montarCaminhoRelativo(UUID oficinaId, EntidadeTipo entidadeTipo, String nomeArquivo) {
        LocalDate hoje = LocalDate.now();
        String tipoPasta = switch (entidadeTipo) {
            case ORDEM_SERVICO -> "ordens-servico";
            case CLIENTE -> "clientes";
            case PECA -> "pecas";
        };

        return String.format("oficina-%s/%s/%d/%02d/%s",
                oficinaId.toString(),
                tipoPasta,
                hoje.getYear(),
                hoje.getMonthValue(),
                nomeArquivo
        );
    }

    /**
     * Gera nome único para o arquivo.
     * Formato: {uuid}.{extensao}
     */
    private String gerarNomeArquivo(String nomeOriginal) {
        String extensao = "";
        if (nomeOriginal != null && nomeOriginal.contains(".")) {
            extensao = nomeOriginal.substring(nomeOriginal.lastIndexOf(".")).toLowerCase();
        }
        return UUID.randomUUID().toString() + extensao;
    }

    /**
     * Limpa diretórios vazios após deletar um arquivo.
     */
    private void limparDiretoriosVazios(Path diretorio) {
        try {
            while (diretorio != null && !diretorio.equals(baseLocation)) {
                if (Files.isDirectory(diretorio) && isDiretorioVazio(diretorio)) {
                    Files.delete(diretorio);
                    log.debug("Diretório vazio removido: {}", diretorio);
                    diretorio = diretorio.getParent();
                } else {
                    break;
                }
            }
        } catch (IOException e) {
            log.warn("Erro ao limpar diretórios vazios: {}", e.getMessage());
        }
    }

    private boolean isDiretorioVazio(Path diretorio) throws IOException {
        try (var stream = Files.list(diretorio)) {
            return stream.findAny().isEmpty();
        }
    }
}
