package com.pitstop.veiculo.service;

import com.pitstop.cliente.domain.Cliente;
import com.pitstop.cliente.domain.TipoCliente;
import com.pitstop.cliente.exception.ClienteNotFoundException;
import com.pitstop.cliente.repository.ClienteRepository;
import com.pitstop.shared.security.tenant.TenantContext;
import com.pitstop.veiculo.domain.Veiculo;
import com.pitstop.veiculo.dto.*;
import com.pitstop.veiculo.exception.PlacaJaExisteException;
import com.pitstop.veiculo.exception.VeiculoNotFoundException;
import com.pitstop.veiculo.mapper.VeiculoMapper;
import com.pitstop.veiculo.repository.VeiculoRepository;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Testes unitarios para VeiculoService.
 * Usa Mockito para mockar dependencias.
 *
 * @author PitStop Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("VeiculoService - Testes Unitarios")
class VeiculoServiceTest {

    @Mock
    private VeiculoRepository veiculoRepository;

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private VeiculoMapper veiculoMapper;

    @InjectMocks
    private VeiculoService veiculoService;

    private static MockedStatic<TenantContext> tenantContextMock;
    private static final UUID OFICINA_ID = UUID.randomUUID();
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

    // ===================== CREATE =====================

    @Nested
    @DisplayName("create() - Criacao de Veiculo")
    class CriarVeiculoTests {

        @Test
        @DisplayName("Deve criar veiculo com sucesso")
        void deveCriarVeiculoComSucesso() {
            // Arrange
            VeiculoRequestDTO request = createValidVeiculoRequest();
            Cliente cliente = createClienteEntity();
            Veiculo veiculo = createVeiculoEntity();
            VeiculoResponseDTO expectedResponse = createVeiculoResponse();

            when(clienteRepository.findByOficinaIdAndId(OFICINA_ID, CLIENTE_ID))
                .thenReturn(Optional.of(cliente));
            when(veiculoRepository.existsByOficinaIdAndPlaca(eq(OFICINA_ID), anyString()))
                .thenReturn(false);
            when(veiculoMapper.toEntity(request)).thenReturn(veiculo);
            when(veiculoRepository.save(any(Veiculo.class))).thenReturn(veiculo);
            when(veiculoMapper.toResponse(veiculo)).thenReturn(expectedResponse);

            // Act
            VeiculoResponseDTO response = veiculoService.create(request);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getPlaca()).isEqualTo("ABC1234");
            assertThat(response.getMarca()).isEqualTo("Volkswagen");
            assertThat(response.getModelo()).isEqualTo("Gol");

            verify(clienteRepository).findByOficinaIdAndId(OFICINA_ID, CLIENTE_ID);
            verify(veiculoRepository).existsByOficinaIdAndPlaca(eq(OFICINA_ID), anyString());
            verify(veiculoRepository).save(any(Veiculo.class));
        }

        @Test
        @DisplayName("Deve lancar excecao ao criar veiculo com placa duplicada")
        void deveLancarExcecaoAoCriarVeiculoComPlacaDuplicada() {
            // Arrange
            VeiculoRequestDTO request = createValidVeiculoRequest();
            Cliente cliente = createClienteEntity();

            when(clienteRepository.findByOficinaIdAndId(OFICINA_ID, CLIENTE_ID))
                .thenReturn(Optional.of(cliente));
            when(veiculoRepository.existsByOficinaIdAndPlaca(eq(OFICINA_ID), anyString()))
                .thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> veiculoService.create(request))
                .isInstanceOf(PlacaJaExisteException.class)
                .hasMessageContaining(request.getPlaca());

            verify(veiculoRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lancar excecao ao criar veiculo com cliente inexistente")
        void deveLancarExcecaoAoCriarVeiculoComClienteInexistente() {
            // Arrange
            VeiculoRequestDTO request = createValidVeiculoRequest();

            when(clienteRepository.findByOficinaIdAndId(OFICINA_ID, CLIENTE_ID))
                .thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> veiculoService.create(request))
                .isInstanceOf(ClienteNotFoundException.class);

            verify(veiculoRepository, never()).existsByOficinaIdAndPlaca(any(), anyString());
            verify(veiculoRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve normalizar placa com hifen ao criar")
        void deveNormalizarPlacaComHifenAoCriar() {
            // Arrange
            VeiculoRequestDTO request = VeiculoRequestDTO.builder()
                .clienteId(CLIENTE_ID)
                .placa("ABC-1234") // Com hifen
                .marca("Volkswagen")
                .modelo("Gol")
                .ano(2020)
                .build();
            Cliente cliente = createClienteEntity();
            Veiculo veiculo = createVeiculoEntity();
            VeiculoResponseDTO expectedResponse = createVeiculoResponse();

            when(clienteRepository.findByOficinaIdAndId(OFICINA_ID, CLIENTE_ID))
                .thenReturn(Optional.of(cliente));
            when(veiculoRepository.existsByOficinaIdAndPlaca(OFICINA_ID, "ABC1234")) // Sem hifen
                .thenReturn(false);
            when(veiculoMapper.toEntity(request)).thenReturn(veiculo);
            when(veiculoRepository.save(any(Veiculo.class))).thenReturn(veiculo);
            when(veiculoMapper.toResponse(veiculo)).thenReturn(expectedResponse);

            // Act
            veiculoService.create(request);

            // Assert
            verify(veiculoRepository).existsByOficinaIdAndPlaca(OFICINA_ID, "ABC1234");
        }
    }

    // ===================== FIND BY ID =====================

    @Nested
    @DisplayName("findById() - Busca por ID")
    class BuscarPorIdTests {

        @Test
        @DisplayName("Deve buscar veiculo por ID com sucesso")
        void deveBuscarVeiculoPorIdComSucesso() {
            // Arrange
            UUID id = UUID.randomUUID();
            Veiculo veiculo = createVeiculoEntity();
            veiculo.setId(id);
            VeiculoResponseDTO expectedResponse = createVeiculoResponse();
            expectedResponse.setId(id);

            when(veiculoRepository.findByOficinaIdAndId(OFICINA_ID, id))
                .thenReturn(Optional.of(veiculo));
            when(veiculoMapper.toResponse(veiculo)).thenReturn(expectedResponse);

            // Act
            VeiculoResponseDTO response = veiculoService.findById(id);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(id);

            verify(veiculoRepository).findByOficinaIdAndId(OFICINA_ID, id);
        }

        @Test
        @DisplayName("Deve lancar excecao ao buscar veiculo inexistente")
        void deveLancarExcecaoAoBuscarVeiculoInexistente() {
            // Arrange
            UUID id = UUID.randomUUID();
            when(veiculoRepository.findByOficinaIdAndId(OFICINA_ID, id))
                .thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> veiculoService.findById(id))
                .isInstanceOf(VeiculoNotFoundException.class);
        }
    }

    // ===================== FIND BY PLACA =====================

    @Nested
    @DisplayName("findByPlaca() - Busca por Placa")
    class BuscarPorPlacaTests {

        @Test
        @DisplayName("Deve buscar veiculo por placa com sucesso")
        void deveBuscarVeiculoPorPlacaComSucesso() {
            // Arrange
            String placa = "ABC1234";
            Veiculo veiculo = createVeiculoEntity();
            VeiculoResponseDTO expectedResponse = createVeiculoResponse();

            when(veiculoRepository.findByOficinaIdAndPlaca(OFICINA_ID, placa))
                .thenReturn(Optional.of(veiculo));
            when(veiculoMapper.toResponse(veiculo)).thenReturn(expectedResponse);

            // Act
            VeiculoResponseDTO response = veiculoService.findByPlaca(placa);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getPlaca()).isEqualTo(placa);
        }

        @Test
        @DisplayName("Deve normalizar placa com hifen ao buscar")
        void deveNormalizarPlacaComHifenAoBuscar() {
            // Arrange
            String placaComHifen = "ABC-1234";
            String placaSemHifen = "ABC1234";
            Veiculo veiculo = createVeiculoEntity();
            VeiculoResponseDTO expectedResponse = createVeiculoResponse();

            when(veiculoRepository.findByOficinaIdAndPlaca(OFICINA_ID, placaSemHifen))
                .thenReturn(Optional.of(veiculo));
            when(veiculoMapper.toResponse(veiculo)).thenReturn(expectedResponse);

            // Act
            veiculoService.findByPlaca(placaComHifen);

            // Assert
            verify(veiculoRepository).findByOficinaIdAndPlaca(OFICINA_ID, placaSemHifen);
        }

        @Test
        @DisplayName("Deve lancar excecao ao buscar placa inexistente")
        void deveLancarExcecaoAoBuscarPlacaInexistente() {
            // Arrange
            String placa = "XXX0000";
            when(veiculoRepository.findByOficinaIdAndPlaca(OFICINA_ID, placa))
                .thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> veiculoService.findByPlaca(placa))
                .isInstanceOf(VeiculoNotFoundException.class);
        }
    }

    // ===================== FIND ALL =====================

    @Nested
    @DisplayName("findAll() - Listagem com Paginacao")
    class ListarVeiculosTests {

        @Test
        @DisplayName("Deve listar veiculos com paginacao")
        void deveListarVeiculosComPaginacao() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            Veiculo veiculo1 = createVeiculoEntity();
            Veiculo veiculo2 = createVeiculoEntity();
            veiculo2.setPlaca("XYZ5678");
            List<Veiculo> veiculos = List.of(veiculo1, veiculo2);
            Page<Veiculo> veiculosPage = new PageImpl<>(veiculos, pageable, 2);
            VeiculoResponseDTO response1 = createVeiculoResponse();
            VeiculoResponseDTO response2 = createVeiculoResponse();
            response2.setPlaca("XYZ5678");

            when(veiculoRepository.findByOficinaId(OFICINA_ID, pageable)).thenReturn(veiculosPage);
            when(veiculoMapper.toResponse(veiculo1)).thenReturn(response1);
            when(veiculoMapper.toResponse(veiculo2)).thenReturn(response2);

            // Act
            Page<VeiculoResponseDTO> result = veiculoService.findAll(pageable);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getTotalElements()).isEqualTo(2);
            assertThat(result.getContent()).hasSize(2);

            verify(veiculoRepository).findByOficinaId(OFICINA_ID, pageable);
        }

        @Test
        @DisplayName("Deve retornar pagina vazia quando nao ha veiculos")
        void deveRetornarPaginaVaziaQuandoNaoHaVeiculos() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            Page<Veiculo> emptyPage = new PageImpl<>(List.of(), pageable, 0);

            when(veiculoRepository.findByOficinaId(OFICINA_ID, pageable)).thenReturn(emptyPage);

            // Act
            Page<VeiculoResponseDTO> result = veiculoService.findAll(pageable);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getTotalElements()).isZero();
            assertThat(result.getContent()).isEmpty();
        }
    }

    // ===================== FIND BY CLIENTE =====================

    @Nested
    @DisplayName("findByClienteId() - Busca por Cliente")
    class BuscarPorClienteTests {

        @Test
        @DisplayName("Deve buscar veiculos por cliente com sucesso")
        void deveBuscarVeiculosPorClienteComSucesso() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            Cliente cliente = createClienteEntity();
            Veiculo veiculo = createVeiculoEntity();
            Page<Veiculo> page = new PageImpl<>(List.of(veiculo), pageable, 1);
            VeiculoResponseDTO response = createVeiculoResponse();

            when(clienteRepository.findByOficinaIdAndId(OFICINA_ID, CLIENTE_ID))
                .thenReturn(Optional.of(cliente));
            when(veiculoRepository.findByOficinaIdAndClienteId(OFICINA_ID, CLIENTE_ID, pageable))
                .thenReturn(page);
            when(veiculoMapper.toResponse(veiculo)).thenReturn(response);

            // Act
            Page<VeiculoResponseDTO> result = veiculoService.findByClienteId(CLIENTE_ID, pageable);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getTotalElements()).isEqualTo(1);

            verify(clienteRepository).findByOficinaIdAndId(OFICINA_ID, CLIENTE_ID);
            verify(veiculoRepository).findByOficinaIdAndClienteId(OFICINA_ID, CLIENTE_ID, pageable);
        }

        @Test
        @DisplayName("Deve lancar excecao ao buscar veiculos de cliente inexistente")
        void deveLancarExcecaoAoBuscarVeiculosDeClienteInexistente() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            when(clienteRepository.findByOficinaIdAndId(OFICINA_ID, CLIENTE_ID))
                .thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> veiculoService.findByClienteId(CLIENTE_ID, pageable))
                .isInstanceOf(ClienteNotFoundException.class);

            verify(veiculoRepository, never()).findByOficinaIdAndClienteId(any(), any(), any());
        }
    }

    // ===================== UPDATE =====================

    @Nested
    @DisplayName("update() - Atualizacao de Veiculo")
    class AtualizarVeiculoTests {

        @Test
        @DisplayName("Deve atualizar veiculo com sucesso")
        void deveAtualizarVeiculoComSucesso() {
            // Arrange
            UUID id = UUID.randomUUID();
            VeiculoUpdateDTO request = VeiculoUpdateDTO.builder()
                .marca("Fiat")
                .modelo("Uno")
                .ano(2021)
                .cor("Vermelho")
                .build();
            Veiculo veiculo = createVeiculoEntity();
            veiculo.setId(id);
            VeiculoResponseDTO expectedResponse = createVeiculoResponse();
            expectedResponse.setMarca("Fiat");
            expectedResponse.setModelo("Uno");

            when(veiculoRepository.findByOficinaIdAndId(OFICINA_ID, id))
                .thenReturn(Optional.of(veiculo));
            when(veiculoRepository.save(any(Veiculo.class))).thenReturn(veiculo);
            when(veiculoMapper.toResponse(veiculo)).thenReturn(expectedResponse);

            // Act
            VeiculoResponseDTO response = veiculoService.update(id, request);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getMarca()).isEqualTo("Fiat");

            verify(veiculoRepository).findByOficinaIdAndId(OFICINA_ID, id);
            verify(veiculoMapper).updateEntityFromDto(request, veiculo);
            verify(veiculoRepository).save(veiculo);
        }

        @Test
        @DisplayName("Deve lancar excecao ao atualizar veiculo inexistente")
        void deveLancarExcecaoAoAtualizarVeiculoInexistente() {
            // Arrange
            UUID id = UUID.randomUUID();
            VeiculoUpdateDTO request = VeiculoUpdateDTO.builder()
                .marca("Fiat")
                .modelo("Uno")
                .build();

            when(veiculoRepository.findByOficinaIdAndId(OFICINA_ID, id))
                .thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> veiculoService.update(id, request))
                .isInstanceOf(VeiculoNotFoundException.class);

            verify(veiculoRepository, never()).save(any());
        }
    }

    // ===================== UPDATE QUILOMETRAGEM =====================

    @Nested
    @DisplayName("updateQuilometragem() - Atualizacao de Quilometragem")
    class AtualizarQuilometragemTests {

        @Test
        @DisplayName("Deve atualizar quilometragem com sucesso")
        void deveAtualizarQuilometragemComSucesso() {
            // Arrange
            UUID id = UUID.randomUUID();
            QuilometragemUpdateDTO request = new QuilometragemUpdateDTO();
            request.setQuilometragem(75000);
            Veiculo veiculo = createVeiculoEntity();
            veiculo.setId(id);
            veiculo.setQuilometragem(50000);
            VeiculoResponseDTO expectedResponse = createVeiculoResponse();
            expectedResponse.setQuilometragem(75000);

            when(veiculoRepository.findByOficinaIdAndId(OFICINA_ID, id))
                .thenReturn(Optional.of(veiculo));
            when(veiculoRepository.save(any(Veiculo.class))).thenReturn(veiculo);
            when(veiculoMapper.toResponse(veiculo)).thenReturn(expectedResponse);

            // Act
            VeiculoResponseDTO response = veiculoService.updateQuilometragem(id, request);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getQuilometragem()).isEqualTo(75000);

            verify(veiculoRepository).save(veiculo);
        }

        @Test
        @DisplayName("Deve lancar excecao ao atualizar km de veiculo inexistente")
        void deveLancarExcecaoAoAtualizarKmDeVeiculoInexistente() {
            // Arrange
            UUID id = UUID.randomUUID();
            QuilometragemUpdateDTO request = new QuilometragemUpdateDTO();
            request.setQuilometragem(75000);

            when(veiculoRepository.findByOficinaIdAndId(OFICINA_ID, id))
                .thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> veiculoService.updateQuilometragem(id, request))
                .isInstanceOf(VeiculoNotFoundException.class);

            verify(veiculoRepository, never()).save(any());
        }
    }

    // ===================== DELETE =====================

    @Nested
    @DisplayName("delete() - Remocao de Veiculo")
    class DeletarVeiculoTests {

        @Test
        @DisplayName("Deve deletar veiculo com sucesso")
        void deveDeletarVeiculoComSucesso() {
            // Arrange
            UUID id = UUID.randomUUID();
            Veiculo veiculo = createVeiculoEntity();
            veiculo.setId(id);

            when(veiculoRepository.findByOficinaIdAndId(OFICINA_ID, id))
                .thenReturn(Optional.of(veiculo));

            // Act
            veiculoService.delete(id);

            // Assert
            verify(veiculoRepository).findByOficinaIdAndId(OFICINA_ID, id);
            verify(veiculoRepository).delete(veiculo);
        }

        @Test
        @DisplayName("Deve lancar excecao ao deletar veiculo inexistente")
        void deveLancarExcecaoAoDeletarVeiculoInexistente() {
            // Arrange
            UUID id = UUID.randomUUID();
            when(veiculoRepository.findByOficinaIdAndId(OFICINA_ID, id))
                .thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> veiculoService.delete(id))
                .isInstanceOf(VeiculoNotFoundException.class);

            verify(veiculoRepository, never()).delete(any());
        }
    }

    // ===================== FIND MARCAS/MODELOS/ANOS =====================

    @Nested
    @DisplayName("Filtros Dinamicos - Marcas, Modelos, Anos")
    class FiltrosDinamicosTests {

        @Test
        @DisplayName("Deve retornar lista de marcas unicas")
        void deveRetornarListaDeMarcasUnicas() {
            // Arrange
            List<String> marcas = List.of("Volkswagen", "Fiat", "Chevrolet");
            when(veiculoRepository.findDistinctMarcas(OFICINA_ID)).thenReturn(marcas);

            // Act
            List<String> result = veiculoService.findMarcas();

            // Assert
            assertThat(result).hasSize(3);
            assertThat(result).containsExactly("Volkswagen", "Fiat", "Chevrolet");
        }

        @Test
        @DisplayName("Deve retornar lista de modelos unicos")
        void deveRetornarListaDeModelosUnicos() {
            // Arrange
            List<String> modelos = List.of("Gol", "Uno", "Onix");
            when(veiculoRepository.findDistinctModelos(OFICINA_ID)).thenReturn(modelos);

            // Act
            List<String> result = veiculoService.findModelos();

            // Assert
            assertThat(result).hasSize(3);
            assertThat(result).containsExactly("Gol", "Uno", "Onix");
        }

        @Test
        @DisplayName("Deve retornar lista de anos unicos")
        void deveRetornarListaDeAnosUnicos() {
            // Arrange
            List<Integer> anos = List.of(2020, 2021, 2022);
            when(veiculoRepository.findDistinctAnos(OFICINA_ID)).thenReturn(anos);

            // Act
            List<Integer> result = veiculoService.findAnos();

            // Assert
            assertThat(result).hasSize(3);
            assertThat(result).containsExactly(2020, 2021, 2022);
        }
    }

    // ===================== COUNT BY CLIENTE =====================

    @Nested
    @DisplayName("countByClienteId() - Contagem por Cliente")
    class ContarPorClienteTests {

        @Test
        @DisplayName("Deve contar veiculos por cliente corretamente")
        void deveContarVeiculosPorClienteCorretamente() {
            // Arrange
            when(veiculoRepository.countByOficinaIdAndClienteId(OFICINA_ID, CLIENTE_ID))
                .thenReturn(3L);

            // Act
            long count = veiculoService.countByClienteId(CLIENTE_ID);

            // Assert
            assertThat(count).isEqualTo(3L);
        }
    }

    // ===================== METODOS AUXILIARES =====================

    private VeiculoRequestDTO createValidVeiculoRequest() {
        return VeiculoRequestDTO.builder()
            .clienteId(CLIENTE_ID)
            .placa("ABC1234")
            .marca("Volkswagen")
            .modelo("Gol")
            .ano(2020)
            .cor("Prata")
            .quilometragem(50000)
            .build();
    }

    private Cliente createClienteEntity() {
        return Cliente.builder()
            .id(CLIENTE_ID)
            .tipo(TipoCliente.PESSOA_FISICA)
            .nome("Joao da Silva")
            .cpfCnpj("529.982.247-25")
            .email("joao@email.com")
            .celular("(11) 99999-0000")
            .ativo(true)
            .build();
    }

    private Veiculo createVeiculoEntity() {
        Veiculo veiculo = new Veiculo();
        veiculo.setId(UUID.randomUUID());
        veiculo.setClienteId(CLIENTE_ID);
        veiculo.setPlaca("ABC1234");
        veiculo.setMarca("Volkswagen");
        veiculo.setModelo("Gol");
        veiculo.setAno(2020);
        veiculo.setCor("Prata");
        veiculo.setQuilometragem(50000);
        veiculo.setCreatedAt(LocalDateTime.now());
        veiculo.setUpdatedAt(LocalDateTime.now());
        return veiculo;
    }

    private VeiculoResponseDTO createVeiculoResponse() {
        return VeiculoResponseDTO.builder()
            .id(UUID.randomUUID())
            .clienteId(CLIENTE_ID)
            .placa("ABC1234")
            .marca("Volkswagen")
            .modelo("Gol")
            .ano(2020)
            .cor("Prata")
            .quilometragem(50000)
            .descricaoCompleta("Volkswagen Gol 2020")
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
    }
}
