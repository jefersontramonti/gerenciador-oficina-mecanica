package com.pitstop.ordemservico.service;

import com.pitstop.cliente.domain.Cliente;
import com.pitstop.cliente.domain.TipoCliente;
import com.pitstop.cliente.repository.ClienteRepository;
import com.pitstop.financeiro.service.PagamentoService;
import com.pitstop.notificacao.service.EmailService;
import com.pitstop.notificacao.service.NotificacaoEventPublisher;
import com.pitstop.oficina.domain.Oficina;
import com.pitstop.oficina.repository.OficinaRepository;
import com.pitstop.ordemservico.domain.OrdemServico;
import com.pitstop.ordemservico.domain.StatusOS;
import com.pitstop.ordemservico.domain.TipoCobrancaMaoObra;
import com.pitstop.ordemservico.dto.*;
import com.pitstop.ordemservico.exception.OrdemServicoNaoEditavelException;
import com.pitstop.ordemservico.exception.OrdemServicoNaoPagaException;
import com.pitstop.ordemservico.exception.OrdemServicoNotFoundException;
import com.pitstop.ordemservico.exception.TransicaoStatusInvalidaException;
import com.pitstop.ordemservico.mapper.ItemOSMapper;
import com.pitstop.ordemservico.mapper.OrdemServicoMapper;
import com.pitstop.ordemservico.repository.HistoricoStatusOSRepository;
import com.pitstop.ordemservico.repository.OrdemServicoRepository;
import com.pitstop.shared.security.tenant.TenantContext;
import com.pitstop.usuario.domain.PerfilUsuario;
import com.pitstop.usuario.domain.Usuario;
import com.pitstop.usuario.exception.UsuarioNotFoundException;
import com.pitstop.usuario.repository.UsuarioRepository;
import com.pitstop.veiculo.domain.Veiculo;
import com.pitstop.veiculo.exception.VeiculoNotFoundException;
import com.pitstop.veiculo.repository.VeiculoRepository;
import com.pitstop.estoque.repository.PecaRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Testes unitarios para OrdemServicoService.
 * Usa Mockito para mockar dependencias.
 *
 * @author PitStop Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OrdemServicoService - Testes Unitarios")
class OrdemServicoServiceTest {

    @Mock
    private OrdemServicoRepository repository;

    @Mock
    private HistoricoStatusOSRepository historicoStatusRepository;

    @Mock
    private VeiculoRepository veiculoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private OficinaRepository oficinaRepository;

    @Mock
    private PecaRepository pecaRepository;

    @Mock
    private OrdemServicoMapper mapper;

    @Mock
    private ItemOSMapper itemMapper;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Mock
    private PagamentoService pagamentoService;

    @Mock
    private NotificacaoEventPublisher notificacaoEventPublisher;

    @Mock
    private OrdemServicoPDFService pdfService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private OrdemServicoService ordemServicoService;

    private static MockedStatic<TenantContext> tenantContextMock;
    private static final UUID OFICINA_ID = UUID.randomUUID();
    private static final UUID VEICULO_ID = UUID.randomUUID();
    private static final UUID USUARIO_ID = UUID.randomUUID();
    private static final UUID CLIENTE_ID = UUID.randomUUID();

    @BeforeAll
    static void setUpAll() {
        tenantContextMock = mockStatic(TenantContext.class);
        tenantContextMock.when(TenantContext::getTenantId).thenReturn(OFICINA_ID);
        tenantContextMock.when(TenantContext::isSet).thenReturn(true);
    }

    @AfterAll
    static void tearDownAll() {
        tenantContextMock.close();
    }

    // ===================== CRIAR =====================

    @Nested
    @DisplayName("criar() - Criacao de OS")
    class CriarOSTests {

        @Test
        @DisplayName("Deve criar OS com sucesso")
        void deveCriarOSComSucesso() {
            // Arrange
            CreateOrdemServicoDTO request = createValidOSRequest();
            Veiculo veiculo = createVeiculoEntity();
            Usuario mecanico = createMecanicoEntity();
            Cliente cliente = createClienteEntity();
            Oficina oficina = createOficinaEntity();
            OrdemServico os = createOSEntity();
            OrdemServicoResponseDTO expectedResponse = createOSResponse();

            when(veiculoRepository.findByOficinaIdAndId(OFICINA_ID, VEICULO_ID))
                .thenReturn(Optional.of(veiculo));
            when(usuarioRepository.findByOficinaIdAndId(OFICINA_ID, USUARIO_ID))
                .thenReturn(Optional.of(mecanico));
            when(mapper.toEntity(request)).thenReturn(os);
            when(repository.getNextNumero()).thenReturn(1L);
            when(oficinaRepository.findById(OFICINA_ID)).thenReturn(Optional.of(oficina));
            when(repository.save(any(OrdemServico.class))).thenReturn(os);
            when(clienteRepository.findByOficinaIdAndId(eq(OFICINA_ID), any())).thenReturn(Optional.of(cliente));
            when(clienteRepository.findByOficinaIdAndIdIncludingInactive(eq(OFICINA_ID), any()))
                .thenReturn(Optional.of(cliente));
            when(mapper.toResponse(os)).thenReturn(expectedResponse);

            // Act
            OrdemServicoResponseDTO response = ordemServicoService.criar(request);

            // Assert
            assertThat(response).isNotNull();
            verify(veiculoRepository).findByOficinaIdAndId(OFICINA_ID, VEICULO_ID);
            verify(usuarioRepository).findByOficinaIdAndId(OFICINA_ID, USUARIO_ID);
            verify(repository, atLeast(1)).save(any(OrdemServico.class));
        }

        @Test
        @DisplayName("Deve lancar excecao ao criar OS com veiculo inexistente")
        void deveLancarExcecaoAoCriarOSComVeiculoInexistente() {
            // Arrange
            CreateOrdemServicoDTO request = createValidOSRequest();
            when(veiculoRepository.findByOficinaIdAndId(OFICINA_ID, VEICULO_ID))
                .thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> ordemServicoService.criar(request))
                .isInstanceOf(VeiculoNotFoundException.class);

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lancar excecao ao criar OS com mecanico inexistente")
        void deveLancarExcecaoAoCriarOSComMecanicoInexistente() {
            // Arrange
            CreateOrdemServicoDTO request = createValidOSRequest();
            Veiculo veiculo = createVeiculoEntity();

            when(veiculoRepository.findByOficinaIdAndId(OFICINA_ID, VEICULO_ID))
                .thenReturn(Optional.of(veiculo));
            when(usuarioRepository.findByOficinaIdAndId(OFICINA_ID, USUARIO_ID))
                .thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> ordemServicoService.criar(request))
                .isInstanceOf(UsuarioNotFoundException.class);

            verify(repository, never()).save(any());
        }
    }

    // ===================== BUSCAR POR ID =====================

    @Nested
    @DisplayName("buscarPorId() - Busca por ID")
    class BuscarPorIdTests {

        @Test
        @DisplayName("Deve buscar OS por ID com sucesso")
        void deveBuscarOSPorIdComSucesso() {
            // Arrange
            UUID osId = UUID.randomUUID();
            OrdemServico os = createOSEntity();
            os.setId(osId);
            Veiculo veiculo = createVeiculoEntity();
            Usuario mecanico = createMecanicoEntity();
            Cliente cliente = createClienteEntity();
            OrdemServicoResponseDTO expectedResponse = createOSResponse();

            when(repository.findByOficinaIdAndId(OFICINA_ID, osId)).thenReturn(Optional.of(os));
            when(veiculoRepository.findByOficinaIdAndId(OFICINA_ID, VEICULO_ID))
                .thenReturn(Optional.of(veiculo));
            when(usuarioRepository.findByOficinaIdAndId(OFICINA_ID, USUARIO_ID))
                .thenReturn(Optional.of(mecanico));
            when(clienteRepository.findByOficinaIdAndIdIncludingInactive(eq(OFICINA_ID), any()))
                .thenReturn(Optional.of(cliente));
            when(mapper.toResponse(os)).thenReturn(expectedResponse);

            // Act
            OrdemServicoResponseDTO response = ordemServicoService.buscarPorId(osId);

            // Assert
            assertThat(response).isNotNull();
            verify(repository).findByOficinaIdAndId(OFICINA_ID, osId);
        }

        @Test
        @DisplayName("Deve lancar excecao ao buscar OS inexistente")
        void deveLancarExcecaoAoBuscarOSInexistente() {
            // Arrange
            UUID osId = UUID.randomUUID();
            when(repository.findByOficinaIdAndId(OFICINA_ID, osId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> ordemServicoService.buscarPorId(osId))
                .isInstanceOf(OrdemServicoNotFoundException.class);
        }
    }

    // ===================== BUSCAR POR NUMERO =====================

    @Nested
    @DisplayName("buscarPorNumero() - Busca por Numero")
    class BuscarPorNumeroTests {

        @Test
        @DisplayName("Deve buscar OS por numero com sucesso")
        void deveBuscarOSPorNumeroComSucesso() {
            // Arrange
            Long numero = 123L;
            OrdemServico os = createOSEntity();
            os.setNumero(numero);
            Veiculo veiculo = createVeiculoEntity();
            Usuario mecanico = createMecanicoEntity();
            Cliente cliente = createClienteEntity();
            OrdemServicoResponseDTO expectedResponse = createOSResponse();

            when(repository.findByOficinaIdAndNumero(OFICINA_ID, numero)).thenReturn(Optional.of(os));
            when(veiculoRepository.findByOficinaIdAndId(OFICINA_ID, VEICULO_ID))
                .thenReturn(Optional.of(veiculo));
            when(usuarioRepository.findByOficinaIdAndId(OFICINA_ID, USUARIO_ID))
                .thenReturn(Optional.of(mecanico));
            when(clienteRepository.findByOficinaIdAndIdIncludingInactive(eq(OFICINA_ID), any()))
                .thenReturn(Optional.of(cliente));
            when(mapper.toResponse(os)).thenReturn(expectedResponse);

            // Act
            OrdemServicoResponseDTO response = ordemServicoService.buscarPorNumero(numero);

            // Assert
            assertThat(response).isNotNull();
            verify(repository).findByOficinaIdAndNumero(OFICINA_ID, numero);
        }

        @Test
        @DisplayName("Deve lancar excecao ao buscar OS por numero inexistente")
        void deveLancarExcecaoAoBuscarPorNumeroInexistente() {
            // Arrange
            Long numero = 999L;
            when(repository.findByOficinaIdAndNumero(OFICINA_ID, numero)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> ordemServicoService.buscarPorNumero(numero))
                .isInstanceOf(OrdemServicoNotFoundException.class);
        }
    }

    // ===================== APROVAR =====================

    @Nested
    @DisplayName("aprovar() - Aprovacao de Orcamento")
    class AprovarOSTests {

        @Test
        @DisplayName("Deve aprovar orcamento com sucesso")
        void deveAprovarOrcamentoComSucesso() {
            // Arrange
            UUID osId = UUID.randomUUID();
            OrdemServico os = createOSEntity();
            os.setId(osId);
            os.setStatus(StatusOS.ORCAMENTO);
            Veiculo veiculo = createVeiculoEntity();
            Cliente cliente = createClienteEntity();
            Oficina oficina = createOficinaEntity();

            when(repository.findByOficinaIdAndId(OFICINA_ID, osId)).thenReturn(Optional.of(os));
            when(repository.save(any(OrdemServico.class))).thenReturn(os);
            when(veiculoRepository.findByOficinaIdAndId(eq(OFICINA_ID), any()))
                .thenReturn(Optional.of(veiculo));
            when(clienteRepository.findByOficinaIdAndId(eq(OFICINA_ID), any()))
                .thenReturn(Optional.of(cliente));
            when(oficinaRepository.findById(OFICINA_ID)).thenReturn(Optional.of(oficina));

            // Act
            ordemServicoService.aprovar(osId, true);

            // Assert
            assertThat(os.getStatus()).isEqualTo(StatusOS.APROVADO);
            verify(repository).save(os);
        }

        @Test
        @DisplayName("Deve lancar excecao ao aprovar OS que nao esta em ORCAMENTO")
        void deveLancarExcecaoAoAprovarOSQueNaoEstaEmOrcamento() {
            // Arrange
            UUID osId = UUID.randomUUID();
            OrdemServico os = createOSEntity();
            os.setId(osId);
            os.setStatus(StatusOS.EM_ANDAMENTO);

            when(repository.findByOficinaIdAndId(OFICINA_ID, osId)).thenReturn(Optional.of(os));

            // Act & Assert
            assertThatThrownBy(() -> ordemServicoService.aprovar(osId, true))
                .isInstanceOf(TransicaoStatusInvalidaException.class);

            verify(repository, never()).save(any());
        }
    }

    // ===================== INICIAR =====================

    @Nested
    @DisplayName("iniciar() - Iniciar Execucao")
    class IniciarOSTests {

        @Test
        @DisplayName("Deve iniciar OS aprovada com sucesso")
        void deveIniciarOSAprovadaComSucesso() {
            // Arrange
            UUID osId = UUID.randomUUID();
            OrdemServico os = createOSEntity();
            os.setId(osId);
            os.setStatus(StatusOS.APROVADO);
            os.setAprovadoPeloCliente(true);  // Necessario para iniciar
            Veiculo veiculo = createVeiculoEntity();
            Cliente cliente = createClienteEntity();
            Usuario mecanico = createMecanicoEntity();
            Oficina oficina = createOficinaEntity();

            when(repository.findByOficinaIdAndId(OFICINA_ID, osId)).thenReturn(Optional.of(os));
            when(repository.save(any(OrdemServico.class))).thenReturn(os);
            when(veiculoRepository.findByOficinaIdAndId(eq(OFICINA_ID), any()))
                .thenReturn(Optional.of(veiculo));
            when(clienteRepository.findByOficinaIdAndId(eq(OFICINA_ID), any()))
                .thenReturn(Optional.of(cliente));
            when(usuarioRepository.findByOficinaIdAndId(eq(OFICINA_ID), any()))
                .thenReturn(Optional.of(mecanico));
            when(oficinaRepository.findById(OFICINA_ID)).thenReturn(Optional.of(oficina));

            // Act
            ordemServicoService.iniciar(osId);

            // Assert
            assertThat(os.getStatus()).isEqualTo(StatusOS.EM_ANDAMENTO);
            verify(repository).save(os);
        }

        @Test
        @DisplayName("Deve lancar excecao ao iniciar OS que nao esta APROVADA")
        void deveLancarExcecaoAoIniciarOSQueNaoEstaAprovada() {
            // Arrange
            UUID osId = UUID.randomUUID();
            OrdemServico os = createOSEntity();
            os.setId(osId);
            os.setStatus(StatusOS.ORCAMENTO);

            when(repository.findByOficinaIdAndId(OFICINA_ID, osId)).thenReturn(Optional.of(os));

            // Act & Assert
            assertThatThrownBy(() -> ordemServicoService.iniciar(osId))
                .isInstanceOf(TransicaoStatusInvalidaException.class);

            verify(repository, never()).save(any());
        }
    }

    // ===================== FINALIZAR =====================

    @Nested
    @DisplayName("finalizar() - Finalizacao de OS")
    class FinalizarOSTests {

        @Test
        @DisplayName("Deve finalizar OS em andamento com sucesso")
        void deveFinalizarOSEmAndamentoComSucesso() {
            // Arrange
            UUID osId = UUID.randomUUID();
            OrdemServico os = createOSEntity();
            os.setId(osId);
            os.setStatus(StatusOS.EM_ANDAMENTO);
            Veiculo veiculo = createVeiculoEntity();
            Cliente cliente = createClienteEntity();
            Oficina oficina = createOficinaEntity();

            when(repository.findByOficinaIdAndId(OFICINA_ID, osId)).thenReturn(Optional.of(os));
            when(repository.save(any(OrdemServico.class))).thenReturn(os);
            when(veiculoRepository.findByOficinaIdAndId(eq(OFICINA_ID), any()))
                .thenReturn(Optional.of(veiculo));
            when(clienteRepository.findByOficinaIdAndId(eq(OFICINA_ID), any()))
                .thenReturn(Optional.of(cliente));
            when(oficinaRepository.findById(OFICINA_ID)).thenReturn(Optional.of(oficina));

            // Act
            ordemServicoService.finalizar(osId);

            // Assert
            assertThat(os.getStatus()).isEqualTo(StatusOS.FINALIZADO);
            verify(repository).save(os);
            verify(applicationEventPublisher).publishEvent(any());
        }

        @Test
        @DisplayName("Deve lancar excecao ao finalizar OS que nao esta EM_ANDAMENTO")
        void deveLancarExcecaoAoFinalizarOSQueNaoEstaEmAndamento() {
            // Arrange
            UUID osId = UUID.randomUUID();
            OrdemServico os = createOSEntity();
            os.setId(osId);
            os.setStatus(StatusOS.ORCAMENTO);

            when(repository.findByOficinaIdAndId(OFICINA_ID, osId)).thenReturn(Optional.of(os));

            // Act & Assert
            assertThatThrownBy(() -> ordemServicoService.finalizar(osId))
                .isInstanceOf(TransicaoStatusInvalidaException.class);

            verify(repository, never()).save(any());
        }
    }

    // ===================== ENTREGAR =====================

    @Nested
    @DisplayName("entregar() - Entrega do Veiculo")
    class EntregarOSTests {

        @Test
        @DisplayName("Deve entregar veiculo quando OS esta quitada")
        void deveEntregarVeiculoQuandoOSEstaQuitada() {
            // Arrange
            UUID osId = UUID.randomUUID();
            OrdemServico os = createOSEntity();
            os.setId(osId);
            os.setStatus(StatusOS.FINALIZADO);
            Veiculo veiculo = createVeiculoEntity();
            Cliente cliente = createClienteEntity();
            Oficina oficina = createOficinaEntity();

            when(repository.findByOficinaIdAndId(OFICINA_ID, osId)).thenReturn(Optional.of(os));
            when(pagamentoService.isOrdemServicoQuitada(osId)).thenReturn(true);
            when(repository.save(any(OrdemServico.class))).thenReturn(os);
            when(veiculoRepository.findByOficinaIdAndId(eq(OFICINA_ID), any()))
                .thenReturn(Optional.of(veiculo));
            when(clienteRepository.findByOficinaIdAndId(eq(OFICINA_ID), any()))
                .thenReturn(Optional.of(cliente));
            when(oficinaRepository.findById(OFICINA_ID)).thenReturn(Optional.of(oficina));

            // Act
            ordemServicoService.entregar(osId);

            // Assert
            assertThat(os.getStatus()).isEqualTo(StatusOS.ENTREGUE);
            verify(repository).save(os);
        }

        @Test
        @DisplayName("Deve lancar excecao ao entregar OS nao paga")
        void deveLancarExcecaoAoEntregarOSNaoPaga() {
            // Arrange
            UUID osId = UUID.randomUUID();
            OrdemServico os = createOSEntity();
            os.setId(osId);
            os.setNumero(123L);
            os.setStatus(StatusOS.FINALIZADO);

            when(repository.findByOficinaIdAndId(OFICINA_ID, osId)).thenReturn(Optional.of(os));
            when(pagamentoService.isOrdemServicoQuitada(osId)).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> ordemServicoService.entregar(osId))
                .isInstanceOf(OrdemServicoNaoPagaException.class);

            verify(repository, never()).save(any());
        }
    }

    // ===================== CANCELAR =====================

    @Nested
    @DisplayName("cancelar() - Cancelamento de OS")
    class CancelarOSTests {

        @Test
        @DisplayName("Deve cancelar OS com sucesso")
        void deveCancelarOSComSucesso() {
            // Arrange
            UUID osId = UUID.randomUUID();
            OrdemServico os = createOSEntity();
            os.setId(osId);
            os.setStatus(StatusOS.ORCAMENTO);
            CancelarOrdemServicoDTO dto = new CancelarOrdemServicoDTO("Cliente desistiu");

            when(repository.findByOficinaIdAndId(OFICINA_ID, osId)).thenReturn(Optional.of(os));
            when(repository.save(any(OrdemServico.class))).thenReturn(os);

            // Act
            ordemServicoService.cancelar(osId, dto);

            // Assert
            assertThat(os.getStatus()).isEqualTo(StatusOS.CANCELADO);
            verify(repository).save(os);
        }

        @Test
        @DisplayName("Deve lancar excecao ao cancelar OS ja entregue")
        void deveLancarExcecaoAoCancelarOSJaEntregue() {
            // Arrange
            UUID osId = UUID.randomUUID();
            OrdemServico os = createOSEntity();
            os.setId(osId);
            os.setStatus(StatusOS.ENTREGUE);
            CancelarOrdemServicoDTO dto = new CancelarOrdemServicoDTO("Motivo");

            when(repository.findByOficinaIdAndId(OFICINA_ID, osId)).thenReturn(Optional.of(os));

            // Act & Assert
            assertThatThrownBy(() -> ordemServicoService.cancelar(osId, dto))
                .isInstanceOf(TransicaoStatusInvalidaException.class);

            verify(repository, never()).save(any());
        }
    }

    // ===================== ATUALIZAR =====================

    @Nested
    @DisplayName("atualizar() - Atualizacao de OS")
    class AtualizarOSTests {

        @Test
        @DisplayName("Deve atualizar OS em status editavel")
        void deveAtualizarOSEmStatusEditavel() {
            // Arrange
            UUID osId = UUID.randomUUID();
            OrdemServico os = createOSEntity();
            os.setId(osId);
            os.setStatus(StatusOS.ORCAMENTO);
            UpdateOrdemServicoDTO dto = new UpdateOrdemServicoDTO(
                "Novos problemas relatados",  // problemasRelatados (min 10 chars)
                "Novo diagnostico tecnico",   // diagnostico
                "Observacoes adicionais",     // observacoes
                LocalDate.now().plusDays(3),  // dataPrevisao
                null,                         // valorMaoObra
                null,                         // descontoPercentual
                null,                         // descontoValor
                null                          // itens
            );
            Veiculo veiculo = createVeiculoEntity();
            Usuario mecanico = createMecanicoEntity();
            Cliente cliente = createClienteEntity();
            OrdemServicoResponseDTO expectedResponse = createOSResponse();

            when(repository.findByOficinaIdAndId(OFICINA_ID, osId)).thenReturn(Optional.of(os));
            when(repository.save(any(OrdemServico.class))).thenReturn(os);
            when(veiculoRepository.findByOficinaIdAndId(OFICINA_ID, VEICULO_ID))
                .thenReturn(Optional.of(veiculo));
            when(usuarioRepository.findByOficinaIdAndId(OFICINA_ID, USUARIO_ID))
                .thenReturn(Optional.of(mecanico));
            when(clienteRepository.findByOficinaIdAndIdIncludingInactive(eq(OFICINA_ID), any()))
                .thenReturn(Optional.of(cliente));
            when(mapper.toResponse(os)).thenReturn(expectedResponse);

            // Act
            OrdemServicoResponseDTO response = ordemServicoService.atualizar(osId, dto);

            // Assert
            assertThat(response).isNotNull();
            verify(mapper).updateEntityFromDto(dto, os);
            verify(repository).save(os);
        }

        @Test
        @DisplayName("Deve lancar excecao ao atualizar OS nao editavel")
        void deveLancarExcecaoAoAtualizarOSNaoEditavel() {
            // Arrange
            UUID osId = UUID.randomUUID();
            OrdemServico os = createOSEntity();
            os.setId(osId);
            os.setStatus(StatusOS.ENTREGUE);
            UpdateOrdemServicoDTO dto = new UpdateOrdemServicoDTO(
                "Novos problemas relatados",  // problemasRelatados (min 10 chars)
                null, null, null, null, null, null, null
            );

            when(repository.findByOficinaIdAndId(OFICINA_ID, osId)).thenReturn(Optional.of(os));

            // Act & Assert
            assertThatThrownBy(() -> ordemServicoService.atualizar(osId, dto))
                .isInstanceOf(OrdemServicoNaoEditavelException.class);

            verify(repository, never()).save(any());
        }
    }

    // ===================== METODOS AUXILIARES =====================

    private CreateOrdemServicoDTO createValidOSRequest() {
        return new CreateOrdemServicoDTO(
            VEICULO_ID,                          // veiculoId
            USUARIO_ID,                          // usuarioId
            "Barulho no motor ao acelerar",      // problemasRelatados (min 10 chars)
            LocalDate.now().plusDays(3),         // dataPrevisao
            TipoCobrancaMaoObra.VALOR_FIXO,      // tipoCobrancaMaoObra
            new BigDecimal("300.00"),            // valorMaoObra
            null,                                // tempoEstimadoHoras
            null,                                // limiteHorasAprovado
            null,                                // descontoPercentual
            null,                                // descontoValor
            null,                                // diagnostico
            null,                                // observacoes
            null                                 // itens
        );
    }

    private OrdemServico createOSEntity() {
        OrdemServico os = new OrdemServico();
        os.setId(UUID.randomUUID());
        os.setNumero(1L);
        os.setStatus(StatusOS.ORCAMENTO);
        os.setVeiculoId(VEICULO_ID);
        os.setUsuarioId(USUARIO_ID);
        os.setProblemasRelatados("Barulho no motor");
        os.setDataAbertura(LocalDateTime.now());
        os.setTipoCobrancaMaoObra(TipoCobrancaMaoObra.VALOR_FIXO);
        os.setValorMaoObra(new BigDecimal("300.00"));
        os.setValorPecas(BigDecimal.ZERO);
        os.setValorTotal(new BigDecimal("300.00"));
        os.setDescontoPercentual(BigDecimal.ZERO);
        os.setValorFinal(new BigDecimal("300.00"));
        return os;
    }

    private Veiculo createVeiculoEntity() {
        Veiculo veiculo = new Veiculo();
        veiculo.setId(VEICULO_ID);
        veiculo.setClienteId(CLIENTE_ID);
        veiculo.setPlaca("ABC1234");
        veiculo.setMarca("Volkswagen");
        veiculo.setModelo("Gol");
        veiculo.setAno(2020);
        return veiculo;
    }

    private Usuario createMecanicoEntity() {
        Usuario usuario = new Usuario();
        usuario.setId(USUARIO_ID);
        usuario.setNome("Joao Mecanico");
        usuario.setEmail("joao@mecanico.com");
        usuario.setPerfil(PerfilUsuario.MECANICO);
        return usuario;
    }

    private Cliente createClienteEntity() {
        return Cliente.builder()
            .id(CLIENTE_ID)
            .tipo(TipoCliente.PESSOA_FISICA)
            .nome("Cliente Teste")
            .cpfCnpj("529.982.247-25")
            .email("cliente@email.com")
            .celular("(11) 99999-0000")
            .ativo(true)
            .build();
    }

    private Oficina createOficinaEntity() {
        Oficina oficina = new Oficina();
        oficina.setId(OFICINA_ID);
        oficina.setNomeFantasia("Oficina Teste");
        oficina.setValorHora(new BigDecimal("80.00"));
        return oficina;
    }

    private OrdemServicoResponseDTO createOSResponse() {
        return new OrdemServicoResponseDTO(
            UUID.randomUUID(),
            1L,
            StatusOS.ORCAMENTO,
            new VeiculoResumoDTO(VEICULO_ID, "ABC-1234", "Volkswagen", "Gol", 2020, "Prata"),
            new ClienteResumoDTO(CLIENTE_ID, "Cliente Teste", "529.982.247-25", "(11) 99999-0000", "cliente@email.com"),
            new UsuarioResumoDTO(USUARIO_ID, "Joao Mecanico", "joao@mecanico.com", "MECANICO"),
            LocalDateTime.now(),
            LocalDate.now().plusDays(3),
            null,
            null,
            "Barulho no motor",
            null,
            null,
            TipoCobrancaMaoObra.VALOR_FIXO,
            new BigDecimal("300.00"),
            null,
            null,
            null,
            null,
            BigDecimal.ZERO,
            new BigDecimal("300.00"),
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            new BigDecimal("300.00"),
            false,
            List.of(),
            LocalDateTime.now(),
            LocalDateTime.now()
        );
    }
}
