package com.pitstop.cliente.service;

import com.pitstop.cliente.domain.Cliente;
import com.pitstop.cliente.domain.Endereco;
import com.pitstop.cliente.domain.TipoCliente;
import com.pitstop.cliente.dto.ClienteMapper;
import com.pitstop.cliente.dto.ClienteResponse;
import com.pitstop.cliente.dto.CreateClienteRequest;
import com.pitstop.cliente.dto.UpdateClienteRequest;
import com.pitstop.cliente.exception.ClienteNotFoundException;
import com.pitstop.cliente.exception.ClienteValidationException;
import com.pitstop.cliente.exception.CpfCnpjAlreadyExistsException;
import com.pitstop.cliente.repository.ClienteRepository;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Testes unitarios para ClienteService.
 * Usa Mockito para mockar dependencias.
 *
 * @author PitStop Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ClienteService - Testes Unitarios")
class ClienteServiceTest {

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private ClienteMapper clienteMapper;

    @InjectMocks
    private ClienteService clienteService;

    private static MockedStatic<TenantContext> tenantContextMock;
    private static final UUID OFICINA_ID = UUID.randomUUID();

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
    @DisplayName("create() - Criacao de Cliente")
    class CriarClienteTests {

        @Test
        @DisplayName("Deve criar cliente pessoa fisica com sucesso")
        void deveCriarClientePessoaFisicaComSucesso() {
            // Arrange
            CreateClienteRequest request = createValidPessoaFisicaRequest();
            Cliente cliente = createClienteEntity(TipoCliente.PESSOA_FISICA);
            ClienteResponse expectedResponse = createClienteResponse(TipoCliente.PESSOA_FISICA);

            when(clienteRepository.existsByOficinaIdAndCpfCnpj(OFICINA_ID, request.getCpfCnpj())).thenReturn(false);
            when(clienteMapper.toEntity(request)).thenReturn(cliente);
            when(clienteRepository.save(any(Cliente.class))).thenReturn(cliente);
            when(clienteMapper.toResponse(cliente)).thenReturn(expectedResponse);

            // Act
            ClienteResponse response = clienteService.create(request);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getNome()).isEqualTo("Joao da Silva");
            assertThat(response.getTipo()).isEqualTo(TipoCliente.PESSOA_FISICA);
            assertThat(response.getAtivo()).isTrue();

            verify(clienteRepository).existsByOficinaIdAndCpfCnpj(OFICINA_ID, request.getCpfCnpj());
            verify(clienteRepository).save(any(Cliente.class));
            verify(clienteMapper).toResponse(cliente);
        }

        @Test
        @DisplayName("Deve criar cliente pessoa juridica com sucesso")
        void deveCriarClientePessoaJuridicaComSucesso() {
            // Arrange
            CreateClienteRequest request = createValidPessoaJuridicaRequest();
            Cliente cliente = createClienteEntity(TipoCliente.PESSOA_JURIDICA);
            ClienteResponse expectedResponse = createClienteResponse(TipoCliente.PESSOA_JURIDICA);

            when(clienteRepository.existsByOficinaIdAndCpfCnpj(OFICINA_ID, request.getCpfCnpj())).thenReturn(false);
            when(clienteMapper.toEntity(request)).thenReturn(cliente);
            when(clienteRepository.save(any(Cliente.class))).thenReturn(cliente);
            when(clienteMapper.toResponse(cliente)).thenReturn(expectedResponse);

            // Act
            ClienteResponse response = clienteService.create(request);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getTipo()).isEqualTo(TipoCliente.PESSOA_JURIDICA);

            verify(clienteRepository).save(any(Cliente.class));
        }

        @Test
        @DisplayName("Deve lancar excecao ao criar cliente com CPF/CNPJ duplicado")
        void deveLancarExcecaoAoCriarClienteComCpfCnpjDuplicado() {
            // Arrange
            CreateClienteRequest request = createValidPessoaFisicaRequest();
            when(clienteRepository.existsByOficinaIdAndCpfCnpj(OFICINA_ID, request.getCpfCnpj())).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> clienteService.create(request))
                .isInstanceOf(CpfCnpjAlreadyExistsException.class)
                .hasMessageContaining(request.getCpfCnpj());

            verify(clienteRepository).existsByOficinaIdAndCpfCnpj(OFICINA_ID, request.getCpfCnpj());
            verify(clienteRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lancar excecao ao criar PF com CNPJ")
        void deveLancarExcecaoAoCriarPfComCnpj() {
            // Arrange
            CreateClienteRequest request = CreateClienteRequest.builder()
                .tipo(TipoCliente.PESSOA_FISICA)
                .nome("Joao da Silva")
                .cpfCnpj("11.222.333/0001-81") // CNPJ valido
                .celular("(11) 99999-0000")
                .build();

            when(clienteRepository.existsByOficinaIdAndCpfCnpj(OFICINA_ID, request.getCpfCnpj())).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> clienteService.create(request))
                .isInstanceOf(ClienteValidationException.class)
                .hasMessageContaining("Pessoa Física deve informar CPF");

            verify(clienteRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lancar excecao ao criar PJ com CPF")
        void deveLancarExcecaoAoCriarPjComCpf() {
            // Arrange
            CreateClienteRequest request = CreateClienteRequest.builder()
                .tipo(TipoCliente.PESSOA_JURIDICA)
                .nome("Empresa LTDA")
                .cpfCnpj("529.982.247-25") // CPF valido
                .celular("(11) 99999-0000")
                .build();

            when(clienteRepository.existsByOficinaIdAndCpfCnpj(OFICINA_ID, request.getCpfCnpj())).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> clienteService.create(request))
                .isInstanceOf(ClienteValidationException.class)
                .hasMessageContaining("Pessoa Jurídica deve informar CNPJ");

            verify(clienteRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lancar excecao com CPF invalido (digitos verificadores)")
        void deveLancarExcecaoComCpfInvalido() {
            // Arrange
            CreateClienteRequest request = CreateClienteRequest.builder()
                .tipo(TipoCliente.PESSOA_FISICA)
                .nome("Joao da Silva")
                .cpfCnpj("111.111.111-11") // CPF com digitos repetidos (invalido)
                .celular("(11) 99999-0000")
                .build();

            when(clienteRepository.existsByOficinaIdAndCpfCnpj(OFICINA_ID, request.getCpfCnpj())).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> clienteService.create(request))
                .isInstanceOf(ClienteValidationException.class)
                .hasMessageContaining("CPF/CNPJ inválido");

            verify(clienteRepository, never()).save(any());
        }
    }

    // ===================== FIND BY ID =====================

    @Nested
    @DisplayName("findById() - Busca por ID")
    class BuscarPorIdTests {

        @Test
        @DisplayName("Deve buscar cliente por ID com sucesso")
        void deveBuscarClientePorIdComSucesso() {
            // Arrange
            UUID id = UUID.randomUUID();
            Cliente cliente = createClienteEntity(TipoCliente.PESSOA_FISICA);
            cliente.setId(id);
            ClienteResponse expectedResponse = createClienteResponse(TipoCliente.PESSOA_FISICA);
            expectedResponse.setId(id);

            when(clienteRepository.findByOficinaIdAndIdIncludingInactive(OFICINA_ID, id))
                .thenReturn(Optional.of(cliente));
            when(clienteMapper.toResponse(cliente)).thenReturn(expectedResponse);

            // Act
            ClienteResponse response = clienteService.findById(id);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(id);

            verify(clienteRepository).findByOficinaIdAndIdIncludingInactive(OFICINA_ID, id);
        }

        @Test
        @DisplayName("Deve lancar excecao ao buscar cliente inexistente")
        void deveLancarExcecaoAoBuscarClienteInexistente() {
            // Arrange
            UUID id = UUID.randomUUID();
            when(clienteRepository.findByOficinaIdAndIdIncludingInactive(OFICINA_ID, id))
                .thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> clienteService.findById(id))
                .isInstanceOf(ClienteNotFoundException.class);

            verify(clienteRepository).findByOficinaIdAndIdIncludingInactive(OFICINA_ID, id);
        }
    }

    // ===================== FIND BY CPF/CNPJ =====================

    @Nested
    @DisplayName("findByCpfCnpj() - Busca por CPF/CNPJ")
    class BuscarPorCpfCnpjTests {

        @Test
        @DisplayName("Deve buscar cliente por CPF com sucesso")
        void deveBuscarClientePorCpfComSucesso() {
            // Arrange
            String cpf = "529.982.247-25";
            Cliente cliente = createClienteEntity(TipoCliente.PESSOA_FISICA);
            ClienteResponse expectedResponse = createClienteResponse(TipoCliente.PESSOA_FISICA);

            when(clienteRepository.findByOficinaIdAndCpfCnpj(OFICINA_ID, cpf))
                .thenReturn(Optional.of(cliente));
            when(clienteMapper.toResponse(cliente)).thenReturn(expectedResponse);

            // Act
            ClienteResponse response = clienteService.findByCpfCnpj(cpf);

            // Assert
            assertThat(response).isNotNull();
            verify(clienteRepository).findByOficinaIdAndCpfCnpj(OFICINA_ID, cpf);
        }

        @Test
        @DisplayName("Deve lancar excecao ao buscar por CPF inexistente")
        void deveLancarExcecaoAoBuscarPorCpfInexistente() {
            // Arrange
            String cpf = "000.000.000-00";
            when(clienteRepository.findByOficinaIdAndCpfCnpj(OFICINA_ID, cpf))
                .thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> clienteService.findByCpfCnpj(cpf))
                .isInstanceOf(ClienteNotFoundException.class);
        }
    }

    // ===================== FIND ALL =====================

    @Nested
    @DisplayName("findAll() - Listagem com Paginacao")
    class ListarClientesTests {

        @Test
        @DisplayName("Deve listar clientes com paginacao")
        void deveListarClientesComPaginacao() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            List<Cliente> clientes = List.of(
                createClienteEntity(TipoCliente.PESSOA_FISICA),
                createClienteEntity(TipoCliente.PESSOA_JURIDICA)
            );
            Page<Cliente> clientesPage = new PageImpl<>(clientes, pageable, 2);
            ClienteResponse response1 = createClienteResponse(TipoCliente.PESSOA_FISICA);
            ClienteResponse response2 = createClienteResponse(TipoCliente.PESSOA_JURIDICA);

            when(clienteRepository.findByOficinaId(OFICINA_ID, pageable)).thenReturn(clientesPage);
            when(clienteMapper.toResponse(clientes.get(0))).thenReturn(response1);
            when(clienteMapper.toResponse(clientes.get(1))).thenReturn(response2);

            // Act
            Page<ClienteResponse> result = clienteService.findAll(pageable);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getTotalElements()).isEqualTo(2);
            assertThat(result.getContent()).hasSize(2);

            verify(clienteRepository).findByOficinaId(OFICINA_ID, pageable);
        }

        @Test
        @DisplayName("Deve retornar pagina vazia quando nao ha clientes")
        void deveRetornarPaginaVaziaQuandoNaoHaClientes() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            Page<Cliente> emptyPage = new PageImpl<>(List.of(), pageable, 0);

            when(clienteRepository.findByOficinaId(OFICINA_ID, pageable)).thenReturn(emptyPage);

            // Act
            Page<ClienteResponse> result = clienteService.findAll(pageable);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getTotalElements()).isZero();
            assertThat(result.getContent()).isEmpty();
        }
    }

    // ===================== FIND BY TIPO =====================

    @Nested
    @DisplayName("findByTipo() - Busca por Tipo")
    class BuscarPorTipoTests {

        @Test
        @DisplayName("Deve buscar apenas pessoas fisicas")
        void deveBuscarApenasPessoasFisicas() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            Cliente cliente = createClienteEntity(TipoCliente.PESSOA_FISICA);
            Page<Cliente> page = new PageImpl<>(List.of(cliente), pageable, 1);
            ClienteResponse response = createClienteResponse(TipoCliente.PESSOA_FISICA);

            when(clienteRepository.findByOficinaIdAndTipo(OFICINA_ID, TipoCliente.PESSOA_FISICA, pageable))
                .thenReturn(page);
            when(clienteMapper.toResponse(cliente)).thenReturn(response);

            // Act
            Page<ClienteResponse> result = clienteService.findByTipo(TipoCliente.PESSOA_FISICA, pageable);

            // Assert
            assertThat(result.getContent()).allMatch(c -> c.getTipo() == TipoCliente.PESSOA_FISICA);
        }
    }

    // ===================== FIND BY NOME =====================

    @Nested
    @DisplayName("findByNome() - Busca por Nome")
    class BuscarPorNomeTests {

        @Test
        @DisplayName("Deve buscar clientes por nome parcial")
        void deveBuscarClientesPorNomeParcial() {
            // Arrange
            String nome = "Silva";
            Pageable pageable = PageRequest.of(0, 10);
            Cliente cliente = createClienteEntity(TipoCliente.PESSOA_FISICA);
            Page<Cliente> page = new PageImpl<>(List.of(cliente), pageable, 1);
            ClienteResponse response = createClienteResponse(TipoCliente.PESSOA_FISICA);

            when(clienteRepository.findByOficinaIdAndNomeContainingIgnoreCase(OFICINA_ID, nome, pageable))
                .thenReturn(page);
            when(clienteMapper.toResponse(cliente)).thenReturn(response);

            // Act
            Page<ClienteResponse> result = clienteService.findByNome(nome, pageable);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);

            verify(clienteRepository).findByOficinaIdAndNomeContainingIgnoreCase(OFICINA_ID, nome, pageable);
        }
    }

    // ===================== UPDATE =====================

    @Nested
    @DisplayName("update() - Atualizacao de Cliente")
    class AtualizarClienteTests {

        @Test
        @DisplayName("Deve atualizar cliente com sucesso")
        void deveAtualizarClienteComSucesso() {
            // Arrange
            UUID id = UUID.randomUUID();
            UpdateClienteRequest request = UpdateClienteRequest.builder()
                .nome("Joao da Silva Atualizado")
                .email("joao.novo@email.com")
                .celular("(11) 98888-0000")
                .build();
            Cliente cliente = createClienteEntity(TipoCliente.PESSOA_FISICA);
            cliente.setId(id);
            ClienteResponse expectedResponse = createClienteResponse(TipoCliente.PESSOA_FISICA);
            expectedResponse.setNome("Joao da Silva Atualizado");

            when(clienteRepository.findByOficinaIdAndId(OFICINA_ID, id)).thenReturn(Optional.of(cliente));
            when(clienteRepository.save(any(Cliente.class))).thenReturn(cliente);
            when(clienteMapper.toResponse(cliente)).thenReturn(expectedResponse);

            // Act
            ClienteResponse response = clienteService.update(id, request);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getNome()).isEqualTo("Joao da Silva Atualizado");

            verify(clienteRepository).findByOficinaIdAndId(OFICINA_ID, id);
            verify(clienteMapper).updateEntityFromDto(request, cliente);
            verify(clienteRepository).save(cliente);
        }

        @Test
        @DisplayName("Deve lancar excecao ao atualizar cliente inexistente")
        void deveLancarExcecaoAoAtualizarClienteInexistente() {
            // Arrange
            UUID id = UUID.randomUUID();
            UpdateClienteRequest request = UpdateClienteRequest.builder()
                .nome("Nome Atualizado")
                .celular("(11) 99999-0000")
                .build();

            when(clienteRepository.findByOficinaIdAndId(OFICINA_ID, id)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> clienteService.update(id, request))
                .isInstanceOf(ClienteNotFoundException.class);

            verify(clienteRepository, never()).save(any());
        }
    }

    // ===================== DELETE (SOFT DELETE) =====================

    @Nested
    @DisplayName("delete() - Soft Delete de Cliente")
    class DeletarClienteTests {

        @Test
        @DisplayName("Deve desativar cliente (soft delete) com sucesso")
        void deveDesativarClienteComSucesso() {
            // Arrange
            UUID id = UUID.randomUUID();
            Cliente cliente = createClienteEntity(TipoCliente.PESSOA_FISICA);
            cliente.setId(id);
            cliente.setAtivo(true);

            when(clienteRepository.findByOficinaIdAndId(OFICINA_ID, id)).thenReturn(Optional.of(cliente));
            when(clienteRepository.save(any(Cliente.class))).thenReturn(cliente);

            // Act
            clienteService.delete(id);

            // Assert
            assertThat(cliente.getAtivo()).isFalse();
            verify(clienteRepository).findByOficinaIdAndId(OFICINA_ID, id);
            verify(clienteRepository).save(cliente);
        }

        @Test
        @DisplayName("Deve lancar excecao ao deletar cliente inexistente")
        void deveLancarExcecaoAoDeletarClienteInexistente() {
            // Arrange
            UUID id = UUID.randomUUID();
            when(clienteRepository.findByOficinaIdAndId(OFICINA_ID, id)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> clienteService.delete(id))
                .isInstanceOf(ClienteNotFoundException.class);

            verify(clienteRepository, never()).save(any());
        }
    }

    // ===================== REATIVAR =====================

    @Nested
    @DisplayName("reativar() - Reativacao de Cliente")
    class ReativarClienteTests {

        @Test
        @DisplayName("Deve reativar cliente desativado com sucesso")
        void deveReativarClienteDesativadoComSucesso() {
            // Arrange
            UUID id = UUID.randomUUID();
            Cliente cliente = createClienteEntity(TipoCliente.PESSOA_FISICA);
            cliente.setId(id);
            cliente.setAtivo(false);
            ClienteResponse expectedResponse = createClienteResponse(TipoCliente.PESSOA_FISICA);
            expectedResponse.setAtivo(true);

            when(clienteRepository.findByOficinaIdAndIdIncludingInactive(OFICINA_ID, id))
                .thenReturn(Optional.of(cliente));
            when(clienteRepository.save(any(Cliente.class))).thenReturn(cliente);
            when(clienteMapper.toResponse(cliente)).thenReturn(expectedResponse);

            // Act
            ClienteResponse response = clienteService.reativar(id);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getAtivo()).isTrue();
            assertThat(cliente.getAtivo()).isTrue();

            verify(clienteRepository).findByOficinaIdAndIdIncludingInactive(OFICINA_ID, id);
            verify(clienteRepository).save(cliente);
        }

        @Test
        @DisplayName("Deve lancar excecao ao reativar cliente inexistente")
        void deveLancarExcecaoAoReativarClienteInexistente() {
            // Arrange
            UUID id = UUID.randomUUID();
            when(clienteRepository.findByOficinaIdAndIdIncludingInactive(OFICINA_ID, id))
                .thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> clienteService.reativar(id))
                .isInstanceOf(ClienteNotFoundException.class);

            verify(clienteRepository, never()).save(any());
        }
    }

    // ===================== COUNT BY TIPO =====================

    @Nested
    @DisplayName("countByTipo() - Contagem por Tipo")
    class ContarPorTipoTests {

        @Test
        @DisplayName("Deve contar clientes por tipo corretamente")
        void deveContarClientesPorTipoCorretamente() {
            // Arrange
            when(clienteRepository.countByOficinaIdAndTipo(OFICINA_ID, TipoCliente.PESSOA_FISICA))
                .thenReturn(15L);
            when(clienteRepository.countByOficinaIdAndTipo(OFICINA_ID, TipoCliente.PESSOA_JURIDICA))
                .thenReturn(5L);

            // Act
            long countPF = clienteService.countByTipo(TipoCliente.PESSOA_FISICA);
            long countPJ = clienteService.countByTipo(TipoCliente.PESSOA_JURIDICA);

            // Assert
            assertThat(countPF).isEqualTo(15L);
            assertThat(countPJ).isEqualTo(5L);
        }
    }

    // ===================== METODOS AUXILIARES =====================

    private CreateClienteRequest createValidPessoaFisicaRequest() {
        return CreateClienteRequest.builder()
            .tipo(TipoCliente.PESSOA_FISICA)
            .nome("Joao da Silva")
            .cpfCnpj("529.982.247-25") // CPF valido
            .email("joao@email.com")
            .celular("(11) 99999-0000")
            .logradouro("Rua das Flores")
            .numero("123")
            .bairro("Centro")
            .cidade("Sao Paulo")
            .estado("SP")
            .cep("01310-100")
            .build();
    }

    private CreateClienteRequest createValidPessoaJuridicaRequest() {
        return CreateClienteRequest.builder()
            .tipo(TipoCliente.PESSOA_JURIDICA)
            .nome("Empresa Exemplo LTDA")
            .cpfCnpj("11.222.333/0001-81") // CNPJ valido
            .email("contato@empresa.com")
            .telefone("(11) 3333-4444")
            .celular("(11) 99999-0000")
            .logradouro("Avenida Paulista")
            .numero("1000")
            .complemento("Sala 501")
            .bairro("Bela Vista")
            .cidade("Sao Paulo")
            .estado("SP")
            .cep("01310-100")
            .build();
    }

    private Cliente createClienteEntity(TipoCliente tipo) {
        Endereco endereco = new Endereco();
        endereco.setLogradouro("Rua das Flores");
        endereco.setNumero("123");
        endereco.setBairro("Centro");
        endereco.setCidade("Sao Paulo");
        endereco.setEstado("SP");
        endereco.setCep("01310-100");

        return Cliente.builder()
            .id(UUID.randomUUID())
            .tipo(tipo)
            .nome(tipo == TipoCliente.PESSOA_FISICA ? "Joao da Silva" : "Empresa Exemplo LTDA")
            .cpfCnpj(tipo == TipoCliente.PESSOA_FISICA ? "529.982.247-25" : "11.222.333/0001-81")
            .email("contato@email.com")
            .telefone("(11) 3333-4444")
            .celular("(11) 99999-0000")
            .endereco(endereco)
            .ativo(true)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
    }

    private ClienteResponse createClienteResponse(TipoCliente tipo) {
        ClienteResponse.EnderecoResponse enderecoResponse = ClienteResponse.EnderecoResponse.builder()
            .logradouro("Rua das Flores")
            .numero("123")
            .bairro("Centro")
            .cidade("Sao Paulo")
            .estado("SP")
            .cep("01310-100")
            .build();

        return ClienteResponse.builder()
            .id(UUID.randomUUID())
            .tipo(tipo)
            .nome(tipo == TipoCliente.PESSOA_FISICA ? "Joao da Silva" : "Empresa Exemplo LTDA")
            .cpfCnpj(tipo == TipoCliente.PESSOA_FISICA ? "529.982.247-25" : "11.222.333/0001-81")
            .email("contato@email.com")
            .telefone("(11) 3333-4444")
            .celular("(11) 99999-0000")
            .endereco(enderecoResponse)
            .ativo(true)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
    }
}
