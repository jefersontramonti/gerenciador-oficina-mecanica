package com.pitstop.oficina.service;

import com.pitstop.oficina.domain.Oficina;
import com.pitstop.oficina.dto.CreateOficinaRequest;
import com.pitstop.oficina.dto.OficinaResponse;
import com.pitstop.oficina.exception.CnpjAlreadyExistsException;
import com.pitstop.oficina.exception.OficinaNotFoundException;
import com.pitstop.oficina.exception.PlanUpgradeException;
import com.pitstop.oficina.mapper.OficinaMapper;
import com.pitstop.oficina.repository.OficinaRepository;
import com.pitstop.cliente.domain.Endereco;
import com.pitstop.oficina.domain.*;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para OficinaService.
 * Usa Mockito para mockar dependências.
 *
 * @author PitStop Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OficinaService - Testes Unitários")
class OficinaServiceTest {

    @Mock
    private OficinaRepository oficinaRepository;

    @Mock
    private OficinaMapper oficinaMapper;

    @InjectMocks
    private OficinaService oficinaService;

    @Test
    @DisplayName("Deve criar oficina com sucesso")
    void deveCriarOficinaComSucesso() {
        // Arrange
        CreateOficinaRequest request = createValidRequest();
        Oficina oficina = createOficinaEntity();
        OficinaResponse expectedResponse = createOficinaResponse();

        when(oficinaMapper.toEntity(request)).thenReturn(oficina);
        when(oficinaRepository.save(any(Oficina.class))).thenReturn(oficina);
        when(oficinaMapper.toResponse(oficina)).thenReturn(expectedResponse);

        // Act
        OficinaResponse response = oficinaService.create(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.nomeFantasia()).isEqualTo("Silva Mecânica");
        assertThat(response.status()).isEqualTo(StatusOficina.ATIVA);
        assertThat(response.plano()).isEqualTo(PlanoAssinatura.PROFISSIONAL);

        verify(oficinaRepository).save(any(Oficina.class));
        verify(oficinaMapper).toResponse(oficina);
    }

    @Test
    @Disabled("CNPJ validation not implemented yet - see OficinaService.java:59-62 TODO")
    @DisplayName("Deve lançar exceção ao criar oficina com CNPJ existente")
    void deveLancarExcecaoAoCriarOficinaComCnpjExistente() {
        // Arrange
        CreateOficinaRequest request = createValidRequest();
        when(oficinaRepository.existsByCnpj(request.cnpj())).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> oficinaService.create(request))
            .isInstanceOf(CnpjAlreadyExistsException.class)
            .hasMessageContaining("Já existe uma oficina cadastrada com o CNPJ");

        verify(oficinaRepository).existsByCnpj(request.cnpj());
        verify(oficinaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve buscar oficina por ID com sucesso")
    void deveBuscarOficinaPorIdComSucesso() {
        // Arrange
        UUID id = UUID.randomUUID();
        Oficina oficina = createOficinaEntity();
        oficina.setId(id);
        OficinaResponse expectedResponse = createOficinaResponse();

        when(oficinaRepository.findById(id)).thenReturn(Optional.of(oficina));
        when(oficinaMapper.toResponse(oficina)).thenReturn(expectedResponse);

        // Act
        OficinaResponse response = oficinaService.findById(id);

        // Assert
        assertThat(response).isNotNull();
        verify(oficinaRepository).findById(id);
    }

    @Test
    @DisplayName("Deve lançar exceção ao buscar oficina inexistente")
    void deveLancarExcecaoAoBuscarOficinaInexistente() {
        // Arrange
        UUID id = UUID.randomUUID();
        when(oficinaRepository.findById(id)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> oficinaService.findById(id))
            .isInstanceOf(OficinaNotFoundException.class)
            .hasMessageContaining("Oficina não encontrada com o ID");

        verify(oficinaRepository).findById(id);
    }

    @Test
    @DisplayName("Deve suspender oficina com sucesso")
    void deveSuspenderOficinaComSucesso() {
        // Arrange
        UUID id = UUID.randomUUID();
        Oficina oficina = createOficinaEntity();
        oficina.setId(id);
        oficina.setStatus(StatusOficina.ATIVA);

        when(oficinaRepository.findById(id)).thenReturn(Optional.of(oficina));
        when(oficinaRepository.save(any(Oficina.class))).thenReturn(oficina);

        // Act
        oficinaService.suspend(id);

        // Assert
        assertThat(oficina.getStatus()).isEqualTo(StatusOficina.SUSPENSA);
        verify(oficinaRepository).findById(id);
        verify(oficinaRepository).save(oficina);
    }

    @Test
    @DisplayName("Deve reativar oficina suspensa com sucesso")
    void deveReativarOficinaSuspensaComSucesso() {
        // Arrange
        UUID id = UUID.randomUUID();
        Oficina oficina = createOficinaEntity();
        oficina.setId(id);
        oficina.setStatus(StatusOficina.SUSPENSA);

        when(oficinaRepository.findById(id)).thenReturn(Optional.of(oficina));
        when(oficinaRepository.save(any(Oficina.class))).thenReturn(oficina);

        // Act
        oficinaService.activate(id);

        // Assert
        assertThat(oficina.getStatus()).isEqualTo(StatusOficina.ATIVA);
        assertThat(oficina.getDataVencimentoPlano()).isAfter(LocalDate.now());
        verify(oficinaRepository).save(oficina);
    }

    @Test
    @DisplayName("Deve fazer upgrade de plano com sucesso")
    void deveFazerUpgradeDePlanoComSucesso() {
        // Arrange
        UUID id = UUID.randomUUID();
        Oficina oficina = createOficinaEntity();
        oficina.setId(id);
        oficina.setPlano(PlanoAssinatura.ECONOMICO);

        OficinaResponse expectedResponse = createOficinaResponse();

        when(oficinaRepository.findById(id)).thenReturn(Optional.of(oficina));
        when(oficinaRepository.save(any(Oficina.class))).thenReturn(oficina);
        when(oficinaMapper.toResponse(oficina)).thenReturn(expectedResponse);

        // Act
        OficinaResponse response = oficinaService.upgradePlan(id, PlanoAssinatura.PROFISSIONAL);

        // Assert
        assertThat(oficina.getPlano()).isEqualTo(PlanoAssinatura.PROFISSIONAL);
        assertThat(oficina.getValorMensalidade()).isEqualByComparingTo(new BigDecimal("199.90"));
        verify(oficinaRepository).save(oficina);
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar fazer upgrade para plano inferior")
    void deveLancarExcecaoAoTentarUpgradeParaPlanoInferior() {
        // Arrange
        UUID id = UUID.randomUUID();
        Oficina oficina = createOficinaEntity();
        oficina.setId(id);
        oficina.setPlano(PlanoAssinatura.PROFISSIONAL);

        when(oficinaRepository.findById(id)).thenReturn(Optional.of(oficina));

        // Act & Assert
        assertThatThrownBy(() -> oficinaService.upgradePlan(id, PlanoAssinatura.ECONOMICO))
            .isInstanceOf(PlanUpgradeException.class);

        verify(oficinaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve calcular MRR corretamente")
    void deveCalcularMRRCorretamente() {
        // Arrange
        Double expectedMRR = 599.70; // 3 oficinas: 99.90 + 199.90 + 299.90
        when(oficinaRepository.calculateMRR()).thenReturn(expectedMRR);

        // Act
        Double mrr = oficinaService.calculateMRR();

        // Assert
        assertThat(mrr).isEqualTo(expectedMRR);
        verify(oficinaRepository).calculateMRR();
    }

    // ===== Métodos auxiliares para criar objetos de teste =====

    private CreateOficinaRequest createValidRequest() {
        Endereco endereco = new Endereco();
        endereco.setLogradouro("Rua das Flores");
        endereco.setNumero("123");
        endereco.setBairro("Centro");
        endereco.setCidade("São Paulo");
        endereco.setEstado("SP");
        endereco.setCep("01000000");

        Contato contato = Contato.builder()
            .email("contato@silvamecanica.com.br")
            .telefoneCelular("(11) 99999-0000")
            .telefoneFixo("(11) 3000-1000")
            .build();

        return new CreateOficinaRequest(
            "Auto Mecânica Silva LTDA",      // nome (razaoSocial)
            "Silva Mecânica",                 // nomeFantasia
            "12345678000190",                 // cnpj
            "123456789",                      // inscricaoEstadual
            "987654321",                      // inscricaoMunicipal
            TipoPessoa.PESSOA_JURIDICA,      // tipoPessoa
            RegimeTributario.SIMPLES_NACIONAL, // regimeTributario
            PlanoAssinatura.PROFISSIONAL,    // planoAssinatura
            contato,                          // contato
            endereco,                         // endereco
            null,                             // informacoesOperacionais
            null,                             // redesSociais
            null                              // dadosBancarios
        );
    }

    private Oficina createOficinaEntity() {
        Endereco endereco = new Endereco();
        endereco.setLogradouro("Rua das Flores");
        endereco.setNumero("123");
        endereco.setBairro("Centro");
        endereco.setCidade("São Paulo");
        endereco.setEstado("SP");
        endereco.setCep("01000000");

        Contato contato = Contato.builder()
            .email("contato@silvamecanica.com.br")
            .telefoneCelular("(11) 99999-0000")
            .telefoneFixo("(11) 3000-1000")
            .build();

        return Oficina.builder()
            .id(UUID.randomUUID())
            .razaoSocial("Auto Mecânica Silva LTDA")
            .nomeFantasia("Silva Mecânica")
            .cnpjCpf("12345678000190")
            .tipoPessoa(TipoPessoa.PESSOA_JURIDICA)
            .nomeResponsavel("José Silva")
            .contato(contato)
            .regimeTributario(RegimeTributario.SIMPLES_NACIONAL)
            .plano(PlanoAssinatura.PROFISSIONAL)
            .status(StatusOficina.ATIVA)
            .dataAssinatura(LocalDate.now())
            .dataVencimentoPlano(LocalDate.now().plusDays(7))
            .valorMensalidade(new BigDecimal("199.90"))
            .endereco(endereco)
            .build();
    }

    private OficinaResponse createOficinaResponse() {
        Endereco endereco = new Endereco();
        endereco.setLogradouro("Rua das Flores");
        endereco.setNumero("123");
        endereco.setBairro("Centro");
        endereco.setCidade("São Paulo");
        endereco.setEstado("SP");
        endereco.setCep("01000000");

        return new OficinaResponse(
            UUID.randomUUID(),
            "Silva Mecânica",
            "Auto Mecânica Silva LTDA",
            "12345678000190",
            "123456789",
            "987654321",
            TipoPessoa.PESSOA_JURIDICA,
            "José Silva",
            "contato@silvamecanica.com.br",
            "1130001000",
            "11999990000",
            RegimeTributario.SIMPLES_NACIONAL,
            PlanoAssinatura.PROFISSIONAL,
            StatusOficina.ATIVA,
            LocalDate.now(),
            LocalDate.now().plusDays(7),
            new BigDecimal("199.90"),
            endereco,
            null,
            null,
            null,
            null,
            new BigDecimal("80.00") // valorHora
        );
    }
}
