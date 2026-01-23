package com.pitstop.estoque.service;

import com.pitstop.estoque.domain.MovimentacaoEstoque;
import com.pitstop.estoque.domain.Peca;
import com.pitstop.estoque.domain.TipoMovimentacao;
import com.pitstop.estoque.domain.UnidadeMedida;
import com.pitstop.estoque.exception.EstoqueInsuficienteException;
import com.pitstop.estoque.exception.MovimentacaoInvalidaException;
import com.pitstop.estoque.exception.PecaNotFoundException;
import com.pitstop.estoque.repository.MovimentacaoEstoqueRepository;
import com.pitstop.estoque.repository.PecaRepository;
import com.pitstop.ordemservico.domain.ItemOS;
import com.pitstop.ordemservico.domain.OrigemPeca;
import com.pitstop.ordemservico.domain.TipoItem;
import com.pitstop.shared.security.tenant.TenantContext;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Testes unitarios para MovimentacaoEstoqueService.
 * Testa operacoes de entrada, saida, ajuste e baixa de estoque.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MovimentacaoEstoqueService - Testes Unitarios")
class MovimentacaoEstoqueServiceTest {

    @Mock
    private MovimentacaoEstoqueRepository movimentacaoRepository;

    @Mock
    private PecaRepository pecaRepository;

    @InjectMocks
    private MovimentacaoEstoqueService movimentacaoService;

    private static MockedStatic<TenantContext> tenantContextMock;
    private static final UUID OFICINA_ID = UUID.randomUUID();
    private static final UUID USUARIO_ID = UUID.randomUUID();

    @BeforeAll
    static void setUpAll() {
        tenantContextMock = mockStatic(TenantContext.class);
        tenantContextMock.when(TenantContext::getTenantId).thenReturn(OFICINA_ID);
    }

    @AfterAll
    static void tearDownAll() {
        tenantContextMock.close();
    }

    // ==================== REGISTRAR ENTRADA ====================

    @Nested
    @DisplayName("registrarEntrada()")
    class RegistrarEntrada {

        @Test
        @DisplayName("Deve registrar entrada de estoque com sucesso")
        void deveRegistrarEntradaComSucesso() {
            // Arrange
            UUID pecaId = UUID.randomUUID();
            Peca peca = criarPecaValida(pecaId);
            peca.setQuantidadeAtual(10);

            when(pecaRepository.findByOficinaIdAndIdForUpdate(OFICINA_ID, pecaId))
                    .thenReturn(Optional.of(peca));
            when(pecaRepository.save(any(Peca.class))).thenAnswer(inv -> inv.getArgument(0));
            when(movimentacaoRepository.save(any(MovimentacaoEstoque.class))).thenAnswer(inv -> {
                MovimentacaoEstoque m = inv.getArgument(0);
                m = MovimentacaoEstoque.builder()
                        .id(UUID.randomUUID())
                        .pecaId(m.getPecaId())
                        .tipo(m.getTipo())
                        .quantidade(m.getQuantidade())
                        .quantidadeAnterior(m.getQuantidadeAnterior())
                        .quantidadeAtual(m.getQuantidadeAtual())
                        .valorUnitario(m.getValorUnitario())
                        .build();
                return m;
            });

            // Act
            MovimentacaoEstoque resultado = movimentacaoService.registrarEntrada(
                    pecaId, 5, new BigDecimal("25.00"), USUARIO_ID, "Compra fornecedor", null
            );

            // Assert
            assertThat(resultado).isNotNull();
            assertThat(resultado.getTipo()).isEqualTo(TipoMovimentacao.ENTRADA);
            assertThat(resultado.getQuantidade()).isEqualTo(5);
            assertThat(resultado.getQuantidadeAnterior()).isEqualTo(10);
            assertThat(resultado.getQuantidadeAtual()).isEqualTo(15);
            verify(pecaRepository).save(argThat(p -> p.getQuantidadeAtual() == 15));
        }

        @Test
        @DisplayName("Deve lancar excecao quando peca nao encontrada")
        void deveLancarExcecaoQuandoPecaNaoEncontrada() {
            // Arrange
            UUID pecaId = UUID.randomUUID();
            when(pecaRepository.findByOficinaIdAndIdForUpdate(OFICINA_ID, pecaId))
                    .thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> movimentacaoService.registrarEntrada(
                    pecaId, 5, new BigDecimal("25.00"), USUARIO_ID, "Compra", null
            )).isInstanceOf(PecaNotFoundException.class);
        }
    }

    // ==================== REGISTRAR SAIDA ====================

    @Nested
    @DisplayName("registrarSaida()")
    class RegistrarSaida {

        @Test
        @DisplayName("Deve registrar saida de estoque com sucesso")
        void deveRegistrarSaidaComSucesso() {
            // Arrange
            UUID pecaId = UUID.randomUUID();
            Peca peca = criarPecaValida(pecaId);
            peca.setQuantidadeAtual(10);

            when(pecaRepository.findByOficinaIdAndIdForUpdate(OFICINA_ID, pecaId))
                    .thenReturn(Optional.of(peca));
            when(pecaRepository.save(any(Peca.class))).thenAnswer(inv -> inv.getArgument(0));
            when(movimentacaoRepository.save(any(MovimentacaoEstoque.class))).thenAnswer(inv -> {
                MovimentacaoEstoque m = inv.getArgument(0);
                return MovimentacaoEstoque.builder()
                        .id(UUID.randomUUID())
                        .pecaId(m.getPecaId())
                        .tipo(m.getTipo())
                        .quantidade(m.getQuantidade())
                        .quantidadeAnterior(m.getQuantidadeAnterior())
                        .quantidadeAtual(m.getQuantidadeAtual())
                        .valorUnitario(m.getValorUnitario())
                        .build();
            });

            // Act
            MovimentacaoEstoque resultado = movimentacaoService.registrarSaida(
                    pecaId, 3, new BigDecimal("50.00"), USUARIO_ID, "Venda avulsa", null
            );

            // Assert
            assertThat(resultado).isNotNull();
            assertThat(resultado.getTipo()).isEqualTo(TipoMovimentacao.SAIDA);
            assertThat(resultado.getQuantidade()).isEqualTo(3);
            assertThat(resultado.getQuantidadeAnterior()).isEqualTo(10);
            assertThat(resultado.getQuantidadeAtual()).isEqualTo(7);
            verify(pecaRepository).save(argThat(p -> p.getQuantidadeAtual() == 7));
        }

        @Test
        @DisplayName("Deve lancar excecao quando estoque insuficiente")
        void deveLancarExcecaoQuandoEstoqueInsuficiente() {
            // Arrange
            UUID pecaId = UUID.randomUUID();
            Peca peca = criarPecaValida(pecaId);
            peca.setQuantidadeAtual(5);

            when(pecaRepository.findByOficinaIdAndIdForUpdate(OFICINA_ID, pecaId))
                    .thenReturn(Optional.of(peca));

            // Act & Assert
            assertThatThrownBy(() -> movimentacaoService.registrarSaida(
                    pecaId, 10, new BigDecimal("50.00"), USUARIO_ID, "Venda", null
            )).isInstanceOf(EstoqueInsuficienteException.class);

            verify(pecaRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lancar excecao quando peca nao encontrada")
        void deveLancarExcecaoQuandoPecaNaoEncontrada() {
            // Arrange
            UUID pecaId = UUID.randomUUID();
            when(pecaRepository.findByOficinaIdAndIdForUpdate(OFICINA_ID, pecaId))
                    .thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> movimentacaoService.registrarSaida(
                    pecaId, 3, new BigDecimal("50.00"), USUARIO_ID, "Venda", null
            )).isInstanceOf(PecaNotFoundException.class);
        }
    }

    // ==================== REGISTRAR AJUSTE ====================

    @Nested
    @DisplayName("registrarAjuste()")
    class RegistrarAjuste {

        @Test
        @DisplayName("Deve registrar ajuste positivo com sucesso")
        void deveRegistrarAjustePositivoComSucesso() {
            // Arrange
            UUID pecaId = UUID.randomUUID();
            Peca peca = criarPecaValida(pecaId);
            peca.setQuantidadeAtual(10);

            when(pecaRepository.findByOficinaIdAndIdForUpdate(OFICINA_ID, pecaId))
                    .thenReturn(Optional.of(peca));
            when(pecaRepository.save(any(Peca.class))).thenAnswer(inv -> inv.getArgument(0));
            when(movimentacaoRepository.save(any(MovimentacaoEstoque.class))).thenAnswer(inv -> {
                MovimentacaoEstoque m = inv.getArgument(0);
                return MovimentacaoEstoque.builder()
                        .id(UUID.randomUUID())
                        .pecaId(m.getPecaId())
                        .tipo(m.getTipo())
                        .quantidade(m.getQuantidade())
                        .quantidadeAnterior(m.getQuantidadeAnterior())
                        .quantidadeAtual(m.getQuantidadeAtual())
                        .valorUnitario(m.getValorUnitario())
                        .build();
            });

            // Act
            MovimentacaoEstoque resultado = movimentacaoService.registrarAjuste(
                    pecaId, 15, new BigDecimal("25.00"), USUARIO_ID, "Inventario fisico", null
            );

            // Assert
            assertThat(resultado).isNotNull();
            assertThat(resultado.getTipo()).isEqualTo(TipoMovimentacao.AJUSTE);
            assertThat(resultado.getQuantidade()).isEqualTo(5); // Diferenca
            assertThat(resultado.getQuantidadeAnterior()).isEqualTo(10);
            assertThat(resultado.getQuantidadeAtual()).isEqualTo(15);
        }

        @Test
        @DisplayName("Deve registrar ajuste negativo com sucesso")
        void deveRegistrarAjusteNegativoComSucesso() {
            // Arrange
            UUID pecaId = UUID.randomUUID();
            Peca peca = criarPecaValida(pecaId);
            peca.setQuantidadeAtual(10);

            when(pecaRepository.findByOficinaIdAndIdForUpdate(OFICINA_ID, pecaId))
                    .thenReturn(Optional.of(peca));
            when(pecaRepository.save(any(Peca.class))).thenAnswer(inv -> inv.getArgument(0));
            when(movimentacaoRepository.save(any(MovimentacaoEstoque.class))).thenAnswer(inv -> {
                MovimentacaoEstoque m = inv.getArgument(0);
                return MovimentacaoEstoque.builder()
                        .id(UUID.randomUUID())
                        .pecaId(m.getPecaId())
                        .tipo(m.getTipo())
                        .quantidade(m.getQuantidade())
                        .quantidadeAnterior(m.getQuantidadeAnterior())
                        .quantidadeAtual(m.getQuantidadeAtual())
                        .valorUnitario(m.getValorUnitario())
                        .build();
            });

            // Act
            MovimentacaoEstoque resultado = movimentacaoService.registrarAjuste(
                    pecaId, 5, new BigDecimal("25.00"), USUARIO_ID, "Perda identificada", null
            );

            // Assert
            assertThat(resultado).isNotNull();
            assertThat(resultado.getQuantidade()).isEqualTo(5); // Diferenca absoluta
            assertThat(resultado.getQuantidadeAnterior()).isEqualTo(10);
            assertThat(resultado.getQuantidadeAtual()).isEqualTo(5);
        }

        @Test
        @DisplayName("Deve lancar excecao quando quantidade nova igual a atual")
        void deveLancarExcecaoQuandoQuantidadeIgual() {
            // Arrange
            UUID pecaId = UUID.randomUUID();
            Peca peca = criarPecaValida(pecaId);
            peca.setQuantidadeAtual(10);

            when(pecaRepository.findByOficinaIdAndIdForUpdate(OFICINA_ID, pecaId))
                    .thenReturn(Optional.of(peca));

            // Act & Assert
            assertThatThrownBy(() -> movimentacaoService.registrarAjuste(
                    pecaId, 10, new BigDecimal("25.00"), USUARIO_ID, "Ajuste", null
            )).isInstanceOf(MovimentacaoInvalidaException.class);
        }
    }

    // ==================== BAIXAR ESTOQUE POR OS ====================

    @Nested
    @DisplayName("baixarEstoquePorOS()")
    class BaixarEstoquePorOS {

        @Test
        @DisplayName("Deve baixar estoque por OS com sucesso")
        void deveBaixarEstoquePorOSComSucesso() {
            // Arrange
            UUID osId = UUID.randomUUID();
            UUID pecaId = UUID.randomUUID();
            Peca peca = criarPecaValida(pecaId);
            peca.setQuantidadeAtual(10);

            ItemOS item = criarItemOS(pecaId, 2);

            when(pecaRepository.findByOficinaIdAndId(OFICINA_ID, pecaId))
                    .thenReturn(Optional.of(peca));
            when(pecaRepository.findByOficinaIdAndIdForUpdate(OFICINA_ID, pecaId))
                    .thenReturn(Optional.of(peca));
            when(pecaRepository.save(any(Peca.class))).thenAnswer(inv -> inv.getArgument(0));
            when(movimentacaoRepository.save(any(MovimentacaoEstoque.class))).thenAnswer(inv -> {
                MovimentacaoEstoque m = inv.getArgument(0);
                return MovimentacaoEstoque.builder()
                        .id(UUID.randomUUID())
                        .pecaId(m.getPecaId())
                        .ordemServicoId(m.getOrdemServicoId())
                        .tipo(m.getTipo())
                        .quantidade(m.getQuantidade())
                        .quantidadeAnterior(m.getQuantidadeAnterior())
                        .quantidadeAtual(m.getQuantidadeAtual())
                        .valorUnitario(m.getValorUnitario())
                        .build();
            });

            // Act
            List<MovimentacaoEstoque> resultado = movimentacaoService.baixarEstoquePorOS(
                    osId, List.of(item), USUARIO_ID
            );

            // Assert
            assertThat(resultado).hasSize(1);
            assertThat(resultado.get(0).getTipo()).isEqualTo(TipoMovimentacao.BAIXA_OS);
            assertThat(resultado.get(0).getOrdemServicoId()).isEqualTo(osId);
            verify(pecaRepository).save(argThat(p -> p.getQuantidadeAtual() == 8));
        }

        @Test
        @DisplayName("Deve retornar lista vazia quando OS nao tem pecas do estoque")
        void deveRetornarListaVaziaQuandoOSNaoTemPecasEstoque() {
            // Arrange
            UUID osId = UUID.randomUUID();
            ItemOS itemServico = ItemOS.builder()
                    .tipo(TipoItem.SERVICO)
                    .descricao("Mao de obra")
                    .quantidade(1)
                    .valorUnitario(new BigDecimal("100.00"))
                    .build();

            // Act
            List<MovimentacaoEstoque> resultado = movimentacaoService.baixarEstoquePorOS(
                    osId, List.of(itemServico), USUARIO_ID
            );

            // Assert
            assertThat(resultado).isEmpty();
            verify(pecaRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve ignorar pecas avulsas e do cliente")
        void deveIgnorarPecasAvulsasEDoCliente() {
            // Arrange
            UUID osId = UUID.randomUUID();
            UUID pecaId = UUID.randomUUID();

            ItemOS itemAvulsa = ItemOS.builder()
                    .tipo(TipoItem.PECA)
                    .origemPeca(OrigemPeca.AVULSA)
                    .descricao("Peca avulsa")
                    .quantidade(1)
                    .valorUnitario(new BigDecimal("50.00"))
                    .build();

            ItemOS itemCliente = ItemOS.builder()
                    .tipo(TipoItem.PECA)
                    .origemPeca(OrigemPeca.CLIENTE)
                    .descricao("Peca do cliente")
                    .quantidade(1)
                    .valorUnitario(new BigDecimal("0.00"))
                    .build();

            // Act
            List<MovimentacaoEstoque> resultado = movimentacaoService.baixarEstoquePorOS(
                    osId, List.of(itemAvulsa, itemCliente), USUARIO_ID
            );

            // Assert
            assertThat(resultado).isEmpty();
            verify(pecaRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lancar excecao quando estoque insuficiente para OS")
        void deveLancarExcecaoQuandoEstoqueInsuficienteParaOS() {
            // Arrange
            UUID osId = UUID.randomUUID();
            UUID pecaId = UUID.randomUUID();
            Peca peca = criarPecaValida(pecaId);
            peca.setQuantidadeAtual(1); // Quantidade insuficiente

            ItemOS item = criarItemOS(pecaId, 5);

            when(pecaRepository.findByOficinaIdAndId(OFICINA_ID, pecaId))
                    .thenReturn(Optional.of(peca));

            // Act & Assert
            assertThatThrownBy(() -> movimentacaoService.baixarEstoquePorOS(
                    osId, List.of(item), USUARIO_ID
            )).isInstanceOf(EstoqueInsuficienteException.class);

            verify(pecaRepository, never()).save(any());
        }
    }

    // ==================== ESTORNAR ESTOQUE POR OS ====================

    @Nested
    @DisplayName("estornarEstoquePorOS()")
    class EstornarEstoquePorOS {

        @Test
        @DisplayName("Deve estornar estoque por OS cancelada com sucesso")
        void deveEstornarEstoquePorOSCanceladaComSucesso() {
            // Arrange
            UUID osId = UUID.randomUUID();
            UUID pecaId = UUID.randomUUID();
            Peca peca = criarPecaValida(pecaId);
            peca.setQuantidadeAtual(8); // Apos baixa

            MovimentacaoEstoque baixa = MovimentacaoEstoque.builder()
                    .id(UUID.randomUUID())
                    .pecaId(pecaId)
                    .ordemServicoId(osId)
                    .tipo(TipoMovimentacao.BAIXA_OS)
                    .quantidade(2)
                    .quantidadeAnterior(10)
                    .quantidadeAtual(8)
                    .valorUnitario(new BigDecimal("50.00"))
                    .build();

            when(movimentacaoRepository.findByOficinaIdAndOrdemServicoIdOrderByCreatedAtDesc(OFICINA_ID, osId))
                    .thenReturn(List.of(baixa));
            when(pecaRepository.findByOficinaIdAndIdForUpdate(OFICINA_ID, pecaId))
                    .thenReturn(Optional.of(peca));
            when(pecaRepository.save(any(Peca.class))).thenAnswer(inv -> inv.getArgument(0));
            when(movimentacaoRepository.save(any(MovimentacaoEstoque.class))).thenAnswer(inv -> {
                MovimentacaoEstoque m = inv.getArgument(0);
                return MovimentacaoEstoque.builder()
                        .id(UUID.randomUUID())
                        .pecaId(m.getPecaId())
                        .ordemServicoId(m.getOrdemServicoId())
                        .tipo(m.getTipo())
                        .quantidade(m.getQuantidade())
                        .quantidadeAnterior(m.getQuantidadeAnterior())
                        .quantidadeAtual(m.getQuantidadeAtual())
                        .valorUnitario(m.getValorUnitario())
                        .build();
            });

            // Act
            List<MovimentacaoEstoque> resultado = movimentacaoService.estornarEstoquePorOS(osId, USUARIO_ID);

            // Assert
            assertThat(resultado).hasSize(1);
            assertThat(resultado.get(0).getTipo()).isEqualTo(TipoMovimentacao.DEVOLUCAO);
            verify(pecaRepository).save(argThat(p -> p.getQuantidadeAtual() == 10)); // Estoque restaurado
        }

        @Test
        @DisplayName("Deve retornar lista vazia quando OS nao tem movimentacoes de baixa")
        void deveRetornarListaVaziaQuandoOSNaoTemBaixas() {
            // Arrange
            UUID osId = UUID.randomUUID();
            when(movimentacaoRepository.findByOficinaIdAndOrdemServicoIdOrderByCreatedAtDesc(OFICINA_ID, osId))
                    .thenReturn(List.of());

            // Act
            List<MovimentacaoEstoque> resultado = movimentacaoService.estornarEstoquePorOS(osId, USUARIO_ID);

            // Assert
            assertThat(resultado).isEmpty();
        }
    }

    // ==================== CONSULTAS ====================

    @Nested
    @DisplayName("consultas")
    class Consultas {

        @Test
        @DisplayName("Deve buscar historico de peca")
        void deveBuscarHistoricoPeca() {
            // Arrange
            UUID pecaId = UUID.randomUUID();
            Pageable pageable = PageRequest.of(0, 10);
            MovimentacaoEstoque mov = criarMovimentacao(pecaId, TipoMovimentacao.ENTRADA);
            Page<MovimentacaoEstoque> page = new PageImpl<>(List.of(mov), pageable, 1);

            when(movimentacaoRepository.findByOficinaIdAndPecaIdOrderByDataMovimentacaoDesc(
                    OFICINA_ID, pecaId, pageable)).thenReturn(page);

            // Act
            Page<MovimentacaoEstoque> resultado = movimentacaoService.buscarHistoricoPeca(pecaId, pageable);

            // Assert
            assertThat(resultado.getContent()).hasSize(1);
            assertThat(resultado.getContent().get(0).getPecaId()).isEqualTo(pecaId);
        }

        @Test
        @DisplayName("Deve buscar movimentacoes por OS")
        void deveBuscarMovimentacoesPorOS() {
            // Arrange
            UUID osId = UUID.randomUUID();
            UUID pecaId = UUID.randomUUID();
            MovimentacaoEstoque mov = criarMovimentacao(pecaId, TipoMovimentacao.BAIXA_OS);

            when(movimentacaoRepository.findByOficinaIdAndOrdemServicoIdOrderByCreatedAtDesc(OFICINA_ID, osId))
                    .thenReturn(List.of(mov));

            // Act
            List<MovimentacaoEstoque> resultado = movimentacaoService.buscarPorOS(osId);

            // Assert
            assertThat(resultado).hasSize(1);
        }

        @Test
        @DisplayName("Deve buscar movimentacoes com filtros")
        void deveBuscarMovimentacoesComFiltros() {
            // Arrange
            UUID pecaId = UUID.randomUUID();
            Pageable pageable = PageRequest.of(0, 10);
            LocalDateTime dataInicio = LocalDateTime.now().minusDays(7);
            LocalDateTime dataFim = LocalDateTime.now();
            MovimentacaoEstoque mov = criarMovimentacao(pecaId, TipoMovimentacao.ENTRADA);
            Page<MovimentacaoEstoque> page = new PageImpl<>(List.of(mov), pageable, 1);

            when(movimentacaoRepository.findByFilters(
                    OFICINA_ID, pecaId, TipoMovimentacao.ENTRADA, dataInicio, dataFim, USUARIO_ID, pageable
            )).thenReturn(page);

            // Act
            Page<MovimentacaoEstoque> resultado = movimentacaoService.buscarComFiltros(
                    pecaId, TipoMovimentacao.ENTRADA, dataInicio, dataFim, USUARIO_ID, pageable
            );

            // Assert
            assertThat(resultado.getContent()).hasSize(1);
        }
    }

    // ==================== HELPER METHODS ====================

    private Peca criarPecaValida(UUID id) {
        Peca peca = Peca.builder()
                .codigo("FIL-001")
                .descricao("Filtro de Oleo Motor")
                .marca("Fram")
                .unidadeMedida(UnidadeMedida.UNIDADE)
                .quantidadeAtual(10)
                .quantidadeMinima(5)
                .valorCusto(new BigDecimal("25.00"))
                .valorVenda(new BigDecimal("50.00"))
                .ativo(true)
                .build();
        peca.setId(id);
        return peca;
    }

    private ItemOS criarItemOS(UUID pecaId, int quantidade) {
        return ItemOS.builder()
                .tipo(TipoItem.PECA)
                .origemPeca(OrigemPeca.ESTOQUE)
                .pecaId(pecaId)
                .descricao("Filtro de Oleo")
                .quantidade(quantidade)
                .valorUnitario(new BigDecimal("50.00"))
                .build();
    }

    private MovimentacaoEstoque criarMovimentacao(UUID pecaId, TipoMovimentacao tipo) {
        return MovimentacaoEstoque.builder()
                .id(UUID.randomUUID())
                .pecaId(pecaId)
                .usuarioId(USUARIO_ID)
                .tipo(tipo)
                .quantidade(5)
                .quantidadeAnterior(10)
                .quantidadeAtual(tipo == TipoMovimentacao.ENTRADA ? 15 : 5)
                .valorUnitario(new BigDecimal("25.00"))
                .dataMovimentacao(LocalDateTime.now())
                .build();
    }
}
