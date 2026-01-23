package com.pitstop.estoque.service;

import com.pitstop.estoque.domain.Peca;
import com.pitstop.estoque.domain.UnidadeMedida;
import com.pitstop.estoque.exception.CodigoPecaDuplicadoException;
import com.pitstop.estoque.exception.PecaNotFoundException;
import com.pitstop.estoque.repository.PecaRepository;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Testes unitarios para EstoqueService.
 * Testa operacoes CRUD e consultas de pecas do estoque.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EstoqueService - Testes Unitarios")
class EstoqueServiceTest {

    @Mock
    private PecaRepository pecaRepository;

    @InjectMocks
    private EstoqueService estoqueService;

    private static MockedStatic<TenantContext> tenantContextMock;
    private static final UUID OFICINA_ID = UUID.randomUUID();

    @BeforeAll
    static void setUpAll() {
        tenantContextMock = mockStatic(TenantContext.class);
        tenantContextMock.when(TenantContext::getTenantId).thenReturn(OFICINA_ID);
    }

    @AfterAll
    static void tearDownAll() {
        tenantContextMock.close();
    }

    // ==================== CRIAR ====================

    @Nested
    @DisplayName("criar()")
    class Criar {

        @Test
        @DisplayName("Deve criar peca com sucesso")
        void deveCriarPecaComSucesso() {
            // Arrange
            Peca peca = criarPecaValida();
            when(pecaRepository.existsByOficinaIdAndCodigoAndAtivoTrue(OFICINA_ID, peca.getCodigo()))
                    .thenReturn(false);
            when(pecaRepository.save(any(Peca.class))).thenAnswer(invocation -> {
                Peca p = invocation.getArgument(0);
                p.setId(UUID.randomUUID());
                return p;
            });

            // Act
            Peca resultado = estoqueService.criar(peca);

            // Assert
            assertThat(resultado).isNotNull();
            assertThat(resultado.getId()).isNotNull();
            assertThat(resultado.getCodigo()).isEqualTo("FIL-001");
            verify(pecaRepository).save(peca);
        }

        @Test
        @DisplayName("Deve criar peca com quantidade inicial zero quando nao informada")
        void deveCriarPecaComQuantidadeZeroQuandoNaoInformada() {
            // Arrange
            Peca peca = criarPecaValida();
            peca.setQuantidadeAtual(null);
            when(pecaRepository.existsByOficinaIdAndCodigoAndAtivoTrue(OFICINA_ID, peca.getCodigo()))
                    .thenReturn(false);
            when(pecaRepository.save(any(Peca.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            Peca resultado = estoqueService.criar(peca);

            // Assert
            assertThat(resultado.getQuantidadeAtual()).isEqualTo(0);
        }

        @Test
        @DisplayName("Deve lancar excecao quando codigo duplicado")
        void deveLancarExcecaoQuandoCodigoDuplicado() {
            // Arrange
            Peca peca = criarPecaValida();
            when(pecaRepository.existsByOficinaIdAndCodigoAndAtivoTrue(OFICINA_ID, peca.getCodigo()))
                    .thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> estoqueService.criar(peca))
                    .isInstanceOf(CodigoPecaDuplicadoException.class);

            verify(pecaRepository, never()).save(any());
        }
    }

    // ==================== ATUALIZAR ====================

    @Nested
    @DisplayName("atualizar()")
    class Atualizar {

        @Test
        @DisplayName("Deve atualizar peca com sucesso")
        void deveAtualizarPecaComSucesso() {
            // Arrange
            UUID pecaId = UUID.randomUUID();
            Peca pecaExistente = criarPecaValida();
            pecaExistente.setId(pecaId);
            pecaExistente.setQuantidadeAtual(10);

            Peca pecaAtualizada = criarPecaValida();
            pecaAtualizada.setDescricao("Filtro de Oleo Atualizado");
            pecaAtualizada.setValorVenda(new BigDecimal("65.00"));

            when(pecaRepository.findByOficinaIdAndId(OFICINA_ID, pecaId))
                    .thenReturn(Optional.of(pecaExistente));
            when(pecaRepository.save(any(Peca.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            Peca resultado = estoqueService.atualizar(pecaId, pecaAtualizada);

            // Assert
            assertThat(resultado.getDescricao()).isEqualTo("Filtro de Oleo Atualizado");
            assertThat(resultado.getValorVenda()).isEqualByComparingTo("65.00");
            // Quantidade atual nao deve ser alterada pelo atualizar()
            assertThat(resultado.getQuantidadeAtual()).isEqualTo(10);
        }

        @Test
        @DisplayName("Deve lancar excecao quando peca nao encontrada")
        void deveLancarExcecaoQuandoPecaNaoEncontrada() {
            // Arrange
            UUID pecaId = UUID.randomUUID();
            Peca pecaAtualizada = criarPecaValida();
            when(pecaRepository.findByOficinaIdAndId(OFICINA_ID, pecaId))
                    .thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> estoqueService.atualizar(pecaId, pecaAtualizada))
                    .isInstanceOf(PecaNotFoundException.class);
        }

        @Test
        @DisplayName("Deve lancar excecao ao tentar alterar para codigo duplicado")
        void deveLancarExcecaoAoTentarAlterarParaCodigoDuplicado() {
            // Arrange
            UUID pecaId = UUID.randomUUID();
            Peca pecaExistente = criarPecaValida();
            pecaExistente.setId(pecaId);
            pecaExistente.setCodigo("FIL-001");

            Peca pecaAtualizada = criarPecaValida();
            pecaAtualizada.setCodigo("FIL-002"); // Codigo diferente

            when(pecaRepository.findByOficinaIdAndId(OFICINA_ID, pecaId))
                    .thenReturn(Optional.of(pecaExistente));
            when(pecaRepository.existsByOficinaIdAndCodigoAndAtivoTrue(OFICINA_ID, "FIL-002"))
                    .thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> estoqueService.atualizar(pecaId, pecaAtualizada))
                    .isInstanceOf(CodigoPecaDuplicadoException.class);
        }
    }

    // ==================== BUSCAR POR ID ====================

    @Nested
    @DisplayName("buscarPorId()")
    class BuscarPorId {

        @Test
        @DisplayName("Deve buscar peca por ID com sucesso")
        void deveBuscarPecaPorIdComSucesso() {
            // Arrange
            UUID pecaId = UUID.randomUUID();
            Peca peca = criarPecaValida();
            peca.setId(pecaId);
            when(pecaRepository.findByOficinaIdAndId(OFICINA_ID, pecaId))
                    .thenReturn(Optional.of(peca));

            // Act
            Peca resultado = estoqueService.buscarPorId(pecaId);

            // Assert
            assertThat(resultado).isNotNull();
            assertThat(resultado.getId()).isEqualTo(pecaId);
        }

        @Test
        @DisplayName("Deve lancar excecao quando peca nao encontrada")
        void deveLancarExcecaoQuandoPecaNaoEncontrada() {
            // Arrange
            UUID pecaId = UUID.randomUUID();
            when(pecaRepository.findByOficinaIdAndId(OFICINA_ID, pecaId))
                    .thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> estoqueService.buscarPorId(pecaId))
                    .isInstanceOf(PecaNotFoundException.class);
        }
    }

    // ==================== BUSCAR POR CODIGO ====================

    @Nested
    @DisplayName("buscarPorCodigo()")
    class BuscarPorCodigo {

        @Test
        @DisplayName("Deve buscar peca por codigo com sucesso")
        void deveBuscarPecaPorCodigoComSucesso() {
            // Arrange
            Peca peca = criarPecaValida();
            peca.setId(UUID.randomUUID());
            when(pecaRepository.findByOficinaIdAndCodigoAndAtivoTrue(OFICINA_ID, "FIL-001"))
                    .thenReturn(Optional.of(peca));

            // Act
            Peca resultado = estoqueService.buscarPorCodigo("FIL-001");

            // Assert
            assertThat(resultado).isNotNull();
            assertThat(resultado.getCodigo()).isEqualTo("FIL-001");
        }

        @Test
        @DisplayName("Deve lancar excecao quando codigo nao encontrado")
        void deveLancarExcecaoQuandoCodigoNaoEncontrado() {
            // Arrange
            when(pecaRepository.findByOficinaIdAndCodigoAndAtivoTrue(OFICINA_ID, "INEXISTENTE"))
                    .thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> estoqueService.buscarPorCodigo("INEXISTENTE"))
                    .isInstanceOf(PecaNotFoundException.class);
        }
    }

    // ==================== LISTAR ====================

    @Nested
    @DisplayName("listar()")
    class Listar {

        @Test
        @DisplayName("Deve listar todas as pecas com paginacao")
        void deveListarTodasAsPecasComPaginacao() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            List<Peca> pecas = List.of(criarPecaValida(), criarPecaValida());
            Page<Peca> page = new PageImpl<>(pecas, pageable, 2);
            when(pecaRepository.findByOficinaIdAndAtivoTrueOrderByDescricaoAsc(OFICINA_ID, pageable))
                    .thenReturn(page);

            // Act
            Page<Peca> resultado = estoqueService.listarTodas(pageable);

            // Assert
            assertThat(resultado).isNotNull();
            assertThat(resultado.getContent()).hasSize(2);
            assertThat(resultado.getTotalElements()).isEqualTo(2);
        }

        @Test
        @DisplayName("Deve retornar pagina vazia quando nao houver pecas")
        void deveRetornarPaginaVaziaQuandoNaoHouverPecas() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            Page<Peca> page = new PageImpl<>(List.of(), pageable, 0);
            when(pecaRepository.findByOficinaIdAndAtivoTrueOrderByDescricaoAsc(OFICINA_ID, pageable))
                    .thenReturn(page);

            // Act
            Page<Peca> resultado = estoqueService.listarTodas(pageable);

            // Assert
            assertThat(resultado).isNotNull();
            assertThat(resultado.getContent()).isEmpty();
        }
    }

    // ==================== ESTOQUE BAIXO/ZERADO ====================

    @Nested
    @DisplayName("estoque baixo/zerado")
    class EstoqueBaixo {

        @Test
        @DisplayName("Deve listar pecas com estoque baixo")
        void deveListarPecasComEstoqueBaixo() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            Peca peca = criarPecaValida();
            peca.setQuantidadeAtual(2);
            peca.setQuantidadeMinima(5);
            Page<Peca> page = new PageImpl<>(List.of(peca), pageable, 1);
            when(pecaRepository.findEstoqueBaixo(OFICINA_ID, pageable)).thenReturn(page);

            // Act
            Page<Peca> resultado = estoqueService.listarEstoqueBaixo(pageable);

            // Assert
            assertThat(resultado.getContent()).hasSize(1);
            assertThat(resultado.getContent().get(0).isEstoqueBaixo()).isTrue();
        }

        @Test
        @DisplayName("Deve listar pecas com estoque zerado")
        void deveListarPecasComEstoqueZerado() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            Peca peca = criarPecaValida();
            peca.setQuantidadeAtual(0);
            Page<Peca> page = new PageImpl<>(List.of(peca), pageable, 1);
            when(pecaRepository.findEstoqueZerado(OFICINA_ID, pageable)).thenReturn(page);

            // Act
            Page<Peca> resultado = estoqueService.listarEstoqueZerado(pageable);

            // Assert
            assertThat(resultado.getContent()).hasSize(1);
            assertThat(resultado.getContent().get(0).getQuantidadeAtual()).isEqualTo(0);
        }

        @Test
        @DisplayName("Deve contar pecas com estoque baixo")
        void deveContarPecasComEstoqueBaixo() {
            // Arrange
            when(pecaRepository.countEstoqueBaixo(OFICINA_ID)).thenReturn(5L);

            // Act
            long resultado = estoqueService.contarEstoqueBaixo();

            // Assert
            assertThat(resultado).isEqualTo(5L);
        }

        @Test
        @DisplayName("Deve contar pecas com estoque zerado")
        void deveContarPecasComEstoqueZerado() {
            // Arrange
            when(pecaRepository.countEstoqueZerado(OFICINA_ID)).thenReturn(3L);

            // Act
            long resultado = estoqueService.contarEstoqueZerado();

            // Assert
            assertThat(resultado).isEqualTo(3L);
        }
    }

    // ==================== DESATIVAR / REATIVAR ====================

    @Nested
    @DisplayName("desativar/reativar")
    class DesativarReativar {

        @Test
        @DisplayName("Deve desativar peca com sucesso")
        void deveDesativarPecaComSucesso() {
            // Arrange
            UUID pecaId = UUID.randomUUID();
            Peca peca = criarPecaValida();
            peca.setId(pecaId);
            peca.setAtivo(true);
            when(pecaRepository.findByOficinaIdAndId(OFICINA_ID, pecaId))
                    .thenReturn(Optional.of(peca));
            when(pecaRepository.save(any(Peca.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            estoqueService.desativar(pecaId);

            // Assert
            verify(pecaRepository).save(argThat(p -> !p.getAtivo()));
        }

        @Test
        @DisplayName("Deve lancar excecao ao desativar peca inexistente")
        void deveLancarExcecaoAoDesativarPecaInexistente() {
            // Arrange
            UUID pecaId = UUID.randomUUID();
            when(pecaRepository.findByOficinaIdAndId(OFICINA_ID, pecaId))
                    .thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> estoqueService.desativar(pecaId))
                    .isInstanceOf(PecaNotFoundException.class);
        }

        @Test
        @DisplayName("Deve reativar peca com sucesso")
        void deveReativarPecaComSucesso() {
            // Arrange
            UUID pecaId = UUID.randomUUID();
            Peca peca = criarPecaValida();
            peca.setId(pecaId);
            peca.setAtivo(false);
            when(pecaRepository.findByOficinaIdAndId(OFICINA_ID, pecaId))
                    .thenReturn(Optional.of(peca));
            when(pecaRepository.existsByOficinaIdAndCodigoAndAtivoTrue(OFICINA_ID, peca.getCodigo()))
                    .thenReturn(false);
            when(pecaRepository.save(any(Peca.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            estoqueService.reativar(pecaId);

            // Assert
            verify(pecaRepository).save(argThat(Peca::getAtivo));
        }

        @Test
        @DisplayName("Deve lancar excecao ao reativar peca com codigo duplicado")
        void deveLancarExcecaoAoReativarPecaComCodigoDuplicado() {
            // Arrange
            UUID pecaId = UUID.randomUUID();
            Peca peca = criarPecaValida();
            peca.setId(pecaId);
            peca.setAtivo(false);
            when(pecaRepository.findByOficinaIdAndId(OFICINA_ID, pecaId))
                    .thenReturn(Optional.of(peca));
            when(pecaRepository.existsByOficinaIdAndCodigoAndAtivoTrue(OFICINA_ID, peca.getCodigo()))
                    .thenReturn(true); // Ja existe peca ativa com mesmo codigo

            // Act & Assert
            assertThatThrownBy(() -> estoqueService.reativar(pecaId))
                    .isInstanceOf(CodigoPecaDuplicadoException.class);
        }
    }

    // ==================== CONSULTAS ADICIONAIS ====================

    @Nested
    @DisplayName("consultas adicionais")
    class ConsultasAdicionais {

        @Test
        @DisplayName("Deve buscar pecas por marca")
        void deveBuscarPecasPorMarca() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            Peca peca = criarPecaValida();
            peca.setMarca("Fram");
            Page<Peca> page = new PageImpl<>(List.of(peca), pageable, 1);
            when(pecaRepository.findByOficinaIdAndMarcaContainingIgnoreCaseAndAtivoTrue(
                    OFICINA_ID, "Fram", pageable)).thenReturn(page);

            // Act
            Page<Peca> resultado = estoqueService.buscarPorMarca("Fram", pageable);

            // Assert
            assertThat(resultado.getContent()).hasSize(1);
            assertThat(resultado.getContent().get(0).getMarca()).isEqualTo("Fram");
        }

        @Test
        @DisplayName("Deve buscar pecas por descricao")
        void deveBuscarPecasPorDescricao() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            Peca peca = criarPecaValida();
            Page<Peca> page = new PageImpl<>(List.of(peca), pageable, 1);
            when(pecaRepository.findByOficinaIdAndDescricaoContainingIgnoreCaseAndAtivoTrue(
                    OFICINA_ID, "Filtro", pageable)).thenReturn(page);

            // Act
            Page<Peca> resultado = estoqueService.buscarPorDescricao("Filtro", pageable);

            // Assert
            assertThat(resultado.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("Deve calcular valor total do inventario")
        void deveCalcularValorTotalInventario() {
            // Arrange
            BigDecimal valorTotal = new BigDecimal("5000.00");
            when(pecaRepository.calcularValorTotalInventario(OFICINA_ID)).thenReturn(valorTotal);

            // Act
            BigDecimal resultado = estoqueService.calcularValorTotalInventario();

            // Assert
            assertThat(resultado).isEqualByComparingTo("5000.00");
        }

        @Test
        @DisplayName("Deve listar marcas distintas")
        void deveListarMarcasDistintas() {
            // Arrange
            List<String> marcas = List.of("Fram", "Bosch", "NGK");
            when(pecaRepository.findDistinctMarcas(OFICINA_ID)).thenReturn(marcas);

            // Act
            List<String> resultado = estoqueService.listarMarcas();

            // Assert
            assertThat(resultado).containsExactly("Fram", "Bosch", "NGK");
        }
    }

    // ==================== LOCALIZACAO ====================

    @Nested
    @DisplayName("localizacao de pecas")
    class Localizacao {

        @Test
        @DisplayName("Deve listar pecas sem localizacao")
        void deveListarPecasSemLocalizacao() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            Peca peca = criarPecaValida();
            peca.setLocalArmazenamento(null);
            Page<Peca> page = new PageImpl<>(List.of(peca), pageable, 1);
            when(pecaRepository.findPecasSemLocalizacao(OFICINA_ID, pageable)).thenReturn(page);

            // Act
            Page<Peca> resultado = estoqueService.listarPecasSemLocalizacao(pageable);

            // Assert
            assertThat(resultado.getContent()).hasSize(1);
            assertThat(resultado.getContent().get(0).getLocalArmazenamento()).isNull();
        }

        @Test
        @DisplayName("Deve contar pecas sem localizacao")
        void deveContarPecasSemLocalizacao() {
            // Arrange
            when(pecaRepository.countPecasSemLocalizacao(OFICINA_ID)).thenReturn(7L);

            // Act
            long resultado = estoqueService.contarPecasSemLocalizacao();

            // Assert
            assertThat(resultado).isEqualTo(7L);
        }

        @Test
        @DisplayName("Deve definir localizacao da peca")
        void deveDefinirLocalizacaoDaPeca() {
            // Arrange
            UUID pecaId = UUID.randomUUID();
            UUID localId = UUID.randomUUID();
            Peca peca = criarPecaValida();
            peca.setId(pecaId);
            when(pecaRepository.findByOficinaIdAndId(OFICINA_ID, pecaId))
                    .thenReturn(Optional.of(peca));
            when(pecaRepository.save(any(Peca.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            Peca resultado = estoqueService.definirLocalizacaoPeca(pecaId, localId);

            // Assert
            assertThat(resultado.getLocalArmazenamento()).isNotNull();
            assertThat(resultado.getLocalArmazenamento().getId()).isEqualTo(localId);
        }

        @Test
        @DisplayName("Deve remover localizacao da peca quando localId for null")
        void deveRemoverLocalizacaoDaPeca() {
            // Arrange
            UUID pecaId = UUID.randomUUID();
            Peca peca = criarPecaValida();
            peca.setId(pecaId);
            when(pecaRepository.findByOficinaIdAndId(OFICINA_ID, pecaId))
                    .thenReturn(Optional.of(peca));
            when(pecaRepository.save(any(Peca.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            Peca resultado = estoqueService.definirLocalizacaoPeca(pecaId, null);

            // Assert
            assertThat(resultado.getLocalArmazenamento()).isNull();
        }
    }

    // ==================== HELPER METHODS ====================

    private Peca criarPecaValida() {
        return Peca.builder()
                .codigo("FIL-001")
                .descricao("Filtro de Oleo Motor")
                .marca("Fram")
                .aplicacao("Motores 1.0 e 1.4")
                .unidadeMedida(UnidadeMedida.UNIDADE)
                .quantidadeAtual(10)
                .quantidadeMinima(5)
                .valorCusto(new BigDecimal("25.00"))
                .valorVenda(new BigDecimal("50.00"))
                .ativo(true)
                .build();
    }
}
