# Relatório de Implementação - Nota Fiscal Eletrônica (NF-e)

**Sistema:** PitStop - Gestão de Oficina Mecânica
**Versão:** 1.0
**Data:** 01/11/2025
**Autor:** Equipe Técnica PitStop

---

## 1. VISÃO GERAL

### 1.1 Objetivo
Implementar a funcionalidade de emissão de Nota Fiscal Eletrônica (NF-e) no sistema PitStop, permitindo que oficinas mecânicas emitam notas fiscais para seus clientes de forma automatizada, integrada com a SEFAZ (Secretaria da Fazenda).

### 1.2 Escopo
- **Incluído:**
  - Emissão de NF-e (modelo 55)
  - Integração com SEFAZ via Web Services
  - Consulta de status de NF-e
  - Cancelamento de NF-e
  - Carta de Correção Eletrônica (CC-e)
  - Armazenamento de XMLs (enviados e retornados)
  - Geração de DANFE (Documento Auxiliar da Nota Fiscal Eletrônica) em PDF
  - Envio de DANFE por email para o cliente
  - Controle de numeração e série
  - Certificado Digital A1 (armazenado localmente) ou A3 (token/smart card)

- **Não incluído (futuras fases):**
  - NFS-e (Nota Fiscal de Serviço Eletrônica - municipal)
  - Inutilização de faixa de numeração
  - Manifesto do Destinatário
  - Contingência offline

### 1.3 Benefícios
- ✅ Conformidade fiscal e legal
- ✅ Redução de erros manuais
- ✅ Integração automática com Ordem de Serviço
- ✅ Agilidade no processo de faturamento
- ✅ Rastreabilidade completa das notas emitidas
- ✅ Relatórios fiscais automatizados

---

## 2. PRÉ-REQUISITOS E ONBOARDING DO CLIENTE

### 2.1 Requisitos Legais Obrigatórios

**⚠️ IMPORTANTE:** O sistema PitStop NÃO pode emitir NF-e se o cliente não tiver os requisitos legais básicos. Estes são **obrigações fiscais externas ao sistema** e devem ser providenciadas pelo dono da oficina **antes** de usar a funcionalidade de NF-e.

#### 2.1.1 Documentação Fiscal Obrigatória

**O cliente DEVE TER:**

1. **✅ CNPJ Ativo**
   - Emitido pela Receita Federal do Brasil
   - Status: Ativo (não pode estar suspenso, baixado ou inapto)
   - Validação: Consulta pública no portal da RFB

2. **✅ Inscrição Estadual (IE)**
   - Emitida pela SEFAZ do estado onde a oficina está localizada
   - Status: Ativa e regular
   - Necessária para autorizar emissão de NF-e

3. **✅ Certificado Digital e-CNPJ (A1 ou A3)**
   - **Tipo A1**: Arquivo .pfx armazenado no servidor
     - Validade: 1 ano
     - Custo médio: R$ 150 - R$ 250/ano
     - Facilidade: Mais fácil de integrar

   - **Tipo A3**: Token USB ou Smart Card
     - Validade: 3 anos
     - Custo médio: R$ 200 - R$ 400 + token (R$ 100-150)
     - Segurança: Mais seguro (chave nunca sai do token)

4. **✅ Credenciamento na SEFAZ**
   - Solicitado no portal da SEFAZ do estado
   - Prazo de aprovação: 1-3 dias úteis
   - Sem custo (serviço público)
   - Pré-requisito: Ter IE ativa

5. **✅ Regime Tributário Definido**
   - Simples Nacional
   - Lucro Presumido
   - Lucro Real
   - Necessário para definir tributação correta

#### 2.1.2 Fluxo de Preparação do Cliente

```
Cliente não tem nada
        ↓
1. Abrir CNPJ (se não tiver)
   → Contador ou Junta Comercial
   → Prazo: 5-15 dias úteis
        ↓
2. Solicitar Inscrição Estadual
   → Portal da SEFAZ do estado
   → Prazo: 5-10 dias úteis
        ↓
3. Comprar Certificado Digital e-CNPJ
   → Autoridade Certificadora (AC)
   → Ex: Serasa, Certisign, Valid, etc.
   → Prazo: 1-2 dias (presencial)
        ↓
4. Credenciar na SEFAZ para NF-e
   → Portal da SEFAZ
   → Prazo: 1-3 dias úteis
        ↓
5. ✅ PRONTO! Pode configurar no PitStop
```

### 2.2 Wizard de Configuração Inicial (First-Time Setup)

Quando o cliente faz login pela primeira vez após a contratação, o sistema detecta que a empresa não está configurada e apresenta um **wizard em 3 etapas**.

#### 2.2.1 Etapa 1: Dados da Empresa

**Campos obrigatórios:**

```java
// EmpresaSetupDTO.java
public class EmpresaSetupDTO {
    @NotBlank(message = "Razão social é obrigatória")
    @Size(min = 3, max = 100)
    private String razaoSocial;

    @NotBlank
    @Size(max = 100)
    private String nomeFantasia;

    @NotBlank(message = "CNPJ é obrigatório")
    @CNPJ  // Validação customizada
    private String cnpj;

    @NotBlank
    @Size(max = 14)
    private String inscricaoEstadual;

    @NotBlank
    @Size(max = 15)
    private String inscricaoMunicipal; // Opcional

    @NotNull
    @Enumerated(EnumType.STRING)
    private RegimeTributario regimeTributario;

    // Endereço completo
    @Valid
    private EnderecoDTO endereco;

    // Contatos
    @NotBlank
    @Email
    private String emailNfe;

    @Pattern(regexp = "\\d{10,11}")
    private String telefone;
}
```

**Tela (Wireframe):**

```
┌────────────────────────────────────────────────────┐
│  🏢 Configuração da Oficina - Passo 1 de 3        │
├────────────────────────────────────────────────────┤
│                                                    │
│  Dados da Empresa                                  │
│  ┌──────────────────────────────────────────────┐ │
│  │ Razão Social *                               │ │
│  │ [PITSTOP MECANICA LTDA___________________]  │ │
│  │                                               │ │
│  │ Nome Fantasia                                 │ │
│  │ [PitStop Auto Center_____________________]  │ │
│  │                                               │ │
│  │ CNPJ * (apenas números)                      │ │
│  │ [12345678000190] [Validar ✓]                │ │
│  │ ✅ CNPJ válido e ativo                       │ │
│  │                                               │ │
│  │ Inscrição Estadual *                         │ │
│  │ [123456789123_____]                          │ │
│  │                                               │ │
│  │ Regime Tributário *                          │ │
│  │ [Simples Nacional ▼]                         │ │
│  └──────────────────────────────────────────────┘ │
│                                                    │
│  Endereço Completo                                 │
│  ┌──────────────────────────────────────────────┐ │
│  │ CEP *                                         │ │
│  │ [01234567] [🔍 Buscar]                       │ │
│  │                                               │ │
│  │ Logradouro *          Número *               │ │
│  │ [Rua das Oficinas__] [1000]                  │ │
│  │                                               │ │
│  │ Complemento           Bairro *                │ │
│  │ [Galpão 2_________] [Centro__________]       │ │
│  │                                               │ │
│  │ Cidade *              UF *                    │ │
│  │ [São Paulo________] [SP ▼]                   │ │
│  └──────────────────────────────────────────────┘ │
│                                                    │
│  Contatos                                          │
│  ┌──────────────────────────────────────────────┐ │
│  │ Telefone                                      │ │
│  │ [(11) 3333-4444_____]                        │ │
│  │                                               │ │
│  │ Email para NF-e *                            │ │
│  │ [nfe@pitstop.com.br_____________________]   │ │
│  └──────────────────────────────────────────────┘ │
│                                                    │
│  [Cancelar]               [Próximo: Certificado →]│
└────────────────────────────────────────────────────┘
```

**Validações backend:**

```java
@Service
public class EmpresaValidationService {

    /**
     * Valida CNPJ na Receita Federal (API pública).
     */
    public CnpjValidationResult validarCNPJ(String cnpj) {
        // Consulta API da RFB
        // https://www.receitaws.com.br/v1/cnpj/{cnpj}

        CnpjValidationResult result = new CnpjValidationResult();
        result.setValido(true);
        result.setSituacao("ATIVA");
        result.setDataAbertura(LocalDate.parse("2020-01-15"));

        return result;
    }

    /**
     * Valida IE na SEFAZ (serviço SOAP ou API REST).
     */
    public boolean validarInscricaoEstadual(String ie, String uf) {
        // Implementar consulta SEFAZ
        return true;
    }
}
```

#### 2.2.2 Etapa 2: Certificado Digital

**Campos:**

```java
public class CertificadoSetupDTO {
    @NotNull
    private TipoCertificado tipo; // A1 ou A3

    // Se A1
    @ValidFile(extensions = {"pfx", "p12"})
    private MultipartFile arquivoCertificado;

    @NotBlank(message = "Senha do certificado é obrigatória")
    private String senhaCertificado;

    // Informações extraídas automaticamente
    private String titular;
    private String cnpjCertificado;
    private LocalDate dataValidade;
    private String tipoCertificadoCompleto; // "e-CNPJ A1"
}
```

**Tela:**

```
┌────────────────────────────────────────────────────┐
│  🔐 Configuração da Oficina - Passo 2 de 3        │
├────────────────────────────────────────────────────┤
│                                                    │
│  Certificado Digital para NF-e                     │
│                                                    │
│  Qual tipo você possui?                            │
│  (•) A1 - Arquivo .pfx no computador              │
│  ( ) A3 - Token USB ou Smart Card                 │
│                                                    │
│  ┌──────────────────────────────────────────────┐ │
│  │ 📁 Upload do Certificado A1                  │ │
│  │                                               │ │
│  │  [Selecionar arquivo .pfx ou .p12]           │ │
│  │  ou arraste aqui                              │ │
│  │                                               │ │
│  │  Arquivo selecionado:                        │ │
│  │  📄 certificado-pitstop.pfx (4.2 KB)        │ │
│  │                                               │ │
│  │  Senha do certificado *                      │ │
│  │  [**********************]                    │ │
│  │  [👁️ Mostrar]                                │ │
│  │                                               │ │
│  │  ⚠️ SEGURANÇA:                               │ │
│  │  • A senha será criptografada (AES-256)     │ │
│  │  • O arquivo será armazenado com segurança  │ │
│  │  • Acesso restrito apenas ao sistema        │ │
│  └──────────────────────────────────────────────┘ │
│                                                    │
│  [Validar Certificado]                             │
│                                                    │
│  Informações do Certificado:                       │
│  ┌──────────────────────────────────────────────┐ │
│  │ ✅ Titular: PITSTOP MECANICA LTDA            │ │
│  │ ✅ CNPJ: 12.345.678/0001-90                  │ │
│  │ ✅ Tipo: e-CNPJ A1                           │ │
│  │ ✅ Validade: 31/12/2025 (364 dias)           │ │
│  │ ✅ Emitido por: Serasa Experian              │ │
│  │ ✅ Cadeia de certificação válida             │ │
│  └──────────────────────────────────────────────┘ │
│                                                    │
│  ℹ️ Não tem certificado digital?                  │
│  [Ver onde comprar] [Tutorial em vídeo]           │
│                                                    │
│  [← Voltar]     [Testar Conexão]    [Próximo →]  │
└────────────────────────────────────────────────────┘
```

**Validação do certificado:**

```java
@Service
public class CertificadoService {

    /**
     * Valida e extrai informações do certificado digital.
     */
    public CertificadoInfo validarCertificado(
        MultipartFile arquivo,
        String senha
    ) throws CertificadoInvalidoException {

        try {
            // Carregar certificado
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(arquivo.getInputStream(), senha.toCharArray());

            // Obter certificado X509
            String alias = keyStore.aliases().nextElement();
            X509Certificate cert = (X509Certificate) keyStore.getCertificate(alias);

            // Validar validade
            cert.checkValidity();

            // Extrair informações
            CertificadoInfo info = new CertificadoInfo();
            info.setTitular(extrairCN(cert));
            info.setCnpj(extrairCNPJ(cert));
            info.setDataValidade(cert.getNotAfter().toInstant()
                .atZone(ZoneId.systemDefault()).toLocalDate());
            info.setTipo("e-CNPJ A1");
            info.setEmissor(cert.getIssuerDN().getName());

            // Validar cadeia de certificação
            validarCadeiaCertificacao(cert);

            return info;

        } catch (Exception e) {
            throw new CertificadoInvalidoException("Certificado inválido: " + e.getMessage());
        }
    }

    /**
     * Testa conexão com SEFAZ usando o certificado.
     */
    public TestConexaoResult testarConexaoSefaz(
        String cnpj,
        String uf,
        byte[] certificado,
        String senha
    ) {
        try {
            ConfiguracoesNfe config = new ConfiguracoesNfe();
            config.setEstado(Estados.valueOf(uf));
            config.setAmbiente(TipoAmbiente.HOMOLOGACAO);
            config.setCertificado(carregarCertificado(certificado, senha));

            // Testar consulta de status do serviço
            TRetConsStatServ retorno = Nfe.statusServico(config, TipoDocumento.NFE);

            TestConexaoResult result = new TestConexaoResult();
            result.setConexaoOk("107".equals(retorno.getCStat()));
            result.setMotivo(retorno.getXMotivo());
            result.setAmbiente("Homologação");

            return result;

        } catch (Exception e) {
            throw new NFeException("Erro ao testar conexão: " + e.getMessage());
        }
    }
}
```

#### 2.2.3 Etapa 3: Configurações de NF-e

**Campos:**

```java
public class NFeConfigDTO {
    @NotNull
    @Min(1)
    @Max(999)
    private Integer serie = 1;

    @NotNull
    @Min(1)
    private Long proximoNumero = 1L;

    @NotNull
    private TipoAmbiente ambiente; // HOMOLOGACAO ou PRODUCAO

    // Tributação padrão
    @NotBlank
    private String csosnPadrao; // Ex: "102"

    @NotBlank
    private String cstPisPadrao; // Ex: "49"

    @NotBlank
    private String cstCofinsPadrao; // Ex: "49"

    @Size(max = 500)
    private String informacoesComplementaresPadrao;
}
```

**Tela:**

```
┌────────────────────────────────────────────────────┐
│  📄 Configuração da Oficina - Passo 3 de 3        │
├────────────────────────────────────────────────────┤
│                                                    │
│  Configurações de Nota Fiscal Eletrônica           │
│                                                    │
│  Numeração                                         │
│  ┌──────────────────────────────────────────────┐ │
│  │ Série da NF-e *                               │ │
│  │ [1___]  (Padrão: 1)                          │ │
│  │                                               │ │
│  │ Próximo número *                              │ │
│  │ [000001___]                                   │ │
│  │                                               │ │
│  │ ℹ️ Esta será sua primeira NF-e: 1/1          │ │
│  └──────────────────────────────────────────────┘ │
│                                                    │
│  Ambiente de Emissão                               │
│  ┌──────────────────────────────────────────────┐ │
│  │ (•) Homologação - Testes (RECOMENDADO)       │ │
│  │     ✓ Notas SEM valor fiscal                 │ │
│  │     ✓ Ideal para testes e treinamento        │ │
│  │     ✓ Pode mudar para Produção depois        │ │
│  │                                               │ │
│  │ ( ) Produção - Notas com validade fiscal     │ │
│  │     ⚠️ Apenas após testar em Homologação     │ │
│  └──────────────────────────────────────────────┘ │
│                                                    │
│  Tributação Padrão (Simples Nacional)              │
│  ┌──────────────────────────────────────────────┐ │
│  │ CSOSN (ICMS) *                                │ │
│  │ [102 - Sem tributação pelo ICMS ▼]           │ │
│  │                                               │ │
│  │ CST PIS *                                     │ │
│  │ [49 - Outras operações ▼]                    │ │
│  │                                               │ │
│  │ CST COFINS *                                  │ │
│  │ [49 - Outras operações ▼]                    │ │
│  │                                               │ │
│  │ ℹ️ Consulte seu contador sobre tributação   │ │
│  └──────────────────────────────────────────────┘ │
│                                                    │
│  Informações Adicionais (opcional)                 │
│  ┌──────────────────────────────────────────────┐ │
│  │ Texto exibido em todas as NF-e:              │ │
│  │ ┌──────────────────────────────────────────┐ │ │
│  │ │Sistema PitStop - Gestão de Oficina       │ │ │
│  │ │Atendimento: (11) 3333-4444               │ │ │
│  │ │                                           │ │ │
│  │ │                                           │ │ │
│  │ └──────────────────────────────────────────┘ │ │
│  └──────────────────────────────────────────────┘ │
│                                                    │
│  [Testar Emissão de NF-e]                         │
│                                                    │
│  [← Voltar]                    [Concluir Setup ✓] │
└────────────────────────────────────────────────────┘
```

#### 2.2.4 Teste Final de Conexão

Após concluir as 3 etapas, o sistema executa um teste completo:

```java
@Service
public class SetupValidationService {

    /**
     * Valida todas as configurações antes de ativar NF-e.
     */
    public SetupValidationResult validarSetupCompleto(UUID empresaId) {

        Empresa empresa = empresaRepository.findById(empresaId).orElseThrow();

        SetupValidationResult result = new SetupValidationResult();
        List<String> erros = new ArrayList<>();
        List<String> avisos = new ArrayList<>();

        // 1. Validar dados cadastrais
        if (empresa.getCnpj() == null || !validarCNPJ(empresa.getCnpj())) {
            erros.add("CNPJ inválido ou inativo");
        }

        if (empresa.getInscricaoEstadual() == null) {
            erros.add("Inscrição Estadual não informada");
        }

        // 2. Validar certificado
        try {
            certificadoService.validarCertificado(empresa);
        } catch (CertificadoInvalidoException e) {
            erros.add("Certificado digital: " + e.getMessage());
        }

        // 3. Testar conexão SEFAZ
        try {
            TestConexaoResult conexao = nfeSefazService.testarConexao(empresa);
            if (!conexao.isConexaoOk()) {
                erros.add("Falha na conexão com SEFAZ: " + conexao.getMotivo());
            }
        } catch (Exception e) {
            erros.add("Erro ao conectar com SEFAZ: " + e.getMessage());
        }

        // 4. Verificar credenciamento
        if (!sefazService.verificarCredenciamento(empresa.getCnpj(), empresa.getUf())) {
            erros.add("CNPJ não está credenciado na SEFAZ para emitir NF-e");
        }

        // 5. Avisos (não bloqueantes)
        if (empresa.getAmbienteNfe().equals("1")) { // Produção
            avisos.add("Você está em PRODUÇÃO. Notas terão validade fiscal!");
        }

        LocalDate validadeCert = empresa.getDataValidadeCertificado();
        if (validadeCert.isBefore(LocalDate.now().plusMonths(1))) {
            avisos.add("Certificado vence em menos de 30 dias. Renove logo!");
        }

        result.setValido(erros.isEmpty());
        result.setErros(erros);
        result.setAvisos(avisos);

        return result;
    }
}
```

**Tela de resultado:**

```
┌────────────────────────────────────────────────────┐
│  ✅ Validação Concluída!                           │
├────────────────────────────────────────────────────┤
│                                                    │
│  Resultado dos Testes:                             │
│                                                    │
│  ✅ CNPJ válido e ativo                           │
│  ✅ Inscrição Estadual regular                    │
│  ✅ Certificado digital válido (vence 31/12/2025) │
│  ✅ Conexão com SEFAZ-SP estabelecida             │
│  ✅ CNPJ credenciado para emitir NF-e             │
│  ✅ Ambiente: Homologação (testes)                │
│                                                    │
│  ⚠️ Avisos:                                        │
│  • Você está em modo Homologação                  │
│  • As notas NÃO têm validade fiscal               │
│  • Faça testes antes de ativar Produção           │
│                                                    │
│  🎉 Tudo pronto para emitir NF-e!                 │
│                                                    │
│  Próximos passos:                                  │
│  1. Finalize uma Ordem de Serviço                 │
│  2. Clique em "Emitir NF-e"                       │
│  3. Verifique o DANFE e o XML gerados             │
│  4. Quando estiver seguro, mude para Produção     │
│                                                    │
│  [Ver Tutorial] [Emitir NF-e de Teste] [Dashboard]│
└────────────────────────────────────────────────────┘
```

### 2.3 Tratamento de Erros Comuns no Setup

#### 2.3.1 Erro: Certificado Expirado

```
┌────────────────────────────────────────────────────┐
│  ❌ Certificado Digital Expirado                   │
├────────────────────────────────────────────────────┤
│                                                    │
│  O certificado digital não é mais válido.          │
│                                                    │
│  Detalhes:                                         │
│  • Vencimento: 15/08/2024                         │
│  • Situação: EXPIRADO há 78 dias                  │
│                                                    │
│  O que fazer:                                      │
│  1. Renove o certificado junto à Autoridade       │
│     Certificadora (AC) onde foi emitido           │
│  2. Compre um novo certificado se necessário      │
│  3. Faça o upload do certificado renovado         │
│                                                    │
│  Onde renovar:                                     │
│  • Serasa Experian: www.serasaexperian.com.br    │
│  • Certisign: www.certisign.com.br                │
│  • Valid: www.valid.com.br                        │
│  • Soluti: www.soluti.com.br                      │
│                                                    │
│  [Ver Tutorial] [Fechar]                           │
└────────────────────────────────────────────────────┘
```

#### 2.3.2 Erro: CNPJ Não Credenciado

```
┌────────────────────────────────────────────────────┐
│  ❌ CNPJ Não Credenciado na SEFAZ                  │
├────────────────────────────────────────────────────┤
│                                                    │
│  Seu CNPJ não está autorizado a emitir NF-e.      │
│                                                    │
│  Possíveis causas:                                 │
│  • Credenciamento não foi solicitado              │
│  • Inscrição Estadual (IE) inativa                │
│  • Pendências fiscais com a SEFAZ                 │
│  • Processo de credenciamento ainda em análise    │
│                                                    │
│  Como resolver (SEFAZ-SP):                         │
│                                                    │
│  1️⃣ Acesse o portal da SEFAZ:                     │
│     https://www.fazenda.sp.gov.br                  │
│                                                    │
│  2️⃣ Entre em "Serviços > NF-e > Credenciamento"  │
│                                                    │
│  3️⃣ Preencha o formulário de solicitação         │
│                                                    │
│  4️⃣ Aguarde aprovação (1-3 dias úteis)           │
│                                                    │
│  5️⃣ Volte ao PitStop e tente novamente            │
│                                                    │
│  ℹ️ Outros estados têm processos similares        │
│                                                    │
│  Precisa de ajuda?                                 │
│  [📞 Suporte PitStop] [📄 Tutorial SEFAZ] [Fechar]│
└────────────────────────────────────────────────────┘
```

#### 2.3.3 Erro: Senha Incorreta

```
┌────────────────────────────────────────────────────┐
│  ❌ Senha do Certificado Incorreta                 │
├────────────────────────────────────────────────────┤
│                                                    │
│  A senha informada não confere com o certificado.  │
│                                                    │
│  Detalhes:                                         │
│  • Tentativas restantes: 2 de 3                   │
│  • Após 3 tentativas, aguarde 30 minutos          │
│                                                    │
│  ⚠️ ATENÇÃO: Senhas incorretas podem bloquear     │
│  o certificado permanentemente!                    │
│                                                    │
│  Dicas:                                            │
│  • Verifique se o Caps Lock está desligado        │
│  • Certifique-se de usar a senha correta          │
│  • Contate quem instalou o certificado            │
│                                                    │
│  Esqueceu a senha?                                 │
│  • Entre em contato com a AC que emitiu           │
│  • Pode ser necessário renovar o certificado      │
│                                                    │
│  [Tentar Novamente] [Recuperar Senha] [Cancelar]  │
└────────────────────────────────────────────────────┘
```

#### 2.3.4 Erro: SEFAZ Indisponível

```
┌────────────────────────────────────────────────────┐
│  ⚠️ SEFAZ Temporariamente Indisponível             │
├────────────────────────────────────────────────────┤
│                                                    │
│  Não foi possível conectar aos servidores da       │
│  SEFAZ no momento.                                 │
│                                                    │
│  Detalhes técnicos:                                │
│  • Código: Timeout de conexão                     │
│  • Servidor: nfe.fazenda.sp.gov.br               │
│  • Tentativa: 3 de 3                              │
│                                                    │
│  Possíveis causas:                                 │
│  • Manutenção programada na SEFAZ                 │
│  • Instabilidade temporária                       │
│  • Problema na sua conexão de internet            │
│                                                    │
│  O que fazer:                                      │
│  1. Verifique sua conexão com a internet          │
│  2. Consulte o status da SEFAZ:                   │
│     http://www.nfe.fazenda.gov.br/portal/         │
│        disponibilidade.aspx                        │
│  3. Tente novamente em alguns minutos             │
│                                                    │
│  Você pode salvar as configurações e testar       │
│  a conexão depois.                                 │
│                                                    │
│  [Consultar Status SEFAZ] [Tentar Novamente]      │
│  [Salvar e Testar Depois]                          │
└────────────────────────────────────────────────────┘
```

### 2.4 Modelo de Negócio e Planos

#### 2.4.1 Sugestão de Preços

**Plano BÁSICO** - R$ 49/mês
- ❌ Sem NF-e
- ✅ Gestão de Ordem de Serviço
- ✅ Cadastro de clientes e veículos
- ✅ Relatórios básicos
- ✅ Até 100 OS/mês

**Plano PREMIUM** - R$ 149/mês
- ✅ **Emissão ilimitada de NF-e**
- ✅ DANFE automático por email
- ✅ Armazenamento de XMLs
- ✅ Relatórios fiscais
- ✅ Até 500 OS/mês
- ✅ Suporte prioritário

**Plano EMPRESARIAL** - R$ 399/mês
- ✅ Tudo do Premium
- ✅ Múltiplas filiais
- ✅ Dashboard consolidado
- ✅ API para integrações
- ✅ OS ilimitadas
- ✅ Gerente de conta dedicado

#### 2.4.2 Feature Toggle por Plano

```java
@Service
public class PlanService {

    public boolean podeEmitirNFe(UUID empresaId) {
        Empresa empresa = empresaRepository.findById(empresaId).orElseThrow();
        Plano plano = empresa.getPlano();

        return plano == Plano.PREMIUM || plano == Plano.EMPRESARIAL;
    }

    public boolean podeUsarMultiplasFiliais(UUID empresaId) {
        Empresa empresa = empresaRepository.findById(empresaId).orElseThrow();
        return empresa.getPlano() == Plano.EMPRESARIAL;
    }
}
```

**Tela de upgrade:**

```
┌────────────────────────────────────────────────────┐
│  🔒 Funcionalidade Exclusiva do Plano Premium      │
├────────────────────────────────────────────────────┤
│                                                    │
│  A emissão de Nota Fiscal Eletrônica (NF-e) está  │
│  disponível apenas nos planos Premium e            │
│  Empresarial.                                      │
│                                                    │
│  Seu plano atual: BÁSICO (R$ 49/mês)              │
│                                                    │
│  ✨ Upgrade para Premium e tenha:                  │
│  ✅ Emissão ilimitada de NF-e                     │
│  ✅ DANFE automático por email                    │
│  ✅ Relatórios fiscais completos                  │
│  ✅ Armazenamento seguro de XMLs                  │
│  ✅ Suporte prioritário                           │
│                                                    │
│  💰 Preço: R$ 149/mês                             │
│  🎁 Primeiro mês grátis!                           │
│                                                    │
│  [Ver Planos Completos] [Fazer Upgrade Agora]     │
│  [Continuar no Plano Básico]                       │
└────────────────────────────────────────────────────┘
```

### 2.5 Documentação para o Cliente

Criar guias em PDF/vídeo:

**"Preparando sua Oficina para Emitir NF-e"**

1. ✅ Tenha CNPJ ativo
2. ✅ Solicite Inscrição Estadual (SEFAZ)
3. ✅ Compre Certificado Digital e-CNPJ
4. ✅ Credenciar na SEFAZ para NF-e
5. ✅ Configure no PitStop
6. ✅ Teste em homologação
7. ✅ Ative em produção

**"Tutorial: Onde Comprar Certificado Digital"**
- Lista de Autoridades Certificadoras
- Comparativo de preços
- Passo a passo da compra

**"Como Credenciar na SEFAZ"**
- Tutorial específico por estado
- Screenshots do processo
- Documentos necessários

---

## 3. ARQUITETURA DA SOLUÇÃO

### 2.1 Estrutura de Módulos

```
src/main/java/com/pitstop/
├── notafiscal/                    # Novo módulo NF-e
│   ├── domain/                   # Entidades de domínio
│   │   ├── NotaFiscal.java
│   │   ├── ItemNotaFiscal.java
│   │   ├── StatusNFe.java (enum)
│   │   ├── ModeloNF.java (enum)
│   │   └── TipoEmissao.java (enum)
│   ├── repository/               # Repositórios JPA
│   │   ├── NotaFiscalRepository.java
│   │   └── ItemNotaFiscalRepository.java
│   ├── service/                  # Lógica de negócio
│   │   ├── NotaFiscalService.java
│   │   ├── NFeSefazService.java (integração SEFAZ)
│   │   ├── DanfeService.java (geração PDF)
│   │   └── CertificadoDigitalService.java
│   ├── controller/               # Endpoints REST
│   │   └── NotaFiscalController.java
│   └── dto/                      # DTOs de entrada/saída
│       ├── EmitirNFeRequest.java
│       ├── CancelarNFeRequest.java
│       ├── NotaFiscalResponseDTO.java
│       └── DanfeDTO.java
├── config/
│   └── NFePadrao.java            # Configurações padrão NF-e
└── shared/
    └── exception/
        └── NFeException.java     # Exceções específicas de NF-e
```

### 2.2 Dependências Maven

```xml
<!-- Java Nota Fiscal Eletrônica (biblioteca open-source) -->
<dependency>
    <groupId>br.com.swconsultoria</groupId>
    <artifactId>java-nfe</artifactId>
    <version>4.00.21</version>
</dependency>

<!-- Apache PDFBox para geração DANFE -->
<dependency>
    <groupId>org.apache.pdfbox</groupId>
    <artifactId>pdfbox</artifactId>
    <version>3.0.1</version>
</dependency>

<!-- JasperReports para DANFE (alternativa mais robusta) -->
<dependency>
    <groupId>net.sf.jasperreports</groupId>
    <artifactId>jasperreports</artifactId>
    <version>6.21.3</version>
</dependency>

<!-- Geração de código de barras no DANFE -->
<dependency>
    <groupId>net.sf.barcode4j</groupId>
    <artifactId>barcode4j</artifactId>
    <version>2.1</version>
</dependency>

<!-- BouncyCastle para certificados digitais -->
<dependency>
    <groupId>org.bouncycastle</groupId>
    <artifactId>bcprov-jdk18on</artifactId>
    <version>1.78.1</version>
</dependency>
<dependency>
    <groupId>org.bouncycastle</groupId>
    <artifactId>bcpkix-jdk18on</artifactId>
    <version>1.78.1</version>
</dependency>
```

---

## 3. MODELO DE DADOS

### 3.1 Entidades Principais

#### 3.1.1 NotaFiscal

```java
@Entity
@Table(name = "notas_fiscais")
public class NotaFiscal {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Relacionamentos
    @ManyToOne
    @JoinColumn(name = "ordem_servico_id")
    private OrdemServico ordemServico;

    @ManyToOne
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @ManyToOne
    @JoinColumn(name = "usuario_emitente_id")
    private Usuario usuarioEmitente;

    // Dados da NF-e
    private Long numero;              // Número da NF-e
    private Integer serie;            // Série da NF-e

    @Enumerated(EnumType.STRING)
    private ModeloNF modelo;          // 55 (NF-e)

    @Enumerated(EnumType.STRING)
    private StatusNFe status;         // DIGITACAO, AUTORIZADA, CANCELADA, REJEITADA

    private String chaveAcesso;       // 44 dígitos
    private Integer codigoNumerico;   // 8 dígitos aleatórios

    @Column(columnDefinition = "TEXT")
    private String xmlEnviado;        // XML enviado para SEFAZ

    @Column(columnDefinition = "TEXT")
    private String xmlAutorizado;     // XML retornado pela SEFAZ

    @Column(columnDefinition = "TEXT")
    private String xmlCancelamento;   // XML de cancelamento (se houver)

    private String protocoloAutorizacao;
    private LocalDateTime dataHoraAutorizacao;

    private String protocoloCancelamento;
    private LocalDateTime dataHoraCancelamento;
    private String justificativaCancelamento;

    // Valores
    private BigDecimal valorTotal;
    private BigDecimal valorProdutos;
    private BigDecimal valorServicos;
    private BigDecimal baseCalculoICMS;
    private BigDecimal valorICMS;
    private BigDecimal baseCalculoICMSST;
    private BigDecimal valorICMSST;

    // Impostos
    private BigDecimal valorPIS;
    private BigDecimal valorCOFINS;
    private BigDecimal valorIPI;
    private BigDecimal valorISS;

    // Totalizadores
    private BigDecimal valorDesconto;
    private BigDecimal valorOutrasDespesas;

    // Natureza da operação
    private String naturezaOperacao;  // Ex: "VENDA DE SERVIÇOS"

    // CFOP (Código Fiscal de Operações)
    private String cfop;              // Ex: "5933" (Prestação de serviço)

    // Informações complementares
    @Column(columnDefinition = "TEXT")
    private String informacoesComplementares;

    @Column(columnDefinition = "TEXT")
    private String informacoesFisco;

    // Datas
    private LocalDateTime dataEmissao;
    private LocalDateTime dataSaida;

    // Auditoria
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "notaFiscal", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemNotaFiscal> itens = new ArrayList<>();

    // Métodos de negócio
    public void adicionarItem(ItemNotaFiscal item) { ... }
    public void calcularTotais() { ... }
    public void autorizar(String chaveAcesso, String protocolo) { ... }
    public void cancelar(String protocolo, String justificativa) { ... }
}
```

#### 3.1.2 ItemNotaFiscal

```java
@Entity
@Table(name = "itens_nota_fiscal")
public class ItemNotaFiscal {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "nota_fiscal_id", nullable = false)
    private NotaFiscal notaFiscal;

    private Integer numeroItem;       // Sequencial dentro da NF-e

    @Enumerated(EnumType.STRING)
    private TipoItem tipo;            // PRODUTO, SERVICO

    // Produto/Serviço
    private String codigo;            // Código interno
    private String codigoEAN;         // EAN/GTIN (se produto)
    private String descricao;
    private String ncm;               // Nomenclatura Comum do Mercosul
    private String cest;              // Código Especificador da Substituição Tributária
    private String cfop;              // CFOP do item
    private String unidadeComercial;  // "UN", "PC", "KG", "HR"

    // Quantidades e valores
    private BigDecimal quantidade;
    private BigDecimal valorUnitario;
    private BigDecimal valorTotal;
    private BigDecimal valorDesconto;
    private BigDecimal valorOutrasDespesas;

    // Tributação ICMS
    @Enumerated(EnumType.STRING)
    private CstICMS cstICMS;          // 00, 10, 20, 30, 40, 41, 50, 51, 60, 70, 90

    private BigDecimal aliquotaICMS;
    private BigDecimal valorICMS;
    private BigDecimal aliquotaICMSST;
    private BigDecimal valorICMSST;

    // Tributação PIS/COFINS
    @Enumerated(EnumType.STRING)
    private CstPIS cstPIS;

    @Enumerated(EnumType.STRING)
    private CstCOFINS cstCOFINS;

    private BigDecimal aliquotaPIS;
    private BigDecimal valorPIS;
    private BigDecimal aliquotaCOFINS;
    private BigDecimal valorCOFINS;

    // Tributação IPI (se aplicável)
    private BigDecimal aliquotaIPI;
    private BigDecimal valorIPI;

    // Auditoria
    private LocalDateTime createdAt;
}
```

### 3.2 Enums

```java
public enum StatusNFe {
    DIGITACAO("Digitação"),
    VALIDADA("Validada"),
    ASSINADA("Assinada"),
    ENVIADA("Enviada para SEFAZ"),
    AUTORIZADA("Autorizada"),
    DENEGADA("Denegada"),
    REJEITADA("Rejeitada"),
    CANCELADA("Cancelada"),
    INUTILIZADA("Inutilizada");
}

public enum ModeloNF {
    NFE_55("55", "NF-e (modelo 55)"),
    NFCE_65("65", "NFC-e (modelo 65)");
}

public enum TipoEmissao {
    NORMAL("1", "Emissão normal"),
    CONTINGENCIA_FS_IA("2", "Contingência FS-IA"),
    SCAN("3", "Contingência SCAN"),
    EPEC("4", "Contingência EPEC"),
    CONTINGENCIA_FS_DA("5", "Contingência FS-DA"),
    CONTINGENCIA_SVC_AN("6", "Contingência SVC-AN"),
    CONTINGENCIA_SVC_RS("7", "Contingência SVC-RS"),
    OFFLINE("9", "Contingência off-line");
}

public enum CstICMS {
    CST_00("00", "Tributada integralmente"),
    CST_10("10", "Tributada e com cobrança do ICMS por substituição tributária"),
    CST_20("20", "Com redução de base de cálculo"),
    CST_30("30", "Isenta ou não tributada e com cobrança do ICMS por substituição tributária"),
    CST_40("40", "Isenta"),
    CST_41("41", "Não tributada"),
    CST_50("50", "Suspensão"),
    CST_51("51", "Diferimento"),
    CST_60("60", "ICMS cobrado anteriormente por substituição tributária"),
    CST_70("70", "Com redução de base de cálculo e cobrança do ICMS por substituição tributária"),
    CST_90("90", "Outros");
}
```

### 3.3 Migração Liquibase

```sql
-- V009__create_nota_fiscal_table.sql

CREATE TABLE notas_fiscais (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ordem_servico_id UUID REFERENCES ordem_servico(id),
    cliente_id UUID NOT NULL REFERENCES clientes(id),
    usuario_emitente_id UUID REFERENCES usuarios(id),

    numero BIGINT NOT NULL,
    serie INTEGER NOT NULL DEFAULT 1,
    modelo VARCHAR(2) NOT NULL DEFAULT '55',
    status VARCHAR(20) NOT NULL,

    chave_acesso VARCHAR(44) UNIQUE,
    codigo_numerico INTEGER,

    xml_enviado TEXT,
    xml_autorizado TEXT,
    xml_cancelamento TEXT,

    protocolo_autorizacao VARCHAR(50),
    data_hora_autorizacao TIMESTAMP,

    protocolo_cancelamento VARCHAR(50),
    data_hora_cancelamento TIMESTAMP,
    justificativa_cancelamento TEXT,

    valor_total DECIMAL(15,2) NOT NULL,
    valor_produtos DECIMAL(15,2) DEFAULT 0,
    valor_servicos DECIMAL(15,2) DEFAULT 0,
    base_calculo_icms DECIMAL(15,2) DEFAULT 0,
    valor_icms DECIMAL(15,2) DEFAULT 0,
    base_calculo_icms_st DECIMAL(15,2) DEFAULT 0,
    valor_icms_st DECIMAL(15,2) DEFAULT 0,
    valor_pis DECIMAL(15,2) DEFAULT 0,
    valor_cofins DECIMAL(15,2) DEFAULT 0,
    valor_ipi DECIMAL(15,2) DEFAULT 0,
    valor_iss DECIMAL(15,2) DEFAULT 0,
    valor_desconto DECIMAL(15,2) DEFAULT 0,
    valor_outras_despesas DECIMAL(15,2) DEFAULT 0,

    natureza_operacao VARCHAR(60) NOT NULL,
    cfop VARCHAR(4) NOT NULL,

    informacoes_complementares TEXT,
    informacoes_fisco TEXT,

    data_emissao TIMESTAMP NOT NULL,
    data_saida TIMESTAMP,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_nfe_numero_serie UNIQUE (numero, serie)
);

CREATE TABLE itens_nota_fiscal (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nota_fiscal_id UUID NOT NULL REFERENCES notas_fiscais(id) ON DELETE CASCADE,

    numero_item INTEGER NOT NULL,
    tipo VARCHAR(10) NOT NULL,

    codigo VARCHAR(60),
    codigo_ean VARCHAR(14),
    descricao VARCHAR(120) NOT NULL,
    ncm VARCHAR(8),
    cest VARCHAR(7),
    cfop VARCHAR(4) NOT NULL,
    unidade_comercial VARCHAR(6) NOT NULL,

    quantidade DECIMAL(15,4) NOT NULL,
    valor_unitario DECIMAL(15,4) NOT NULL,
    valor_total DECIMAL(15,2) NOT NULL,
    valor_desconto DECIMAL(15,2) DEFAULT 0,
    valor_outras_despesas DECIMAL(15,2) DEFAULT 0,

    cst_icms VARCHAR(3),
    aliquota_icms DECIMAL(5,2),
    valor_icms DECIMAL(15,2),
    aliquota_icms_st DECIMAL(5,2),
    valor_icms_st DECIMAL(15,2),

    cst_pis VARCHAR(2),
    cst_cofins VARCHAR(2),
    aliquota_pis DECIMAL(5,2),
    valor_pis DECIMAL(15,2),
    aliquota_cofins DECIMAL(5,2),
    valor_cofins DECIMAL(15,2),

    aliquota_ipi DECIMAL(5,2),
    valor_ipi DECIMAL(15,2),

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_item_nfe UNIQUE (nota_fiscal_id, numero_item)
);

CREATE INDEX idx_nfe_status ON notas_fiscais(status);
CREATE INDEX idx_nfe_data_emissao ON notas_fiscais(data_emissao);
CREATE INDEX idx_nfe_cliente ON notas_fiscais(cliente_id);
CREATE INDEX idx_nfe_ordem_servico ON notas_fiscais(ordem_servico_id);
CREATE INDEX idx_nfe_chave_acesso ON notas_fiscais(chave_acesso);

-- Sequence para numeração de NF-e
CREATE SEQUENCE nfe_numero_seq START WITH 1 INCREMENT BY 1;
```

---

## 4. CONFIGURAÇÕES E DADOS DA EMPRESA

### 4.1 Tabela de Configurações da Empresa

```sql
-- V010__create_empresa_configuracao.sql

CREATE TABLE empresa (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Dados cadastrais
    razao_social VARCHAR(100) NOT NULL,
    nome_fantasia VARCHAR(100),
    cnpj VARCHAR(14) NOT NULL UNIQUE,
    inscricao_estadual VARCHAR(14),
    inscricao_municipal VARCHAR(15),

    -- Endereço
    logradouro VARCHAR(60) NOT NULL,
    numero VARCHAR(10) NOT NULL,
    complemento VARCHAR(60),
    bairro VARCHAR(60) NOT NULL,
    codigo_municipio VARCHAR(7) NOT NULL,
    municipio VARCHAR(60) NOT NULL,
    uf VARCHAR(2) NOT NULL,
    cep VARCHAR(8) NOT NULL,
    codigo_pais VARCHAR(4) DEFAULT '1058',
    pais VARCHAR(60) DEFAULT 'Brasil',

    -- Contatos
    telefone VARCHAR(14),
    email VARCHAR(100) NOT NULL,

    -- Regime tributário
    regime_tributario VARCHAR(1) NOT NULL,  -- 1=Simples, 2=Simples excesso, 3=Normal

    -- CSOSN padrão (para Simples Nacional)
    csosn_padrao VARCHAR(3) DEFAULT '102',  -- 102=Sem tributação

    -- Certificado Digital
    tipo_certificado VARCHAR(2) NOT NULL,  -- A1 ou A3
    caminho_certificado VARCHAR(255),      -- Para A1 (.pfx)
    senha_certificado VARCHAR(100),        -- Criptografada
    data_validade_certificado DATE,

    -- Configurações NF-e
    serie_nfe INTEGER DEFAULT 1,
    ultimo_numero_nfe BIGINT DEFAULT 0,
    ambiente_nfe VARCHAR(1) DEFAULT '2',   -- 1=Produção, 2=Homologação

    -- Logo para DANFE
    logo_base64 TEXT,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Inserir dados iniciais da empresa (deve ser preenchido no setup inicial)
INSERT INTO empresa (
    razao_social,
    nome_fantasia,
    cnpj,
    inscricao_estadual,
    logradouro,
    numero,
    bairro,
    codigo_municipio,
    municipio,
    uf,
    cep,
    email,
    regime_tributario,
    tipo_certificado
) VALUES (
    'PITSTOP MECANICA LTDA',
    'PitStop Auto Center',
    '12345678000190',
    '123456789',
    'RUA DAS OFICINAS',
    '1000',
    'CENTRO',
    '3550308',
    'São Paulo',
    'SP',
    '01000000',
    'nfe@pitstop.com.br',
    '1',
    'A1'
);
```

### 4.2 Entidade Empresa

```java
@Entity
@Table(name = "empresa")
public class Empresa {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String razaoSocial;
    private String nomeFantasia;
    private String cnpj;
    private String inscricaoEstadual;
    private String inscricaoMunicipal;

    private String logradouro;
    private String numero;
    private String complemento;
    private String bairro;
    private String codigoMunicipio;
    private String municipio;
    private String uf;
    private String cep;

    private String telefone;
    private String email;

    private String regimeTributario;
    private String csosnPadrao;

    private String tipoCertificado;
    private String caminhoCertificado;
    private String senhaCertificado;
    private LocalDate dataValidadeCertificado;

    private Integer serieNfe;
    private Long ultimoNumeroNfe;
    private String ambienteNfe;

    @Column(columnDefinition = "TEXT")
    private String logoBase64;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

---

## 5. LÓGICA DE NEGÓCIO

### 5.1 Service: NotaFiscalService

```java
@Service
@Transactional
@Slf4j
public class NotaFiscalService {

    private final NotaFiscalRepository notaFiscalRepository;
    private final OrdemServicoRepository ordemServicoRepository;
    private final EmpresaRepository empresaRepository;
    private final NFeSefazService nfeSefazService;
    private final DanfeService danfeService;
    private final EmailService emailService;

    /**
     * Emite uma NF-e a partir de uma Ordem de Serviço finalizada.
     */
    public NotaFiscalResponseDTO emitirNFePorOrdemServico(UUID ordemServicoId) {
        log.info("Iniciando emissão de NF-e para OS: {}", ordemServicoId);

        // 1. Buscar Ordem de Serviço
        OrdemServico os = ordemServicoRepository.findById(ordemServicoId)
            .orElseThrow(() -> new BusinessException("Ordem de Serviço não encontrada"));

        // 2. Validações
        if (os.getStatus() != StatusOS.FINALIZADO && os.getStatus() != StatusOS.ENTREGUE) {
            throw new BusinessException("Apenas OS finalizadas podem gerar NF-e");
        }

        if (notaFiscalRepository.existsByOrdemServicoId(ordemServicoId)) {
            throw new BusinessException("Já existe uma NF-e para esta OS");
        }

        // 3. Buscar dados da empresa
        Empresa empresa = empresaRepository.findFirst()
            .orElseThrow(() -> new BusinessException("Dados da empresa não configurados"));

        // 4. Criar entidade NotaFiscal
        NotaFiscal nfe = new NotaFiscal();
        nfe.setOrdemServico(os);
        nfe.setCliente(os.getVeiculo().getCliente());
        nfe.setUsuarioEmitente(getCurrentUser());
        nfe.setNumero(empresaRepository.getProximoNumeroNFe());
        nfe.setSerie(empresa.getSerieNfe());
        nfe.setModelo(ModeloNF.NFE_55);
        nfe.setStatus(StatusNFe.DIGITACAO);
        nfe.setNaturezaOperacao("PRESTACAO DE SERVICOS");
        nfe.setCfop("5933"); // Prestação de serviço sujeito ao ICMS
        nfe.setDataEmissao(LocalDateTime.now());

        // 5. Adicionar itens da OS
        int numeroItem = 1;
        for (ItemOS itemOS : os.getItens()) {
            ItemNotaFiscal itemNfe = new ItemNotaFiscal();
            itemNfe.setNumeroItem(numeroItem++);
            itemNfe.setTipo(itemOS.getTipo());
            itemNfe.setDescricao(itemOS.getDescricao());
            itemNfe.setQuantidade(itemOS.getQuantidade());
            itemNfe.setValorUnitario(itemOS.getValorUnitario());
            itemNfe.setValorTotal(itemOS.getValorTotal());
            itemNfe.setUnidadeComercial(itemOS.getTipo() == TipoItem.SERVICO ? "HR" : "UN");
            itemNfe.setCfop("5933");
            itemNfe.setNcm(itemOS.getTipo() == TipoItem.SERVICO ? "00" : obterNCM(itemOS));

            // Tributação (Simples Nacional)
            itemNfe.setCstICMS(CstICMS.CST_102); // Sem tributação
            itemNfe.setCstPIS(CstPIS.CST_49);    // Outras operações
            itemNfe.setCstCOFINS(CstCOFINS.CST_49);

            nfe.adicionarItem(itemNfe);
        }

        // 6. Calcular totais
        nfe.calcularTotais();

        // 7. Salvar no banco
        nfe = notaFiscalRepository.save(nfe);

        // 8. Gerar XML e enviar para SEFAZ
        try {
            String xmlAssinado = nfeSefazService.gerarXML(nfe, empresa);
            nfe.setXmlEnviado(xmlAssinado);
            nfe.setStatus(StatusNFe.ASSINADA);

            // Enviar para SEFAZ
            RetornoSefaz retorno = nfeSefazService.enviarNFe(xmlAssinado, empresa);

            if (retorno.isAutorizada()) {
                nfe.setStatus(StatusNFe.AUTORIZADA);
                nfe.setChaveAcesso(retorno.getChaveAcesso());
                nfe.setProtocoloAutorizacao(retorno.getProtocolo());
                nfe.setDataHoraAutorizacao(retorno.getDataHora());
                nfe.setXmlAutorizado(retorno.getXmlRetorno());

                log.info("NF-e autorizada: {} - Chave: {}", nfe.getNumero(), nfe.getChaveAcesso());

                // Gerar DANFE
                byte[] danfePdf = danfeService.gerarDanfe(nfe);

                // Enviar por email
                emailService.enviarDanfe(nfe.getCliente().getEmail(), nfe, danfePdf);

            } else {
                nfe.setStatus(StatusNFe.REJEITADA);
                log.error("NF-e rejeitada: {} - Motivo: {}", nfe.getNumero(), retorno.getMotivo());
                throw new NFeException("NF-e rejeitada pela SEFAZ: " + retorno.getMotivo());
            }

        } catch (Exception e) {
            nfe.setStatus(StatusNFe.REJEITADA);
            log.error("Erro ao emitir NF-e", e);
            throw new NFeException("Erro ao emitir NF-e: " + e.getMessage());
        } finally {
            notaFiscalRepository.save(nfe);
        }

        return toResponseDTO(nfe);
    }

    /**
     * Cancela uma NF-e autorizada.
     */
    public void cancelarNFe(UUID nfeId, String justificativa) {
        NotaFiscal nfe = notaFiscalRepository.findById(nfeId)
            .orElseThrow(() -> new BusinessException("NF-e não encontrada"));

        // Validações
        if (nfe.getStatus() != StatusNFe.AUTORIZADA) {
            throw new BusinessException("Apenas NF-e autorizadas podem ser canceladas");
        }

        if (justificativa == null || justificativa.length() < 15) {
            throw new BusinessException("Justificativa deve ter no mínimo 15 caracteres");
        }

        LocalDateTime agora = LocalDateTime.now();
        Duration duracao = Duration.between(nfe.getDataHoraAutorizacao(), agora);
        if (duracao.toHours() > 24) {
            throw new BusinessException("NF-e só pode ser cancelada em até 24 horas após autorização");
        }

        // Enviar cancelamento para SEFAZ
        Empresa empresa = empresaRepository.findFirst().orElseThrow();

        try {
            RetornoSefaz retorno = nfeSefazService.cancelarNFe(
                nfe.getChaveAcesso(),
                nfe.getProtocoloAutorizacao(),
                justificativa,
                empresa
            );

            if (retorno.isCancelada()) {
                nfe.setStatus(StatusNFe.CANCELADA);
                nfe.setProtocoloCancelamento(retorno.getProtocolo());
                nfe.setDataHoraCancelamento(retorno.getDataHora());
                nfe.setJustificativaCancelamento(justificativa);
                nfe.setXmlCancelamento(retorno.getXmlRetorno());

                notaFiscalRepository.save(nfe);

                log.info("NF-e cancelada: {} - Protocolo: {}", nfe.getChaveAcesso(), retorno.getProtocolo());
            } else {
                throw new NFeException("Cancelamento rejeitado: " + retorno.getMotivo());
            }
        } catch (Exception e) {
            log.error("Erro ao cancelar NF-e", e);
            throw new NFeException("Erro ao cancelar NF-e: " + e.getMessage());
        }
    }
}
```

### 5.2 Service: NFeSefazService (Integração SEFAZ)

```java
@Service
@Slf4j
public class NFeSefazService {

    private final CertificadoDigitalService certificadoService;

    /**
     * Gera o XML da NF-e conforme layout 4.0.
     */
    public String gerarXML(NotaFiscal nfe, Empresa empresa) throws Exception {
        log.info("Gerando XML para NF-e: {}", nfe.getNumero());

        // Usar biblioteca java-nfe
        ConfiguracoesNfe config = configurarNFe(empresa);

        TNFe tnfe = new TNFe();

        // Identificação da NF-e
        TNFe.InfNFe infNfe = new TNFe.InfNFe();
        infNfe.setVersao("4.00");

        TNFe.InfNFe.Ide ide = new TNFe.InfNFe.Ide();
        ide.setCUF(getCodigoUF(empresa.getUf()));
        ide.setCNF(gerarCodigoNumerico());
        ide.setNatOp(nfe.getNaturezaOperacao());
        ide.setMod("55");
        ide.setNFe();  // Tipo 1 = Saída
        ide.setCMunFG(empresa.getCodigoMunicipio());
        ide.setTpImp("1");  // DANFE retrato
        ide.setTpEmis("1"); // Emissão normal
        ide.setTpAmb(empresa.getAmbienteNfe());
        ide.setFinNFe("1"); // Normal
        ide.setIndFinal("1"); // Consumidor final
        ide.setIndPres("1"); // Operação presencial
        ide.setProcEmi("0");  // Emissão por aplicação própria
        ide.setVerProc("1.0");

        ide.setDhEmi(formatarDataHora(nfe.getDataEmissao()));
        ide.setDhSaiEnt(formatarDataHora(nfe.getDataSaida()));

        // Emitente
        TNFe.InfNFe.Emit emit = criarEmitente(empresa);

        // Destinatário
        TNFe.InfNFe.Dest dest = criarDestinatario(nfe.getCliente());

        // Itens
        List<TNFe.InfNFe.Det> detalhes = new ArrayList<>();
        for (ItemNotaFiscal item : nfe.getItens()) {
            detalhes.add(criarItem(item));
        }

        // Totais
        TNFe.InfNFe.Total total = criarTotais(nfe);

        // Pagamento
        TNFe.InfNFe.Pag pag = criarPagamento(nfe);

        // Montar estrutura completa
        infNfe.setIde(ide);
        infNfe.setEmit(emit);
        infNfe.setDest(dest);
        infNfe.setDet(detalhes);
        infNfe.setTotal(total);
        infNfe.setPag(pag);

        tnfe.setInfNFe(infNfe);

        // Serializar para XML
        String xml = XmlUtil.objectToXml(tnfe);

        // Assinar digitalmente
        String xmlAssinado = AssinaturaDigital.assinar(xml, certificadoService.getCertificado(empresa));

        return xmlAssinado;
    }

    /**
     * Envia a NF-e para autorização na SEFAZ.
     */
    public RetornoSefaz enviarNFe(String xmlAssinado, Empresa empresa) throws Exception {
        log.info("Enviando NF-e para autorização na SEFAZ");

        ConfiguracoesNfe config = configurarNFe(empresa);

        // Enviar para SEFAZ
        TRetEnviNFe retorno = Nfe.enviarNfe(config, xmlAssinado, TipoDocumento.NFE);

        // Processar retorno
        RetornoSefaz resultado = new RetornoSefaz();

        if ("100".equals(retorno.getCStat())) {  // Autorizado o uso da NF-e
            resultado.setAutorizada(true);
            resultado.setChaveAcesso(retorno.getProtNFe().getInfProt().getChNFe());
            resultado.setProtocolo(retorno.getProtNFe().getInfProt().getNProt());
            resultado.setDataHora(parseDataHora(retorno.getProtNFe().getInfProt().getDhRecbto()));
            resultado.setXmlRetorno(XmlUtil.objectToXml(retorno));
        } else {
            resultado.setAutorizada(false);
            resultado.setCodigoStatus(retorno.getCStat());
            resultado.setMotivo(retorno.getXMotivo());
        }

        return resultado;
    }

    /**
     * Cancela uma NF-e autorizada.
     */
    public RetornoSefaz cancelarNFe(String chaveAcesso, String protocolo, String justificativa, Empresa empresa) {
        log.info("Cancelando NF-e: {}", chaveAcesso);

        try {
            ConfiguracoesNfe config = configurarNFe(empresa);

            TRetEvento retorno = Nfe.cancelarNfe(
                config,
                chaveAcesso,
                protocolo,
                justificativa
            );

            RetornoSefaz resultado = new RetornoSefaz();

            if ("135".equals(retorno.getCStat())) {  // Cancelamento homologado
                resultado.setCancelada(true);
                resultado.setProtocolo(retorno.getInfEvento().get(0).getNProt());
                resultado.setDataHora(parseDataHora(retorno.getInfEvento().get(0).getDhRegEvento()));
                resultado.setXmlRetorno(XmlUtil.objectToXml(retorno));
            } else {
                resultado.setCancelada(false);
                resultado.setMotivo(retorno.getXMotivo());
            }

            return resultado;

        } catch (Exception e) {
            throw new NFeException("Erro ao cancelar NF-e", e);
        }
    }

    private ConfiguracoesNfe configurarNFe(Empresa empresa) {
        ConfiguracoesNfe config = new ConfiguracoesNfe();
        config.setEstado(Estados.valueOf(empresa.getUf()));
        config.setAmbiente(empresa.getAmbienteNfe().equals("1") ?
            TipoAmbiente.PRODUCAO : TipoAmbiente.HOMOLOGACAO);
        config.setCertificado(certificadoService.getCertificado(empresa));
        return config;
    }
}
```

### 5.3 Service: DanfeService (Geração de PDF)

```java
@Service
@Slf4j
public class DanfeService {

    /**
     * Gera o DANFE (PDF) da NF-e.
     */
    public byte[] gerarDanfe(NotaFiscal nfe) {
        log.info("Gerando DANFE para NF-e: {}", nfe.getNumero());

        try {
            // Opção 1: Usar JasperReports (mais robusto)
            return gerarDanfeComJasper(nfe);

            // Opção 2: Usar PDFBox (mais simples)
            // return gerarDanfeComPdfBox(nfe);

        } catch (Exception e) {
            log.error("Erro ao gerar DANFE", e);
            throw new NFeException("Erro ao gerar DANFE: " + e.getMessage());
        }
    }

    private byte[] gerarDanfeComJasper(NotaFiscal nfe) throws Exception {
        // Template DANFE em JasperReports (.jrxml)
        InputStream template = getClass().getResourceAsStream("/templates/danfe.jrxml");
        JasperReport jasperReport = JasperCompileManager.compileReport(template);

        // Dados para o relatório
        Map<String, Object> params = new HashMap<>();
        params.put("CHAVE_ACESSO", nfe.getChaveAcesso());
        params.put("NUMERO_NFE", nfe.getNumero());
        params.put("SERIE", nfe.getSerie());
        params.put("DATA_EMISSAO", nfe.getDataEmissao());
        params.put("PROTOCOLO", nfe.getProtocoloAutorizacao());
        // ... demais campos

        // Lista de itens
        JRBeanCollectionDataSource itensDS = new JRBeanCollectionDataSource(nfe.getItens());

        // Gerar PDF
        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, params, itensDS);
        return JasperExportManager.exportReportToPdf(jasperPrint);
    }
}
```

---

## 6. API REST ENDPOINTS

### 6.1 NotaFiscalController

```java
@RestController
@RequestMapping("/api/notas-fiscais")
@SecurityRequirement(name = "bearer-auth")
public class NotaFiscalController {

    private final NotaFiscalService notaFiscalService;

    /**
     * Emite uma NF-e a partir de uma Ordem de Serviço.
     */
    @PostMapping("/emitir")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE')")
    public ResponseEntity<NotaFiscalResponseDTO> emitir(@RequestBody @Valid EmitirNFeRequest request) {
        NotaFiscalResponseDTO response = notaFiscalService.emitirNFePorOrdemServico(request.getOrdemServicoId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Lista todas as NF-e com filtros.
     */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE', 'MECANICO')")
    public ResponseEntity<Page<NotaFiscalResponseDTO>> listar(
        @RequestParam(required = false) StatusNFe status,
        @RequestParam(required = false) LocalDateTime dataInicio,
        @RequestParam(required = false) LocalDateTime dataFim,
        Pageable pageable
    ) {
        Page<NotaFiscalResponseDTO> page = notaFiscalService.listar(status, dataInicio, dataFim, pageable);
        return ResponseEntity.ok(page);
    }

    /**
     * Busca NF-e por ID.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE', 'MECANICO')")
    public ResponseEntity<NotaFiscalResponseDTO> buscarPorId(@PathVariable UUID id) {
        NotaFiscalResponseDTO response = notaFiscalService.buscarPorId(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Cancela uma NF-e.
     */
    @PostMapping("/{id}/cancelar")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE')")
    public ResponseEntity<Void> cancelar(
        @PathVariable UUID id,
        @RequestBody @Valid CancelarNFeRequest request
    ) {
        notaFiscalService.cancelarNFe(id, request.getJustificativa());
        return ResponseEntity.noContent().build();
    }

    /**
     * Download do XML da NF-e.
     */
    @GetMapping("/{id}/xml")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE')")
    public ResponseEntity<byte[]> downloadXml(@PathVariable UUID id) {
        byte[] xml = notaFiscalService.getXmlAutorizado(id);

        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_XML)
            .header("Content-Disposition", "attachment; filename=nfe-" + id + ".xml")
            .body(xml);
    }

    /**
     * Download do DANFE (PDF).
     */
    @GetMapping("/{id}/danfe")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE', 'MECANICO')")
    public ResponseEntity<byte[]> downloadDanfe(@PathVariable UUID id) {
        byte[] pdf = notaFiscalService.getDanfePdf(id);

        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header("Content-Disposition", "attachment; filename=danfe-" + id + ".pdf")
            .body(pdf);
    }

    /**
     * Reenvia DANFE por email.
     */
    @PostMapping("/{id}/reenviar-email")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE')")
    public ResponseEntity<Void> reenviarEmail(@PathVariable UUID id) {
        notaFiscalService.reenviarDanfePorEmail(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Consulta situação da NF-e na SEFAZ.
     */
    @GetMapping("/{id}/consultar-situacao")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE')")
    public ResponseEntity<SituacaoNFeDTO> consultarSituacao(@PathVariable UUID id) {
        SituacaoNFeDTO situacao = notaFiscalService.consultarSituacaoNaSefaz(id);
        return ResponseEntity.ok(situacao);
    }
}
```

---

## 7. FLUXO DE TRABALHO

### 7.1 Diagrama de Sequência - Emissão de NF-e

```
Cliente         Frontend        Backend         SEFAZ
  |                |               |              |
  |  Finaliza OS   |               |              |
  |--------------->|               |              |
  |                | POST /emitir  |              |
  |                |-------------->|              |
  |                |               | Valida dados |
  |                |               |------------->|
  |                |               | Gera XML     |
  |                |               |------------->|
  |                |               | Assina XML   |
  |                |               |------------->|
  |                |               | Envia SEFAZ  |
  |                |               |------------->|
  |                |               |              | Valida
  |                |               |              |-------->
  |                |               |    Protocolo |
  |                |               |<-------------|
  |                |               | Salva retorno|
  |                |               |------------->|
  |                |               | Gera DANFE   |
  |                |               |------------->|
  |                |               | Envia Email  |
  |                |               |------------->|
  |                |  200 OK + PDF |              |
  |                |<--------------|              |
  |  Recebe email  |               |              |
  |<---------------|               |              |
```

### 7.2 Estados da NF-e

```
DIGITACAO → VALIDADA → ASSINADA → ENVIADA → AUTORIZADA
                                        ↓
                                   REJEITADA

AUTORIZADA → CANCELADA (até 24h)
```

---

## 8. CONSIDERAÇÕES TÉCNICAS

### 8.1 Certificado Digital

**Tipos suportados:**
- **A1**: Arquivo .pfx armazenado no servidor (validade: 1 ano)
- **A3**: Token USB ou Smart Card (validade: 3 anos)

**Armazenamento seguro:**
- Senha do certificado deve ser criptografada no banco (usar BCrypt ou AES-256)
- Arquivo .pfx deve ter permissões restritas no sistema de arquivos
- Considerar usar AWS Secrets Manager ou Azure Key Vault em produção

### 8.2 Ambientes SEFAZ

- **Homologação (ambiente 2)**: Para testes, sem valor fiscal
- **Produção (ambiente 1)**: Notas com validade fiscal

**URLs dos Web Services variam por estado (UF).**

### 8.3 Tratamento de Erros

Principais códigos de retorno SEFAZ:
- **100**: Autorizado o uso da NF-e
- **101**: Cancelamento homologado
- **135**: Evento registrado e vinculado à NF-e
- **204**: Duplicidade de NF-e
- **539**: Certificado digital vencido
- **573**: CNPJ do emitente não cadastrado

### 8.4 Performance

- **Cache**: Armazenar dados da empresa em cache (Redis) para evitar queries repetidas
- **Async**: Envio para SEFAZ pode ser assíncrono usando `@Async` + fila (RabbitMQ/Kafka)
- **Retry**: Implementar retry automático em caso de timeout da SEFAZ

### 8.5 Segurança

- ✅ Apenas usuários autorizados (ADMIN, GERENTE, ATENDENTE) podem emitir NF-e
- ✅ Cancelamento requer perfil ADMIN ou GERENTE
- ✅ Logs de auditoria para todas operações fiscais
- ✅ XMLs devem ser armazenados por 5 anos (legislação)
- ✅ DANFE deve ser enviado ao cliente automaticamente

---

## 9. CONFIGURAÇÃO INICIAL

### 9.1 Checklist de Setup

- [ ] Cadastrar dados da empresa (CNPJ, IE, endereço)
- [ ] Instalar certificado digital A1 ou conectar token A3
- [ ] Configurar série da NF-e (padrão: 1)
- [ ] Definir regime tributário (Simples Nacional, Normal)
- [ ] Configurar CSOSN/CST padrão para itens
- [ ] Testar emissão em ambiente de homologação
- [ ] Solicitar credenciamento na SEFAZ (se necessário)
- [ ] Validar geração de DANFE
- [ ] Configurar envio de emails (SMTP)
- [ ] Backup automático dos XMLs

### 9.2 Tela de Configuração (Frontend)

```
┌─────────────────────────────────────────────────────┐
│  Configurações de Nota Fiscal Eletrônica           │
├─────────────────────────────────────────────────────┤
│  Dados da Empresa                                   │
│  ├─ Razão Social: [____________________________]   │
│  ├─ CNPJ: [__.____.___/____-__]                    │
│  ├─ Inscrição Estadual: [_____________]            │
│  └─ Regime Tributário: [Simples Nacional ▼]       │
│                                                     │
│  Certificado Digital                                │
│  ├─ Tipo: ( ) A1  (•) A3                          │
│  ├─ Arquivo .pfx: [Selecionar arquivo]            │
│  ├─ Senha: [**********]                            │
│  └─ Validade: 31/12/2025                           │
│                                                     │
│  Configurações de Emissão                          │
│  ├─ Série: [1]                                     │
│  ├─ Último número: [000000]                        │
│  ├─ Ambiente: ( ) Produção  (•) Homologação       │
│  └─ CSOSN padrão: [102 - Sem tributação]          │
│                                                     │
│  [ Testar Conexão SEFAZ ]  [ Salvar ]             │
└─────────────────────────────────────────────────────┘
```

---

## 10. ROADMAP DE IMPLEMENTAÇÃO

### Fase 1 - MVP (4 semanas)

**Semana 1:**
- [ ] Criar estrutura de banco de dados (migrations)
- [ ] Implementar entidades JPA (NotaFiscal, ItemNotaFiscal, Empresa)
- [ ] Configurar dependências Maven
- [ ] Criar repositórios

**Semana 2:**
- [ ] Implementar NFeSefazService (geração de XML)
- [ ] Integrar certificado digital
- [ ] Implementar envio para SEFAZ (ambiente homologação)
- [ ] Testes de autorização

**Semana 3:**
- [ ] Implementar NotaFiscalService (lógica de negócio)
- [ ] Criar endpoints REST
- [ ] Implementar validações
- [ ] Integração com OrdemServico

**Semana 4:**
- [ ] Implementar DanfeService (geração de PDF)
- [ ] Envio automático de email
- [ ] Tela de configurações (frontend)
- [ ] Tela de listagem e detalhes (frontend)
- [ ] Testes end-to-end

### Fase 2 - Funcionalidades Avançadas (2 semanas)

**Semana 5:**
- [ ] Cancelamento de NF-e
- [ ] Carta de Correção Eletrônica (CC-e)
- [ ] Consulta de situação na SEFAZ
- [ ] Download de XML e PDF

**Semana 6:**
- [ ] Relatórios fiscais (notas emitidas, canceladas)
- [ ] Dashboard de NF-e
- [ ] Exportação para Excel
- [ ] Backup automático de XMLs

### Fase 3 - Melhorias (1 semana)

**Semana 7:**
- [ ] Emissão assíncrona com fila
- [ ] Retry automático
- [ ] Logs de auditoria detalhados
- [ ] Inutilização de numeração
- [ ] Documentação completa

---

## 11. ESTIMATIVA DE CUSTOS

### 11.1 Licenças e Serviços

- **Certificado Digital A1**: R$ 150 - R$ 250/ano
- **Certificado Digital A3**: R$ 200 - R$ 400 (3 anos) + Token (R$ 100 - R$ 150)
- **Biblioteca java-nfe**: Open-source (gratuito)
- **SEFAZ Web Services**: Gratuito (serviço público)
- **Armazenamento de XMLs**: ~10 MB/ano por empresa (desprezível)

### 11.2 Esforço de Desenvolvimento

- **Backend**: 80 horas
- **Frontend**: 40 horas
- **Testes**: 30 horas
- **Documentação**: 10 horas
- **Total**: ~160 horas (4 semanas para 1 desenvolvedor full-time)

---

## 12. RISCOS E MITIGAÇÕES

| Risco | Impacto | Probabilidade | Mitigação |
|-------|---------|---------------|-----------|
| Certificado digital expirado | Alto | Média | Alertas automáticos 30 dias antes |
| SEFAZ indisponível | Alto | Baixa | Implementar contingência (FS-DA) |
| Erro na tributação | Alto | Média | Validação dupla + homologação rigorosa |
| XMLs corrompidos | Médio | Baixa | Backup diário + checksum |
| Legislação alterada | Alto | Baixa | Monitorar updates da biblioteca |

---

## 13. COMPLIANCE E LEGISLAÇÃO

### 13.1 Obrigações Legais

- ✅ Armazenar XMLs por 5 anos (Lei 12.682/2012)
- ✅ Enviar DANFE ao destinatário
- ✅ Informar chave de acesso em todas comunicações
- ✅ Cancelamento em até 24 horas após autorização
- ✅ CC-e em até 720 horas após autorização

### 13.2 Notas Técnicas Relevantes

- NT 2023.001: Layout 4.0 da NF-e
- NT 2021.001: Novas tags para produtos específicos
- NT 2020.005: CEST obrigatório para alguns NCMs

---

## 14. CONCLUSÃO

A implementação de NF-e no PitStop proporcionará:

1. **Conformidade fiscal** total com a legislação brasileira
2. **Automação** do processo de faturamento
3. **Integração perfeita** com o fluxo da Ordem de Serviço
4. **Rastreabilidade** completa das notas emitidas
5. **Redução de erros** humanos
6. **Agilidade** no envio de documentos fiscais aos clientes

O sistema seguirá as melhores práticas de desenvolvimento, com arquitetura modular, testes abrangentes e documentação completa.

---

## 15. ANEXOS

### Anexo A: Exemplo de XML NF-e (simplificado)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<nfeProc xmlns="http://www.portalfiscal.inf.br/nfe" versao="4.00">
  <NFe>
    <infNFe Id="NFe35210512345678000190550010000000011234567890" versao="4.00">
      <ide>
        <cUF>35</cUF>
        <cNF>12345678</cNF>
        <natOp>PRESTACAO DE SERVICOS</natOp>
        <mod>55</mod>
        <serie>1</serie>
        <nNF>1</nNF>
        <dhEmi>2025-11-01T14:30:00-03:00</dhEmi>
        <tpNF>1</tpNF>
        <idDest>1</idDest>
        <cMunFG>3550308</cMunFG>
        <tpImp>1</tpImp>
        <tpEmis>1</tpEmis>
        <tpAmb>2</tpAmb>
        <finNFe>1</finNFe>
        <indFinal>1</indFinal>
        <indPres>1</indPres>
      </ide>
      <!-- Demais tags -->
    </infNFe>
    <Signature><!-- Assinatura digital --></Signature>
  </NFe>
</nfeProc>
```

### Anexo B: Referências

- [Portal da NF-e](http://www.nfe.fazenda.gov.br/)
- [Biblioteca java-nfe](https://github.com/Samuel-Oliveira/Java_NFe)
- [Layout XML NF-e 4.0](http://www.nfe.fazenda.gov.br/portal/listaConteudo.aspx?tipoConteudo=/fk9xag5SOM=)
- [Manual de Orientação do Contribuinte](http://www.nfe.fazenda.gov.br/portal/exibirArquivo.aspx?conteudo=mXt2wCo1Ssc=)

---

**Documento revisado em:** 01/11/2025
**Versão:** 1.0
**Status:** Pronto para implementação
