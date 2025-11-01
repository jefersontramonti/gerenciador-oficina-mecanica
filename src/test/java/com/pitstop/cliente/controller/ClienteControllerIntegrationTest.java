package com.pitstop.cliente.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pitstop.cliente.domain.TipoCliente;
import com.pitstop.cliente.dto.CreateClienteRequest;
import com.pitstop.cliente.dto.UpdateClienteRequest;
import com.pitstop.cliente.repository.ClienteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes de integração para o ClienteController.
 *
 * <p>Testa endpoints REST com Spring Boot Test + MockMvc.
 * Usa banco H2 em memória (profile 'test').</p>
 *
 * @author PitStop Team
 * @since 1.0.0
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Cliente Controller Integration Tests")
class ClienteControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ClienteRepository clienteRepository;

    private CreateClienteRequest validPessoaFisicaRequest;
    private CreateClienteRequest validPessoaJuridicaRequest;
    private UpdateClienteRequest validUpdateRequest;

    @BeforeEach
    void setUp() {
        // Limpa base antes de cada teste
        clienteRepository.deleteAll();

        // Prepara dados de teste para Pessoa Física
        validPessoaFisicaRequest = CreateClienteRequest.builder()
            .tipo(TipoCliente.PESSOA_FISICA)
            .nome("João da Silva")
            .cpfCnpj("123.456.789-00")
            .email("joao@email.com")
            .telefone("(11) 3333-4444")
            .celular("(11) 98888-7777")
            .logradouro("Rua das Flores")
            .numero("123")
            .complemento("Apto 45B")
            .bairro("Centro")
            .cidade("São Paulo")
            .estado("SP")
            .cep("01310-100")
            .build();

        // Prepara dados de teste para Pessoa Jurídica
        validPessoaJuridicaRequest = CreateClienteRequest.builder()
            .tipo(TipoCliente.PESSOA_JURIDICA)
            .nome("Empresa XYZ Ltda")
            .cpfCnpj("12.345.678/0001-90")
            .email("contato@empresa.com")
            .telefone("(11) 4444-5555")
            .celular("(11) 99999-8888")
            .logradouro("Av. Paulista")
            .numero("1000")
            .bairro("Bela Vista")
            .cidade("São Paulo")
            .estado("SP")
            .cep("01310-200")
            .build();

        // Prepara dados de atualização
        validUpdateRequest = UpdateClienteRequest.builder()
            .nome("João da Silva Santos")
            .email("joao.santos@email.com")
            .telefone("(11) 3333-5555")
            .celular("(11) 99999-7777")
            .logradouro("Av. Paulista")
            .numero("1000")
            .complemento("Sala 1503")
            .bairro("Bela Vista")
            .cidade("São Paulo")
            .estado("SP")
            .cep("01310-100")
            .build();
    }

    // ========== TESTES DE CRIAÇÃO ==========

    @Test
    @WithMockUser(roles = "ATENDENTE")
    @DisplayName("Deve criar cliente Pessoa Física com sucesso")
    void deveCriarClientePessoaFisica() throws Exception {
        mockMvc.perform(post("/api/clientes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validPessoaFisicaRequest)))
            .andDo(print())
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.tipo").value("PESSOA_FISICA"))
            .andExpect(jsonPath("$.nome").value("João da Silva"))
            .andExpect(jsonPath("$.cpfCnpj").value("123.456.789-00"))
            .andExpect(jsonPath("$.email").value("joao@email.com"))
            .andExpect(jsonPath("$.telefone").value("(11) 3333-4444"))
            .andExpect(jsonPath("$.celular").value("(11) 98888-7777"))
            .andExpect(jsonPath("$.endereco.logradouro").value("Rua das Flores"))
            .andExpect(jsonPath("$.endereco.numero").value("123"))
            .andExpect(jsonPath("$.endereco.cidade").value("São Paulo"))
            .andExpect(jsonPath("$.endereco.estado").value("SP"))
            .andExpect(jsonPath("$.ativo").value(true))
            .andExpect(jsonPath("$.createdAt").exists())
            .andExpect(jsonPath("$.updatedAt").exists());
    }

    @Test
    @WithMockUser(roles = "ATENDENTE")
    @DisplayName("Deve criar cliente Pessoa Jurídica com sucesso")
    void deveCriarClientePessoaJuridica() throws Exception {
        mockMvc.perform(post("/api/clientes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validPessoaJuridicaRequest)))
            .andDo(print())
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.tipo").value("PESSOA_JURIDICA"))
            .andExpect(jsonPath("$.nome").value("Empresa XYZ Ltda"))
            .andExpect(jsonPath("$.cpfCnpj").value("12.345.678/0001-90"));
    }

    @Test
    @WithMockUser(roles = "ATENDENTE")
    @DisplayName("Deve retornar 409 ao tentar criar cliente com CPF/CNPJ duplicado")
    void deveRetornar409QuandoCpfCnpjDuplicado() throws Exception {
        // Primeiro cria um cliente
        mockMvc.perform(post("/api/clientes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validPessoaFisicaRequest)))
            .andExpect(status().isCreated());

        // Tenta criar outro com mesmo CPF
        mockMvc.perform(post("/api/clientes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validPessoaFisicaRequest)))
            .andDo(print())
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.title").value("CPF/CNPJ Duplicado"))
            .andExpect(jsonPath("$.detail").value(containsString("123.456.789-00")));
    }

    @Test
    @WithMockUser(roles = "ATENDENTE")
    @DisplayName("Deve retornar 400 quando nome estiver vazio")
    void deveRetornar400QuandoNomeVazio() throws Exception {
        validPessoaFisicaRequest.setNome("");

        mockMvc.perform(post("/api/clientes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validPessoaFisicaRequest)))
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").value("Erro de Validação"));
    }

    @Test
    @WithMockUser(roles = "ATENDENTE")
    @DisplayName("Deve retornar 400 quando CPF/CNPJ tiver formato inválido")
    void deveRetornar400QuandoCpfCnpjFormatoInvalido() throws Exception {
        validPessoaFisicaRequest.setCpfCnpj("12345678900"); // Sem formatação

        mockMvc.perform(post("/api/clientes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validPessoaFisicaRequest)))
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").value("Erro de Validação"));
    }

    @Test
    @WithMockUser(roles = "ATENDENTE")
    @DisplayName("Deve retornar 400 quando nenhum telefone for informado")
    void deveRetornar400QuandoNenhumTelefone() throws Exception {
        validPessoaFisicaRequest.setTelefone(null);
        validPessoaFisicaRequest.setCelular(null);

        mockMvc.perform(post("/api/clientes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validPessoaFisicaRequest)))
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").value("Erro de Validação"));
    }

    @Test
    @DisplayName("Deve retornar 403 quando usuário não autenticado tenta criar cliente")
    void deveRetornar403QuandoNaoAutenticado() throws Exception {
        mockMvc.perform(post("/api/clientes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validPessoaFisicaRequest)))
            .andDo(print())
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MECANICO")
    @DisplayName("Deve retornar 403 quando mecânico tenta criar cliente")
    void deveRetornar403QuandoMecanicoTentaCriar() throws Exception {
        mockMvc.perform(post("/api/clientes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validPessoaFisicaRequest)))
            .andDo(print())
            .andExpect(status().isForbidden());
    }

    // ========== TESTES DE LEITURA ==========

    @Test
    @WithMockUser(roles = "ATENDENTE")
    @DisplayName("Deve buscar cliente por ID com sucesso")
    void deveBuscarClientePorId() throws Exception {
        // Cria cliente
        String response = mockMvc.perform(post("/api/clientes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validPessoaFisicaRequest)))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();

        String clienteId = objectMapper.readTree(response).get("id").asText();

        // Busca por ID
        mockMvc.perform(get("/api/clientes/{id}", clienteId))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(clienteId))
            .andExpect(jsonPath("$.nome").value("João da Silva"));
    }

    @Test
    @WithMockUser(roles = "ATENDENTE")
    @DisplayName("Deve retornar 404 ao buscar cliente inexistente")
    void deveRetornar404QuandoClienteNaoExiste() throws Exception {
        String idInexistente = "550e8400-e29b-41d4-a716-446655440000";

        mockMvc.perform(get("/api/clientes/{id}", idInexistente))
            .andDo(print())
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.title").value("Cliente Não Encontrado"));
    }

    @Test
    @WithMockUser(roles = "ATENDENTE")
    @DisplayName("Deve buscar cliente por CPF/CNPJ com sucesso")
    void deveBuscarClientePorCpfCnpj() throws Exception {
        // Cria cliente
        mockMvc.perform(post("/api/clientes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validPessoaFisicaRequest)))
            .andExpect(status().isCreated());

        // Busca por CPF
        mockMvc.perform(get("/api/clientes/cpf-cnpj/{cpfCnpj}", "123.456.789-00"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.cpfCnpj").value("123.456.789-00"))
            .andExpect(jsonPath("$.nome").value("João da Silva"));
    }

    @Test
    @WithMockUser(roles = "ATENDENTE")
    @DisplayName("Deve listar clientes com paginação")
    void deveListarClientesComPaginacao() throws Exception {
        // Cria 2 clientes
        mockMvc.perform(post("/api/clientes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validPessoaFisicaRequest)))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/clientes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validPessoaJuridicaRequest)))
            .andExpect(status().isCreated());

        // Lista todos
        mockMvc.perform(get("/api/clientes")
                .param("page", "0")
                .param("size", "10"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content.length()").value(2))
            .andExpect(jsonPath("$.totalElements").value(2))
            .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    @WithMockUser(roles = "ATENDENTE")
    @DisplayName("Deve filtrar clientes por tipo")
    void deveFiltrarClientesPorTipo() throws Exception {
        // Cria 2 clientes
        mockMvc.perform(post("/api/clientes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validPessoaFisicaRequest)))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/clientes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validPessoaJuridicaRequest)))
            .andExpect(status().isCreated());

        // Filtra por PESSOA_FISICA
        mockMvc.perform(get("/api/clientes")
                .param("tipo", "PESSOA_FISICA"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.content[0].tipo").value("PESSOA_FISICA"));
    }

    @Test
    @WithMockUser(roles = "MECANICO")
    @DisplayName("Mecânico pode listar clientes (read-only)")
    void mecanicoPoderListarClientes() throws Exception {
        mockMvc.perform(get("/api/clientes"))
            .andDo(print())
            .andExpect(status().isOk());
    }

    // ========== TESTES DE ATUALIZAÇÃO ==========

    @Test
    @WithMockUser(roles = "ATENDENTE")
    @DisplayName("Deve atualizar cliente com sucesso")
    void deveAtualizarCliente() throws Exception {
        // Cria cliente
        String response = mockMvc.perform(post("/api/clientes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validPessoaFisicaRequest)))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();

        String clienteId = objectMapper.readTree(response).get("id").asText();

        // Atualiza
        mockMvc.perform(put("/api/clientes/{id}", clienteId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validUpdateRequest)))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(clienteId))
            .andExpect(jsonPath("$.nome").value("João da Silva Santos"))
            .andExpect(jsonPath("$.email").value("joao.santos@email.com"))
            .andExpect(jsonPath("$.endereco.logradouro").value("Av. Paulista"))
            .andExpect(jsonPath("$.endereco.numero").value("1000"));
    }

    @Test
    @WithMockUser(roles = "ATENDENTE")
    @DisplayName("Deve retornar 404 ao tentar atualizar cliente inexistente")
    void deveRetornar404AoAtualizarClienteInexistente() throws Exception {
        String idInexistente = "550e8400-e29b-41d4-a716-446655440000";

        mockMvc.perform(put("/api/clientes/{id}", idInexistente)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validUpdateRequest)))
            .andDo(print())
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "MECANICO")
    @DisplayName("Deve retornar 403 quando mecânico tenta atualizar cliente")
    void deveRetornar403QuandoMecanicoTentaAtualizar() throws Exception {
        String idQualquer = "550e8400-e29b-41d4-a716-446655440000";

        mockMvc.perform(put("/api/clientes/{id}", idQualquer)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validUpdateRequest)))
            .andDo(print())
            .andExpect(status().isForbidden());
    }

    // ========== TESTES DE SOFT DELETE ==========

    @Test
    @WithMockUser(roles = "GERENTE")
    @DisplayName("Deve desativar cliente com sucesso (soft delete)")
    void deveDesativarCliente() throws Exception {
        // Cria cliente
        String response = mockMvc.perform(post("/api/clientes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validPessoaFisicaRequest)))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();

        String clienteId = objectMapper.readTree(response).get("id").asText();

        // Desativa
        mockMvc.perform(delete("/api/clientes/{id}", clienteId))
            .andDo(print())
            .andExpect(status().isNoContent());

        // Verifica que não aparece mais na listagem (soft delete)
        mockMvc.perform(get("/api/clientes"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    @DisplayName("Deve reativar cliente desativado")
    void deveReativarCliente() throws Exception {
        // Cria e desativa cliente
        String response = mockMvc.perform(post("/api/clientes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validPessoaFisicaRequest)))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();

        String clienteId = objectMapper.readTree(response).get("id").asText();

        mockMvc.perform(delete("/api/clientes/{id}", clienteId))
            .andExpect(status().isNoContent());

        // Reativa
        mockMvc.perform(patch("/api/clientes/{id}/reativar", clienteId))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(clienteId))
            .andExpect(jsonPath("$.ativo").value(true));

        // Verifica que voltou a aparecer na listagem
        mockMvc.perform(get("/api/clientes"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    @WithMockUser(roles = "ATENDENTE")
    @DisplayName("Deve retornar 403 quando atendente tenta desativar cliente")
    void deveRetornar403QuandoAtendenteTentaDesativar() throws Exception {
        String idQualquer = "550e8400-e29b-41d4-a716-446655440000";

        mockMvc.perform(delete("/api/clientes/{id}", idQualquer))
            .andDo(print())
            .andExpect(status().isForbidden());
    }

    // ========== TESTES DE ENDPOINTS AUXILIARES ==========

    @Test
    @WithMockUser(roles = "ATENDENTE")
    @DisplayName("Deve retornar lista de estados únicos")
    void deveRetornarListaEstados() throws Exception {
        // Cria cliente
        mockMvc.perform(post("/api/clientes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validPessoaFisicaRequest)))
            .andExpect(status().isCreated());

        // Busca estados
        mockMvc.perform(get("/api/clientes/filtros/estados"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[0]").value("SP"));
    }

    @Test
    @WithMockUser(roles = "ATENDENTE")
    @DisplayName("Deve retornar lista de cidades únicas")
    void deveRetornarListaCidades() throws Exception {
        // Cria cliente
        mockMvc.perform(post("/api/clientes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validPessoaFisicaRequest)))
            .andExpect(status().isCreated());

        // Busca cidades
        mockMvc.perform(get("/api/clientes/filtros/cidades"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[0]").value("São Paulo"));
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    @DisplayName("Deve retornar estatísticas de clientes")
    void deveRetornarEstatisticas() throws Exception {
        // Cria 1 PF e 1 PJ
        mockMvc.perform(post("/api/clientes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validPessoaFisicaRequest)))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/clientes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validPessoaJuridicaRequest)))
            .andExpect(status().isCreated());

        // Busca estatísticas
        mockMvc.perform(get("/api/clientes/estatisticas"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.pessoasFisicas").value(1))
            .andExpect(jsonPath("$.pessoasJuridicas").value(1))
            .andExpect(jsonPath("$.total").value(2));
    }

    @Test
    @WithMockUser(roles = "ATENDENTE")
    @DisplayName("Deve retornar 403 quando atendente tenta acessar estatísticas")
    void deveRetornar403QuandoAtendenteTentaEstatisticas() throws Exception {
        mockMvc.perform(get("/api/clientes/estatisticas"))
            .andDo(print())
            .andExpect(status().isForbidden());
    }
}
