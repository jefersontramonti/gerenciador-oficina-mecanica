package com.pitstop.estoque.service;

import com.pitstop.estoque.domain.LocalArmazenamento;
import com.pitstop.estoque.domain.TipoLocal;
import com.pitstop.estoque.dto.CreateLocalArmazenamentoDTO;
import com.pitstop.estoque.dto.UpdateLocalArmazenamentoDTO;
import com.pitstop.estoque.exception.CicloHierarquicoException;
import com.pitstop.estoque.exception.LocalComPecasVinculadasException;
import com.pitstop.estoque.mapper.LocalArmazenamentoMapper;
import com.pitstop.estoque.repository.LocalArmazenamentoRepository;
import com.pitstop.shared.exception.ResourceNotFoundException;
import com.pitstop.shared.security.tenant.TenantContext;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Testes unitarios para LocalArmazenamentoService.
 * Testa operacoes CRUD e regras de hierarquia de locais de armazenamento.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LocalArmazenamentoService - Testes Unitarios")
class LocalArmazenamentoServiceTest {

    @Mock
    private LocalArmazenamentoRepository localRepository;

    @Mock
    private LocalArmazenamentoMapper localMapper;

    @InjectMocks
    private LocalArmazenamentoService localService;

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
        @DisplayName("Deve criar local raiz com sucesso")
        void deveCriarLocalRaizComSucesso() {
            // Arrange
            CreateLocalArmazenamentoDTO dto = new CreateLocalArmazenamentoDTO(
                    "DEP-01", TipoLocal.DEPOSITO, "Deposito Principal", null, 100, null
            );
            LocalArmazenamento local = criarLocalValido(TipoLocal.DEPOSITO);
            local.setLocalizacaoPai(null);

            when(localRepository.existsByOficinaIdAndCodigo(OFICINA_ID, "DEP-01")).thenReturn(false);
            when(localMapper.toEntity(dto)).thenReturn(local);
            when(localRepository.save(any(LocalArmazenamento.class))).thenAnswer(inv -> {
                LocalArmazenamento l = inv.getArgument(0);
                l.setId(UUID.randomUUID());
                return l;
            });

            // Act
            LocalArmazenamento resultado = localService.criar(dto);

            // Assert
            assertThat(resultado).isNotNull();
            assertThat(resultado.getId()).isNotNull();
            verify(localRepository).save(local);
        }

        @Test
        @DisplayName("Deve criar local filho com sucesso")
        void deveCriarLocalFilhoComSucesso() {
            // Arrange
            UUID paiId = UUID.randomUUID();
            LocalArmazenamento pai = criarLocalValido(TipoLocal.DEPOSITO);
            pai.setId(paiId);

            CreateLocalArmazenamentoDTO dto = new CreateLocalArmazenamentoDTO(
                    "PRAT-01", TipoLocal.PRATELEIRA, "Prateleira 1", paiId, 50, null
            );
            LocalArmazenamento local = criarLocalValido(TipoLocal.PRATELEIRA);

            when(localRepository.existsByOficinaIdAndCodigo(OFICINA_ID, "PRAT-01")).thenReturn(false);
            when(localMapper.toEntity(dto)).thenReturn(local);
            when(localRepository.findByOficinaIdAndId(OFICINA_ID, paiId)).thenReturn(Optional.of(pai));
            when(localRepository.save(any(LocalArmazenamento.class))).thenAnswer(inv -> {
                LocalArmazenamento l = inv.getArgument(0);
                l.setId(UUID.randomUUID());
                return l;
            });

            // Act
            LocalArmazenamento resultado = localService.criar(dto);

            // Assert
            assertThat(resultado).isNotNull();
            assertThat(resultado.getLocalizacaoPai()).isEqualTo(pai);
        }

        @Test
        @DisplayName("Deve lancar excecao quando codigo duplicado")
        void deveLancarExcecaoQuandoCodigoDuplicado() {
            // Arrange
            CreateLocalArmazenamentoDTO dto = new CreateLocalArmazenamentoDTO(
                    "DEP-01", TipoLocal.DEPOSITO, "Deposito", null, null, null
            );
            when(localRepository.existsByOficinaIdAndCodigo(OFICINA_ID, "DEP-01")).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> localService.criar(dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("DEP-01");
        }

        @Test
        @DisplayName("Deve lancar excecao quando tipo requer pai mas nao foi informado")
        void deveLancarExcecaoQuandoTipoRequerPaiMasNaoInformado() {
            // Arrange
            CreateLocalArmazenamentoDTO dto = new CreateLocalArmazenamentoDTO(
                    "GAV-01", TipoLocal.GAVETA, "Gaveta 1", null, null, null
            );
            LocalArmazenamento local = criarLocalValido(TipoLocal.GAVETA);
            local.setLocalizacaoPai(null);

            when(localRepository.existsByOficinaIdAndCodigo(OFICINA_ID, "GAV-01")).thenReturn(false);
            when(localMapper.toEntity(dto)).thenReturn(local);

            // Act & Assert
            assertThatThrownBy(() -> localService.criar(dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("raiz");
        }
    }

    // ==================== BUSCAR ====================

    @Nested
    @DisplayName("buscar()")
    class Buscar {

        @Test
        @DisplayName("Deve buscar local por ID com sucesso")
        void deveBuscarLocalPorIdComSucesso() {
            // Arrange
            UUID localId = UUID.randomUUID();
            LocalArmazenamento local = criarLocalValido(TipoLocal.DEPOSITO);
            local.setId(localId);
            when(localRepository.findByOficinaIdAndId(OFICINA_ID, localId))
                    .thenReturn(Optional.of(local));

            // Act
            LocalArmazenamento resultado = localService.buscarPorId(localId);

            // Assert
            assertThat(resultado).isNotNull();
            assertThat(resultado.getId()).isEqualTo(localId);
        }

        @Test
        @DisplayName("Deve lancar excecao quando local nao encontrado por ID")
        void deveLancarExcecaoQuandoLocalNaoEncontradoPorId() {
            // Arrange
            UUID localId = UUID.randomUUID();
            when(localRepository.findByOficinaIdAndId(OFICINA_ID, localId))
                    .thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> localService.buscarPorId(localId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Deve buscar local por codigo com sucesso")
        void deveBuscarLocalPorCodigoComSucesso() {
            // Arrange
            LocalArmazenamento local = criarLocalValido(TipoLocal.DEPOSITO);
            local.setId(UUID.randomUUID());
            when(localRepository.findByOficinaIdAndCodigo(OFICINA_ID, "DEP-01"))
                    .thenReturn(Optional.of(local));

            // Act
            LocalArmazenamento resultado = localService.buscarPorCodigo("dep-01"); // Normaliza para uppercase

            // Assert
            assertThat(resultado).isNotNull();
        }

        @Test
        @DisplayName("Deve lancar excecao quando codigo nao encontrado")
        void deveLancarExcecaoQuandoCodigoNaoEncontrado() {
            // Arrange
            when(localRepository.findByOficinaIdAndCodigo(OFICINA_ID, "INEXISTENTE"))
                    .thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> localService.buscarPorCodigo("INEXISTENTE"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ==================== LISTAR ====================

    @Nested
    @DisplayName("listar()")
    class Listar {

        @Test
        @DisplayName("Deve listar todos os locais ativos")
        void deveListarTodosOsLocaisAtivos() {
            // Arrange
            List<LocalArmazenamento> locais = List.of(
                    criarLocalValido(TipoLocal.DEPOSITO),
                    criarLocalValido(TipoLocal.ARMARIO)
            );
            when(localRepository.findByOficinaIdAndAtivoTrue(OFICINA_ID)).thenReturn(locais);

            // Act
            List<LocalArmazenamento> resultado = localService.listarTodos();

            // Assert
            assertThat(resultado).hasSize(2);
        }

        @Test
        @DisplayName("Deve listar locais raiz")
        void deveListarLocaisRaiz() {
            // Arrange
            LocalArmazenamento deposito = criarLocalValido(TipoLocal.DEPOSITO);
            deposito.setLocalizacaoPai(null);
            when(localRepository.findLocaisRaiz(OFICINA_ID)).thenReturn(List.of(deposito));

            // Act
            List<LocalArmazenamento> resultado = localService.listarLocaisRaiz();

            // Assert
            assertThat(resultado).hasSize(1);
            assertThat(resultado.get(0).isRaiz()).isTrue();
        }

        @Test
        @DisplayName("Deve listar filhos de um local")
        void deveListarFilhosDeUmLocal() {
            // Arrange
            UUID paiId = UUID.randomUUID();
            LocalArmazenamento filho = criarLocalValido(TipoLocal.PRATELEIRA);
            when(localRepository.findByOficinaIdAndLocalizacaoPaiId(OFICINA_ID, paiId))
                    .thenReturn(List.of(filho));

            // Act
            List<LocalArmazenamento> resultado = localService.listarFilhos(paiId);

            // Assert
            assertThat(resultado).hasSize(1);
        }

        @Test
        @DisplayName("Deve listar locais por tipo")
        void deveListarLocaisPorTipo() {
            // Arrange
            List<LocalArmazenamento> armarios = List.of(criarLocalValido(TipoLocal.ARMARIO));
            when(localRepository.findByOficinaIdAndTipoAndAtivoTrue(OFICINA_ID, TipoLocal.ARMARIO))
                    .thenReturn(armarios);

            // Act
            List<LocalArmazenamento> resultado = localService.listarPorTipo(TipoLocal.ARMARIO);

            // Assert
            assertThat(resultado).hasSize(1);
            assertThat(resultado.get(0).getTipo()).isEqualTo(TipoLocal.ARMARIO);
        }

        @Test
        @DisplayName("Deve buscar locais por descricao")
        void deveBuscarLocaisPorDescricao() {
            // Arrange
            LocalArmazenamento local = criarLocalValido(TipoLocal.PRATELEIRA);
            local.setDescricao("Prateleira de Filtros");
            when(localRepository.findByOficinaIdAndDescricaoContainingIgnoreCaseAndAtivoTrue(
                    OFICINA_ID, "Filtros")).thenReturn(List.of(local));

            // Act
            List<LocalArmazenamento> resultado = localService.buscarPorDescricao("Filtros");

            // Assert
            assertThat(resultado).hasSize(1);
        }
    }

    // ==================== ATUALIZAR ====================

    @Nested
    @DisplayName("atualizar()")
    class Atualizar {

        @Test
        @DisplayName("Deve atualizar local com sucesso")
        void deveAtualizarLocalComSucesso() {
            // Arrange
            UUID localId = UUID.randomUUID();
            LocalArmazenamento local = criarLocalValido(TipoLocal.DEPOSITO);
            local.setId(localId);
            local.setCodigo("DEP-01");

            UpdateLocalArmazenamentoDTO dto = new UpdateLocalArmazenamentoDTO(
                    "DEP-01", TipoLocal.DEPOSITO, "Deposito Atualizado", null, 200, "Nova observacao"
            );

            when(localRepository.findByOficinaIdAndId(OFICINA_ID, localId))
                    .thenReturn(Optional.of(local));
            doNothing().when(localMapper).updateEntityFromDTO(dto, local);
            when(localRepository.save(any(LocalArmazenamento.class))).thenAnswer(inv -> inv.getArgument(0));

            // Act
            LocalArmazenamento resultado = localService.atualizar(localId, dto);

            // Assert
            assertThat(resultado).isNotNull();
            verify(localMapper).updateEntityFromDTO(dto, local);
            verify(localRepository).save(local);
        }

        @Test
        @DisplayName("Deve lancar excecao ao tentar alterar para codigo duplicado")
        void deveLancarExcecaoAoTentarAlterarParaCodigoDuplicado() {
            // Arrange
            UUID localId = UUID.randomUUID();
            LocalArmazenamento local = criarLocalValido(TipoLocal.DEPOSITO);
            local.setId(localId);
            local.setCodigo("DEP-01");

            UpdateLocalArmazenamentoDTO dto = new UpdateLocalArmazenamentoDTO(
                    "DEP-02", TipoLocal.DEPOSITO, "Deposito", null, null, null
            );

            when(localRepository.findByOficinaIdAndId(OFICINA_ID, localId))
                    .thenReturn(Optional.of(local));
            when(localRepository.existsByOficinaIdAndCodigoAndIdNot(OFICINA_ID, "DEP-02", localId))
                    .thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> localService.atualizar(localId, dto))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Deve lancar excecao ao criar ciclo hierarquico")
        void deveLancarExcecaoAoCriarCicloHierarquico() {
            // Arrange
            UUID localId = UUID.randomUUID();
            UUID novoPaiId = UUID.randomUUID();
            LocalArmazenamento local = criarLocalValido(TipoLocal.DEPOSITO);
            local.setId(localId);
            local.setCodigo("DEP-01");
            local.setLocalizacaoPai(null);

            LocalArmazenamento novoPai = criarLocalValido(TipoLocal.ARMARIO);
            novoPai.setId(novoPaiId);

            UpdateLocalArmazenamentoDTO dto = new UpdateLocalArmazenamentoDTO(
                    "DEP-01", TipoLocal.DEPOSITO, "Deposito", novoPaiId, null, null
            );

            when(localRepository.findByOficinaIdAndId(OFICINA_ID, localId))
                    .thenReturn(Optional.of(local));
            when(localRepository.findByOficinaIdAndId(OFICINA_ID, novoPaiId))
                    .thenReturn(Optional.of(novoPai));
            when(localRepository.verificaCicloHierarquia(OFICINA_ID, localId, novoPaiId))
                    .thenReturn(true); // Criaria ciclo

            doNothing().when(localMapper).updateEntityFromDTO(dto, local);

            // Act & Assert
            assertThatThrownBy(() -> localService.atualizar(localId, dto))
                    .isInstanceOf(CicloHierarquicoException.class);
        }
    }

    // ==================== DESATIVAR / REATIVAR ====================

    @Nested
    @DisplayName("desativar/reativar")
    class DesativarReativar {

        @Test
        @DisplayName("Deve desativar local com sucesso")
        void deveDesativarLocalComSucesso() {
            // Arrange
            UUID localId = UUID.randomUUID();
            LocalArmazenamento local = criarLocalValido(TipoLocal.DEPOSITO);
            local.setId(localId);
            local.setAtivo(true);

            when(localRepository.findByOficinaIdAndId(OFICINA_ID, localId))
                    .thenReturn(Optional.of(local));
            when(localRepository.save(any(LocalArmazenamento.class))).thenAnswer(inv -> inv.getArgument(0));

            // Act
            localService.desativar(localId);

            // Assert
            verify(localRepository).save(argThat(l -> !l.getAtivo()));
        }

        @Test
        @DisplayName("Deve reativar local com sucesso")
        void deveReativarLocalComSucesso() {
            // Arrange
            UUID localId = UUID.randomUUID();
            LocalArmazenamento local = criarLocalValido(TipoLocal.DEPOSITO);
            local.setId(localId);
            local.setAtivo(false);

            when(localRepository.findByOficinaIdAndId(OFICINA_ID, localId))
                    .thenReturn(Optional.of(local));
            when(localRepository.save(any(LocalArmazenamento.class))).thenAnswer(inv -> inv.getArgument(0));

            // Act
            localService.reativar(localId);

            // Assert
            verify(localRepository).save(argThat(LocalArmazenamento::getAtivo));
        }
    }

    // ==================== EXCLUIR ====================

    @Nested
    @DisplayName("excluir()")
    class Excluir {

        @Test
        @DisplayName("Deve excluir local sem pecas vinculadas")
        void deveExcluirLocalSemPecasVinculadas() {
            // Arrange
            UUID localId = UUID.randomUUID();
            LocalArmazenamento local = criarLocalValido(TipoLocal.PRATELEIRA);
            local.setId(localId);

            when(localRepository.findByOficinaIdAndId(OFICINA_ID, localId))
                    .thenReturn(Optional.of(local));
            when(localRepository.countPecasVinculadas(OFICINA_ID, localId)).thenReturn(0L);
            doNothing().when(localRepository).delete(local);

            // Act
            localService.excluir(localId);

            // Assert
            verify(localRepository).delete(local);
        }

        @Test
        @DisplayName("Deve lancar excecao ao excluir local com pecas vinculadas")
        void deveLancarExcecaoAoExcluirLocalComPecasVinculadas() {
            // Arrange
            UUID localId = UUID.randomUUID();
            LocalArmazenamento local = criarLocalValido(TipoLocal.PRATELEIRA);
            local.setId(localId);

            when(localRepository.findByOficinaIdAndId(OFICINA_ID, localId))
                    .thenReturn(Optional.of(local));
            when(localRepository.countPecasVinculadas(OFICINA_ID, localId)).thenReturn(5L);

            // Act & Assert
            assertThatThrownBy(() -> localService.excluir(localId))
                    .isInstanceOf(LocalComPecasVinculadasException.class);

            verify(localRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Deve lancar excecao ao excluir local inexistente")
        void deveLancarExcecaoAoExcluirLocalInexistente() {
            // Arrange
            UUID localId = UUID.randomUUID();
            when(localRepository.findByOficinaIdAndId(OFICINA_ID, localId))
                    .thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> localService.excluir(localId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ==================== HELPER METHODS ====================

    private LocalArmazenamento criarLocalValido(TipoLocal tipo) {
        return LocalArmazenamento.builder()
                .codigo("DEP-01")
                .tipo(tipo)
                .descricao("Local de Teste")
                .ativo(true)
                .build();
    }
}
