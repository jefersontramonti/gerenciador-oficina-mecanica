package com.pitstop.saas.service;

import com.pitstop.oficina.domain.Oficina;
import com.pitstop.oficina.domain.StatusOficina;
import com.pitstop.oficina.repository.OficinaRepository;
import com.pitstop.saas.domain.*;
import com.pitstop.saas.dto.*;
import com.pitstop.saas.repository.ComunicadoLeituraRepository;
import com.pitstop.saas.repository.ComunicadoRepository;
import com.pitstop.shared.exception.BusinessException;
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
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ComunicadoService {

    private final ComunicadoRepository comunicadoRepository;
    private final ComunicadoLeituraRepository leituraRepository;
    private final OficinaRepository oficinaRepository;

    /**
     * Lista comunicados com filtros
     */
    @Transactional(readOnly = true)
    public Page<ComunicadoDTO> listarComunicados(ComunicadoFilterRequest filter) {
        Pageable pageable = PageRequest.of(filter.page(), filter.size());

        // Convert enums to strings for native query
        String statusStr = filter.status() != null ? filter.status().name() : null;
        String tipoStr = filter.tipo() != null ? filter.tipo().name() : null;
        String prioridadeStr = filter.prioridade() != null ? filter.prioridade().name() : null;

        Page<Comunicado> comunicados = comunicadoRepository.findWithFilters(
            statusStr,
            tipoStr,
            prioridadeStr,
            filter.busca(),
            pageable
        );

        return comunicados.map(ComunicadoDTO::fromEntity);
    }

    /**
     * Busca comunicado por ID
     */
    @Transactional(readOnly = true)
    public ComunicadoDetailDTO buscarPorId(UUID id) {
        Comunicado comunicado = comunicadoRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Comunicado não encontrado"));

        // Busca leituras recentes (top 10)
        List<ComunicadoLeitura> leituras = leituraRepository.findByComunicadoId(id);
        List<ComunicadoDetailDTO.LeituraResumoDTO> leiturasResumo = leituras.stream()
            .limit(10)
            .map(l -> new ComunicadoDetailDTO.LeituraResumoDTO(
                l.getOficina().getId(),
                l.getOficina().getNomeFantasia(),
                l.getVisualizado(),
                l.getDataVisualizacao(),
                l.getConfirmado(),
                l.getDataConfirmacao()
            ))
            .toList();

        return ComunicadoDetailDTO.fromEntity(comunicado, leiturasResumo);
    }

    /**
     * Cria um novo comunicado
     */
    @Transactional
    public ComunicadoDTO criarComunicado(CreateComunicadoRequest request) {
        var userDetails = (CustomUserDetails) SecurityUtils.getCurrentUser();
        var usuario = userDetails.getUsuario();

        Comunicado comunicado = Comunicado.builder()
            .titulo(request.titulo())
            .resumo(request.resumo())
            .conteudo(request.conteudo())
            .tipo(request.tipo())
            .prioridade(request.prioridade())
            .autorId(usuario.getId())
            .autorNome(usuario.getNome())
            .planosAlvo(request.planosAlvo())
            .oficinasAlvo(request.oficinasAlvo())
            .statusOficinasAlvo(request.statusOficinasAlvo())
            .requerConfirmacao(request.requerConfirmacao() != null ? request.requerConfirmacao() : false)
            .exibirNoLogin(request.exibirNoLogin() != null ? request.exibirNoLogin() : false)
            .status(StatusComunicado.RASCUNHO)
            .build();

        comunicado = comunicadoRepository.save(comunicado);

        // Se enviar agora
        if (Boolean.TRUE.equals(request.enviarAgora())) {
            enviarComunicado(comunicado.getId());
            comunicado = comunicadoRepository.findById(comunicado.getId()).orElseThrow();
        }
        // Se agendar
        else if (request.dataAgendamento() != null) {
            agendarComunicado(comunicado.getId(), request.dataAgendamento());
            comunicado = comunicadoRepository.findById(comunicado.getId()).orElseThrow();
        }

        log.info("Comunicado criado: {} por {}", comunicado.getId(), usuario.getEmail());
        return ComunicadoDTO.fromEntity(comunicado);
    }

    /**
     * Atualiza um comunicado (apenas rascunhos e agendados)
     */
    @Transactional
    public ComunicadoDTO atualizarComunicado(UUID id, UpdateComunicadoRequest request) {
        Comunicado comunicado = comunicadoRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Comunicado não encontrado"));

        if (!comunicado.podeEditar()) {
            throw new BusinessException("Este comunicado não pode ser editado");
        }

        comunicado.setTitulo(request.titulo());
        comunicado.setResumo(request.resumo());
        comunicado.setConteudo(request.conteudo());

        if (request.tipo() != null) {
            comunicado.setTipo(request.tipo());
        }
        if (request.prioridade() != null) {
            comunicado.setPrioridade(request.prioridade());
        }

        comunicado.setPlanosAlvo(request.planosAlvo());
        comunicado.setOficinasAlvo(request.oficinasAlvo());
        comunicado.setStatusOficinasAlvo(request.statusOficinasAlvo());

        if (request.requerConfirmacao() != null) {
            comunicado.setRequerConfirmacao(request.requerConfirmacao());
        }
        if (request.exibirNoLogin() != null) {
            comunicado.setExibirNoLogin(request.exibirNoLogin());
        }

        // Reagendar se necessário
        if (request.dataAgendamento() != null) {
            comunicado.agendar(request.dataAgendamento());
        }

        comunicado = comunicadoRepository.save(comunicado);
        log.info("Comunicado atualizado: {}", comunicado.getId());
        return ComunicadoDTO.fromEntity(comunicado);
    }

    /**
     * Envia um comunicado imediatamente
     */
    @Transactional
    public ComunicadoDTO enviarComunicado(UUID id) {
        Comunicado comunicado = comunicadoRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Comunicado não encontrado"));

        if (!comunicado.podeEnviar() && !comunicado.isAgendado()) {
            throw new BusinessException("Este comunicado não pode ser enviado");
        }

        // Busca destinatários
        List<Oficina> destinatarios = buscarDestinatarios(comunicado);

        if (destinatarios.isEmpty()) {
            throw new BusinessException("Nenhum destinatário encontrado para os filtros selecionados");
        }

        // Cria registros de leitura para cada oficina
        for (Oficina oficina : destinatarios) {
            if (!leituraRepository.existsByComunicadoIdAndOficinaId(comunicado.getId(), oficina.getId())) {
                ComunicadoLeitura leitura = ComunicadoLeitura.builder()
                    .comunicado(comunicado)
                    .oficina(oficina)
                    .visualizado(false)
                    .confirmado(false)
                    .build();
                leituraRepository.save(leitura);
            }
        }

        // Atualiza o comunicado
        comunicado.enviar(destinatarios.size());
        comunicado = comunicadoRepository.save(comunicado);

        log.info("Comunicado {} enviado para {} oficinas", comunicado.getId(), destinatarios.size());
        return ComunicadoDTO.fromEntity(comunicado);
    }

    /**
     * Agenda um comunicado para envio futuro
     */
    @Transactional
    public ComunicadoDTO agendarComunicado(UUID id, OffsetDateTime dataAgendamento) {
        Comunicado comunicado = comunicadoRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Comunicado não encontrado"));

        if (!comunicado.isRascunho() && !comunicado.isAgendado()) {
            throw new BusinessException("Este comunicado não pode ser agendado");
        }

        if (dataAgendamento.isBefore(OffsetDateTime.now())) {
            throw new BusinessException("A data de agendamento deve ser no futuro");
        }

        comunicado.agendar(dataAgendamento);
        comunicado = comunicadoRepository.save(comunicado);

        log.info("Comunicado {} agendado para {}", comunicado.getId(), dataAgendamento);
        return ComunicadoDTO.fromEntity(comunicado);
    }

    /**
     * Cancela um comunicado agendado
     */
    @Transactional
    public ComunicadoDTO cancelarComunicado(UUID id) {
        Comunicado comunicado = comunicadoRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Comunicado não encontrado"));

        if (!comunicado.podeCancelar()) {
            throw new BusinessException("Este comunicado não pode ser cancelado");
        }

        comunicado.cancelar();
        comunicado = comunicadoRepository.save(comunicado);

        log.info("Comunicado {} cancelado", comunicado.getId());
        return ComunicadoDTO.fromEntity(comunicado);
    }

    /**
     * Exclui um comunicado (apenas rascunhos)
     */
    @Transactional
    public void excluirComunicado(UUID id) {
        Comunicado comunicado = comunicadoRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Comunicado não encontrado"));

        if (!comunicado.isRascunho()) {
            throw new BusinessException("Apenas rascunhos podem ser excluídos");
        }

        comunicadoRepository.delete(comunicado);
        log.info("Comunicado {} excluído", id);
    }

    /**
     * Retorna métricas dos comunicados
     */
    @Transactional(readOnly = true)
    public ComunicadoMetricasDTO getMetricas() {
        OffsetDateTime inicioMes = OffsetDateTime.now().truncatedTo(ChronoUnit.DAYS).withDayOfMonth(1);

        long totalRascunhos = comunicadoRepository.countByStatus(StatusComunicado.RASCUNHO);
        long totalAgendados = comunicadoRepository.countByStatus(StatusComunicado.AGENDADO);
        long totalEnviados = comunicadoRepository.countByStatus(StatusComunicado.ENVIADO);
        long totalCancelados = comunicadoRepository.countByStatus(StatusComunicado.CANCELADO);

        long enviadosNoMes = comunicadoRepository.countEnviadosDesde(inicioMes);
        long destinatariosNoMes = comunicadoRepository.countDestinatariosDesde(inicioMes);
        long visualizacoesNoMes = comunicadoRepository.countVisualizacoesDesde(inicioMes);

        double taxaVisualizacaoMedia = destinatariosNoMes > 0
            ? (double) visualizacoesNoMes / destinatariosNoMes * 100
            : 0;

        return new ComunicadoMetricasDTO(
            totalRascunhos,
            totalAgendados,
            totalEnviados,
            totalCancelados,
            enviadosNoMes,
            destinatariosNoMes,
            visualizacoesNoMes,
            taxaVisualizacaoMedia
        );
    }

    /**
     * Processa comunicados agendados (chamado por job)
     */
    @Transactional
    public int processarAgendados() {
        List<Comunicado> agendados = comunicadoRepository.findAgendadosParaEnvio(OffsetDateTime.now());
        int count = 0;

        for (Comunicado comunicado : agendados) {
            try {
                enviarComunicado(comunicado.getId());
                count++;
            } catch (Exception e) {
                log.error("Erro ao enviar comunicado agendado {}: {}", comunicado.getId(), e.getMessage());
            }
        }

        if (count > 0) {
            log.info("{} comunicados agendados foram enviados", count);
        }

        return count;
    }

    /**
     * Busca oficinas destinatárias baseado nos filtros
     */
    private List<Oficina> buscarDestinatarios(Comunicado comunicado) {
        List<Oficina> todasOficinas;

        // Se tem oficinas específicas, usa elas
        if (comunicado.getOficinasAlvo() != null && comunicado.getOficinasAlvo().length > 0) {
            todasOficinas = oficinaRepository.findAllById(Arrays.asList(comunicado.getOficinasAlvo()));
        } else {
            todasOficinas = oficinaRepository.findAll();
        }

        // Filtra por planos
        if (comunicado.getPlanosAlvo() != null && comunicado.getPlanosAlvo().length > 0) {
            Set<String> planosSet = Set.of(comunicado.getPlanosAlvo());
            todasOficinas = todasOficinas.stream()
                .filter(o -> o.getPlano() != null && planosSet.contains(o.getPlano().name()))
                .toList();
        }

        // Filtra por status
        if (comunicado.getStatusOficinasAlvo() != null && comunicado.getStatusOficinasAlvo().length > 0) {
            Set<String> statusSet = Set.of(comunicado.getStatusOficinasAlvo());
            todasOficinas = todasOficinas.stream()
                .filter(o -> statusSet.contains(o.getStatus().name()))
                .toList();
        }

        return todasOficinas;
    }

    /**
     * Retorna os enums para uso no frontend
     */
    public ComunicadoEnumsDTO getEnums() {
        return new ComunicadoEnumsDTO(
            TipoComunicado.values(),
            PrioridadeComunicado.values(),
            StatusComunicado.values()
        );
    }

    public record ComunicadoEnumsDTO(
        TipoComunicado[] tipos,
        PrioridadeComunicado[] prioridades,
        StatusComunicado[] status
    ) {}
}
