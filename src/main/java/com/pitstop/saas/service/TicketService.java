package com.pitstop.saas.service;

import com.pitstop.oficina.domain.Oficina;
import com.pitstop.oficina.repository.OficinaRepository;
import com.pitstop.saas.domain.*;
import com.pitstop.saas.dto.*;
import com.pitstop.saas.repository.MensagemTicketRepository;
import com.pitstop.saas.repository.TicketRepository;
import com.pitstop.usuario.domain.Usuario;
import com.pitstop.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TicketService {

    private final TicketRepository ticketRepository;
    private final MensagemTicketRepository mensagemTicketRepository;
    private final OficinaRepository oficinaRepository;
    private final UsuarioRepository usuarioRepository;

    // ==================== CRUD ====================

    @Transactional
    public TicketDTO criarTicket(CreateTicketRequest request) {
        log.info("Criando ticket para {}", request.usuarioEmail());

        Ticket ticket = Ticket.builder()
            .numero(gerarNumeroTicket())
            .usuarioId(request.usuarioId())
            .usuarioNome(request.usuarioNome())
            .usuarioEmail(request.usuarioEmail())
            .tipo(request.tipo())
            .prioridade(request.prioridade())
            .status(StatusTicket.ABERTO)
            .assunto(request.assunto())
            .descricao(request.descricao())
            .anexos(request.anexos() != null ? request.anexos() : List.of())
            .build();

        // Definir SLA baseado na prioridade
        ticket.definirSlaPorPrioridade();

        // Associar oficina se fornecida
        if (request.oficinaId() != null) {
            Oficina oficina = oficinaRepository.findById(request.oficinaId())
                .orElseThrow(() -> new IllegalArgumentException("Oficina não encontrada"));
            ticket.setOficina(oficina);
        }

        // Atribuir se fornecido
        if (request.atribuidoA() != null) {
            Usuario atribuido = usuarioRepository.findById(request.atribuidoA())
                .orElseThrow(() -> new IllegalArgumentException("Usuário para atribuição não encontrado"));
            ticket.setAtribuidoA(atribuido);
        }

        ticket = ticketRepository.save(ticket);
        log.info("Ticket {} criado com sucesso", ticket.getNumero());

        return TicketDTO.fromEntity(ticket);
    }

    @Transactional(readOnly = true)
    public Page<TicketDTO> listarTickets(TicketFilterRequest filter) {
        Pageable pageable = PageRequest.of(
            filter.page(),
            filter.size(),
            Sort.by(Sort.Direction.DESC, "aberturaEm")
        );

        Page<Ticket> tickets = ticketRepository.findAllWithFilters(
            filter.oficinaId(),
            filter.status(),
            filter.tipo(),
            filter.prioridade(),
            filter.atribuidoA(),
            filter.busca(),
            pageable
        );

        return tickets.map(TicketDTO::fromEntitySimples);
    }

    @Transactional(readOnly = true)
    public TicketDetailDTO buscarTicketPorId(UUID id) {
        Ticket ticket = ticketRepository.findByIdWithMensagens(id)
            .orElseThrow(() -> new IllegalArgumentException("Ticket não encontrado"));
        return TicketDetailDTO.fromEntity(ticket);
    }

    @Transactional(readOnly = true)
    public TicketDetailDTO buscarTicketPorNumero(String numero) {
        Ticket ticket = ticketRepository.findByNumero(numero)
            .orElseThrow(() -> new IllegalArgumentException("Ticket não encontrado"));
        return TicketDetailDTO.fromEntity(ticket);
    }

    // ==================== Ações ====================

    @Transactional
    public MensagemTicketDTO responderTicket(UUID ticketId, ResponderTicketRequest request) {
        log.info("Respondendo ticket {}", ticketId);

        Ticket ticket = ticketRepository.findById(ticketId)
            .orElseThrow(() -> new IllegalArgumentException("Ticket não encontrado"));

        if (ticket.getStatus().isFinalizado()) {
            throw new IllegalStateException("Não é possível responder a um ticket finalizado");
        }

        MensagemTicket mensagem = MensagemTicket.builder()
            .ticket(ticket)
            .autorId(request.autorId())
            .autorNome(request.autorNome())
            .autorTipo(TipoAutorMensagem.SUPORTE)
            .isInterno(request.isInterno())
            .conteudo(request.conteudo())
            .anexos(request.anexos() != null ? request.anexos() : List.of())
            .build();

        ticket.adicionarMensagem(mensagem);

        // Se é a primeira resposta pública do suporte, registrar tempo de resposta
        if (!request.isInterno()) {
            ticket.registrarPrimeiraResposta();
            // Mudar status para EM_ANDAMENTO se estava ABERTO
            if (ticket.getStatus() == StatusTicket.ABERTO) {
                ticket.setStatus(StatusTicket.EM_ANDAMENTO);
            }
        }

        ticketRepository.save(ticket);
        log.info("Resposta adicionada ao ticket {}", ticket.getNumero());

        return MensagemTicketDTO.fromEntity(mensagem);
    }

    @Transactional
    public TicketDTO atribuirTicket(UUID ticketId, AtribuirTicketRequest request) {
        log.info("Atribuindo ticket {} para {}", ticketId, request.atribuidoA());

        Ticket ticket = ticketRepository.findById(ticketId)
            .orElseThrow(() -> new IllegalArgumentException("Ticket não encontrado"));

        if (request.atribuidoA() != null) {
            Usuario atribuido = usuarioRepository.findById(request.atribuidoA())
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
            ticket.setAtribuidoA(atribuido);

            // Adicionar mensagem de sistema
            MensagemTicket mensagemSistema = MensagemTicket.builder()
                .ticket(ticket)
                .autorNome("Sistema")
                .autorTipo(TipoAutorMensagem.SISTEMA)
                .isInterno(true)
                .conteudo("Ticket atribuído para " + atribuido.getNome())
                .build();
            ticket.adicionarMensagem(mensagemSistema);
        } else {
            // Desatribuir
            String nomeAnterior = ticket.getAtribuidoA() != null ? ticket.getAtribuidoA().getNome() : "ninguém";
            ticket.setAtribuidoA(null);

            MensagemTicket mensagemSistema = MensagemTicket.builder()
                .ticket(ticket)
                .autorNome("Sistema")
                .autorTipo(TipoAutorMensagem.SISTEMA)
                .isInterno(true)
                .conteudo("Ticket desatribuído (anteriormente: " + nomeAnterior + ")")
                .build();
            ticket.adicionarMensagem(mensagemSistema);
        }

        ticket = ticketRepository.save(ticket);
        return TicketDTO.fromEntity(ticket);
    }

    @Transactional
    public TicketDTO alterarStatus(UUID ticketId, AlterarStatusTicketRequest request) {
        log.info("Alterando status do ticket {} para {}", ticketId, request.status());

        Ticket ticket = ticketRepository.findById(ticketId)
            .orElseThrow(() -> new IllegalArgumentException("Ticket não encontrado"));

        StatusTicket statusAnterior = ticket.getStatus();
        ticket.setStatus(request.status());

        // Atualizar datas específicas
        if (request.status() == StatusTicket.RESOLVIDO) {
            ticket.setResolvidoEm(LocalDateTime.now());
        } else if (request.status() == StatusTicket.FECHADO) {
            ticket.setFechadoEm(LocalDateTime.now());
        }

        // Adicionar mensagem de sistema
        MensagemTicket mensagemSistema = MensagemTicket.builder()
            .ticket(ticket)
            .autorNome("Sistema")
            .autorTipo(TipoAutorMensagem.SISTEMA)
            .isInterno(true)
            .conteudo("Status alterado de " + statusAnterior.getDescricao() + " para " + request.status().getDescricao())
            .build();
        ticket.adicionarMensagem(mensagemSistema);

        ticket = ticketRepository.save(ticket);
        return TicketDTO.fromEntity(ticket);
    }

    @Transactional
    public TicketDTO alterarPrioridade(UUID ticketId, AlterarPrioridadeTicketRequest request) {
        log.info("Alterando prioridade do ticket {} para {}", ticketId, request.prioridade());

        Ticket ticket = ticketRepository.findById(ticketId)
            .orElseThrow(() -> new IllegalArgumentException("Ticket não encontrado"));

        PrioridadeTicket prioridadeAnterior = ticket.getPrioridade();
        ticket.setPrioridade(request.prioridade());
        ticket.definirSlaPorPrioridade();

        // Adicionar mensagem de sistema
        MensagemTicket mensagemSistema = MensagemTicket.builder()
            .ticket(ticket)
            .autorNome("Sistema")
            .autorTipo(TipoAutorMensagem.SISTEMA)
            .isInterno(true)
            .conteudo("Prioridade alterada de " + prioridadeAnterior.getDescricao() + " para " + request.prioridade().getDescricao())
            .build();
        ticket.adicionarMensagem(mensagemSistema);

        ticket = ticketRepository.save(ticket);
        return TicketDTO.fromEntity(ticket);
    }

    // ==================== Métricas ====================

    @Transactional(readOnly = true)
    public TicketMetricasDTO getMetricas() {
        LocalDateTime trintaDiasAtras = LocalDateTime.now().minusDays(30);

        long totalTickets = ticketRepository.count();
        long abertos = ticketRepository.countByStatus(StatusTicket.ABERTO);
        long emAndamento = ticketRepository.countByStatus(StatusTicket.EM_ANDAMENTO);
        long aguardandoCliente = ticketRepository.countByStatus(StatusTicket.AGUARDANDO_CLIENTE);
        long aguardandoInterno = ticketRepository.countByStatus(StatusTicket.AGUARDANDO_INTERNO);
        long resolvidos = ticketRepository.countByStatus(StatusTicket.RESOLVIDO);
        long fechados = ticketRepository.countByStatus(StatusTicket.FECHADO);

        long aguardando = aguardandoCliente + aguardandoInterno;

        // Tickets por prioridade (abertos)
        List<Ticket> ticketsAbertos = ticketRepository.findByAtribuidoAIdAndStatusNotIn(
            null,
            List.of(StatusTicket.RESOLVIDO, StatusTicket.FECHADO)
        );

        // SLA
        List<Ticket> comSlaVencido = ticketRepository.findComSlaVencido();
        long dentroSla = ticketRepository.countDentroSla(trintaDiasAtras);
        long comResposta = ticketRepository.countComResposta(trintaDiasAtras);
        BigDecimal percentualDentroSla = comResposta > 0
            ? BigDecimal.valueOf(dentroSla).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(comResposta), 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

        Double tempoMedioResposta = ticketRepository.avgTempoRespostaDesdeDe(trintaDiasAtras);

        long novosUltimos30d = ticketRepository.countAbertosDesdeDe(trintaDiasAtras);
        long resolvidosUltimos30d = ticketRepository.countResolvidosDesdeDe(trintaDiasAtras);

        List<Ticket> naoAtribuidos = ticketRepository.findNaoAtribuidos();

        return new TicketMetricasDTO(
            totalTickets,
            abertos,
            emAndamento,
            aguardando,
            resolvidos,
            fechados,
            0, // TODO: contar por prioridade
            0,
            0,
            0,
            comSlaVencido.size(),
            dentroSla,
            percentualDentroSla,
            tempoMedioResposta,
            null, // TODO: tempo médio resolução
            novosUltimos30d,
            resolvidosUltimos30d,
            naoAtribuidos.size()
        );
    }

    // ==================== Helpers ====================

    private String gerarNumeroTicket() {
        String ano = String.valueOf(Year.now().getValue());
        String prefix = "TKT-" + ano + "-%";
        Integer nextNum = ticketRepository.findNextNumeroByPrefix(prefix);
        if (nextNum == null) nextNum = 1;
        return String.format("TKT-%s-%05d", ano, nextNum);
    }
}
