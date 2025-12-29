package com.pitstop.saas.service;

import com.pitstop.saas.domain.ComunicadoLeitura;
import com.pitstop.saas.dto.ComunicadoOficinaDTO;
import com.pitstop.saas.dto.ComunicadoOficinaDetailDTO;
import com.pitstop.saas.dto.ComunicadoAlertDTO;
import com.pitstop.saas.repository.ComunicadoLeituraRepository;
import com.pitstop.shared.exception.ResourceNotFoundException;
import com.pitstop.shared.security.CustomUserDetails;
import com.pitstop.shared.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ComunicadoOficinaService {

    private final ComunicadoLeituraRepository leituraRepository;

    /**
     * Retorna o ID da oficina do usuário logado.
     * Retorna null para SUPER_ADMIN (não tem oficina).
     */
    private UUID getOficinaIdAtual() {
        var userDetails = (CustomUserDetails) SecurityUtils.getCurrentUser();
        var usuario = userDetails.getUsuario();

        // SUPER_ADMIN não tem oficina
        if (usuario.getPerfil().name().equals("SUPER_ADMIN")) {
            return null;
        }

        var oficina = usuario.getOficina();
        if (oficina == null) {
            throw new IllegalStateException("Usuário não está vinculado a uma oficina");
        }
        return oficina.getId();
    }

    /**
     * Verifica se o usuário logado é SUPER_ADMIN
     */
    private boolean isSuperAdmin() {
        var userDetails = (CustomUserDetails) SecurityUtils.getCurrentUser();
        return userDetails.getUsuario().getPerfil().name().equals("SUPER_ADMIN");
    }

    /**
     * Lista comunicados recebidos pela oficina
     */
    @Transactional(readOnly = true)
    public Page<ComunicadoOficinaDTO> listarComunicados(int page, int size) {
        UUID oficinaId = getOficinaIdAtual();
        Pageable pageable = PageRequest.of(page, size);

        Page<ComunicadoLeitura> leituras = leituraRepository.findComunicadosEnviadosParaOficina(oficinaId, pageable);

        return leituras.map(ComunicadoOficinaDTO::fromEntity);
    }

    /**
     * Busca detalhes de um comunicado e marca como lido
     */
    @Transactional
    public ComunicadoOficinaDetailDTO buscarEMarcarComoLido(UUID comunicadoId) {
        UUID oficinaId = getOficinaIdAtual();

        ComunicadoLeitura leitura = leituraRepository.findByComunicadoIdAndOficinaId(comunicadoId, oficinaId)
            .orElseThrow(() -> new ResourceNotFoundException("Comunicado não encontrado"));

        // Marca como lido se ainda não foi
        if (!leitura.getVisualizado()) {
            leitura.setVisualizado(true);
            leitura.setDataVisualizacao(OffsetDateTime.now());
            leituraRepository.save(leitura);

            // Incrementa contador no comunicado
            leitura.getComunicado().incrementarVisualizacoes();

            log.info("Comunicado {} marcado como lido pela oficina {}", comunicadoId, oficinaId);
        }

        return ComunicadoOficinaDetailDTO.fromEntity(leitura);
    }

    /**
     * Confirma leitura de um comunicado
     */
    @Transactional
    public void confirmarLeitura(UUID comunicadoId) {
        UUID oficinaId = getOficinaIdAtual();

        ComunicadoLeitura leitura = leituraRepository.findByComunicadoIdAndOficinaId(comunicadoId, oficinaId)
            .orElseThrow(() -> new ResourceNotFoundException("Comunicado não encontrado"));

        if (!leitura.getComunicado().getRequerConfirmacao()) {
            throw new IllegalStateException("Este comunicado não requer confirmação");
        }

        if (!leitura.getConfirmado()) {
            leitura.setConfirmado(true);
            leitura.setDataConfirmacao(OffsetDateTime.now());

            // Garante que também está marcado como visualizado
            if (!leitura.getVisualizado()) {
                leitura.setVisualizado(true);
                leitura.setDataVisualizacao(OffsetDateTime.now());
                leitura.getComunicado().incrementarVisualizacoes();
            }

            leituraRepository.save(leitura);

            // Incrementa contador de confirmações
            leitura.getComunicado().incrementarConfirmacoes();

            log.info("Comunicado {} confirmado pela oficina {}", comunicadoId, oficinaId);
        }
    }

    /**
     * Conta comunicados não lidos.
     * Retorna 0 para SUPER_ADMIN.
     */
    @Transactional(readOnly = true)
    public long contarNaoLidos() {
        if (isSuperAdmin()) {
            return 0L; // SUPER_ADMIN não tem comunicados de oficina
        }
        UUID oficinaId = getOficinaIdAtual();
        return leituraRepository.countByOficinaIdAndVisualizadoFalse(oficinaId);
    }

    /**
     * Retorna dados para alerta no dashboard
     */
    @Transactional(readOnly = true)
    public ComunicadoAlertDTO getAlertaDashboard() {
        UUID oficinaId = getOficinaIdAtual();

        long naoLidos = leituraRepository.countByOficinaIdAndVisualizadoFalse(oficinaId);
        List<ComunicadoLeitura> pendentesConfirmacao = leituraRepository.findPendentesConfirmacao(oficinaId);
        List<ComunicadoLeitura> urgentes = leituraRepository.findNaoLidosByOficinaId(oficinaId).stream()
            .filter(l -> l.getComunicado().getPrioridade().name().equals("URGENTE") ||
                         l.getComunicado().getPrioridade().name().equals("ALTA"))
            .limit(5)
            .toList();

        return new ComunicadoAlertDTO(
            naoLidos,
            pendentesConfirmacao.size(),
            urgentes.stream().map(ComunicadoOficinaDTO::fromEntity).toList()
        );
    }

    /**
     * Retorna comunicados para exibir no login (modal obrigatório)
     */
    @Transactional(readOnly = true)
    public List<ComunicadoOficinaDTO> getComunicadosParaLogin() {
        UUID oficinaId = getOficinaIdAtual();
        List<ComunicadoLeitura> leituras = leituraRepository.findParaExibirNoLogin(oficinaId);
        return leituras.stream().map(ComunicadoOficinaDTO::fromEntity).toList();
    }

    /**
     * Marca todos como lidos
     */
    @Transactional
    public void marcarTodosComoLidos() {
        UUID oficinaId = getOficinaIdAtual();
        List<ComunicadoLeitura> naoLidos = leituraRepository.findNaoLidosByOficinaId(oficinaId);

        OffsetDateTime agora = OffsetDateTime.now();
        for (ComunicadoLeitura leitura : naoLidos) {
            leitura.setVisualizado(true);
            leitura.setDataVisualizacao(agora);
            leitura.getComunicado().incrementarVisualizacoes();
        }

        leituraRepository.saveAll(naoLidos);
        log.info("{} comunicados marcados como lidos pela oficina {}", naoLidos.size(), oficinaId);
    }
}
