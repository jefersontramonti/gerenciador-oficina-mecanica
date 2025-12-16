# 📋 Especificação Técnica Completa: Módulo de Configurações - PitStop

**Versão:** 1.0.0
**Data:** 01/12/2025
**Autor:** PitStop Development Team
**Status:** Especificação para Implementação Completa

---

## 📑 Índice

1. [Visão Geral](#1-visão-geral)
2. [Arquitetura do Sistema](#2-arquitetura-do-sistema)
3. [Modelo de Dados (Backend)](#3-modelo-de-dados-backend)
4. [Migrações de Banco de Dados](#4-migrações-de-banco-de-dados)
5. [Backend - Implementação Java/Spring Boot](#5-backend-implementação-javaspring-boot)
6. [Frontend - Implementação React/TypeScript](#6-frontend-implementação-reacttypescript)
7. [Integrações Externas](#7-integrações-externas)
8. [Segurança e RBAC](#8-segurança-e-rbac)
9. [Testes](#9-testes)
10. [Deploy e Configuração](#10-deploy-e-configuração)
11. [Roadmap de Implementação](#11-roadmap-de-implementação)

---

## 1. Visão Geral

### 1.1 Objetivo

O **Módulo de Configurações** do PitStop é responsável por centralizar todas as configurações do sistema em uma interface unificada, permitindo que administradores e usuários personalizem:

- **Perfil pessoal do usuário** (nome, senha, preferências)
- **Dados da oficina** (CNPJ, endereço, horário de funcionamento)
- **Regras de negócio** (workflow de OS, alertas de estoque, formas de pagamento)
- **Integrações externas** (Email SMTP, WhatsApp, Telegram, Mercado Pago)
- **Segurança** (políticas de senha, 2FA, logs de auditoria)
- **Sistema** (backup, manutenção, monitoramento)

### 1.2 Escopo Funcional

**9 Seções Principais:**

```
┌─────────────────────────────────────────────────┐
│  Configurações                                  │
├──────────────┬──────────────────────────────────┤
│              │                                  │
│ 👤 Perfil    │   [Conteúdo da seção ativa]     │
│ 🏢 Oficina   │                                  │
│ 🔧 OS        │                                  │
│ 📦 Estoque   │                                  │
│ 💰 Financeiro│                                  │
│ 🔔 Notif.    │                                  │
│ 🔌 Integ.    │                                  │
│ 🔒 Segurança │                                  │
│ ⚙️  Sistema  │                                  │
└──────────────┴──────────────────────────────────┘
```

### 1.3 Tecnologias Utilizadas

**Backend:**
- Java 25 + Spring Boot 3.5.7
- Spring Data JPA + PostgreSQL 16
- Liquibase (migrações)
- MapStruct (DTOs)
- Spring Mail (SMTP)
- Twilio SDK (WhatsApp)
- Telegram Bots API
- Mercado Pago SDK

**Frontend:**
- React 19 + TypeScript 5.9
- React Query 5.62 (server state)
- React Hook Form 7.54 + Zod 3.24
- Tailwind CSS 4.0
- Axios 1.7.9

**Integrações:**
- SMTP (Gmail, AWS SES, etc.)
- Twilio WhatsApp Business API
- Evolution API (WhatsApp self-hosted)
- Telegram Bot API
- Mercado Pago API
- ViaCEP API (consulta CEP)

---

## 2. Arquitetura do Sistema

### 2.1 Arquitetura de Alto Nível

```
┌─────────────────────────────────────────────────────────────┐
│                     FRONTEND (React)                        │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐        │
│  │ Configurações│  │   Hooks     │  │  Services   │        │
│  │    Pages    │→ │ React Query │→ │  API Calls  │        │
│  └─────────────┘  └─────────────┘  └─────────────┘        │
└──────────────────────────┬──────────────────────────────────┘
                           │ HTTP/REST (JSON)
                           ↓
┌─────────────────────────────────────────────────────────────┐
│                  BACKEND (Spring Boot)                      │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │ Controllers  │→ │   Services   │→ │ Repositories │     │
│  │  (REST API)  │  │ (Business)   │  │    (JPA)     │     │
│  └──────────────┘  └──────────────┘  └──────────────┘     │
│                           │                                 │
│                           ↓                                 │
│  ┌──────────────────────────────────────────────────┐     │
│  │         Integration Services                     │     │
│  │  ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐           │     │
│  │  │ SMTP │ │Twilio│ │Telegr│ │ MPago│           │     │
│  │  └──────┘ └──────┘ └──────┘ └──────┘           │     │
│  └──────────────────────────────────────────────────┘     │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ↓
                  ┌─────────────────┐
                  │   PostgreSQL    │
                  │   Database      │
                  └─────────────────┘
```

### 2.2 Modelo de Persistência

Utilizaremos **abordagem híbrida** para armazenar configurações:

**Option 1: Tabela Chave-Valor (Flexível)**
- Melhor para configurações dinâmicas e plugins futuros
- Schema-less (adicionar novas configs sem migração)
- Usado para: preferências de usuário, configurações opcionais

**Option 2: Entidades Tipadas (Type-Safe)**
- Melhor para dados estruturados e validações
- Type-safe com Java
- Usado para: dados da oficina, configurações de integração

**Decisão de Design:**
- **ConfiguracaoSistema** (entidade principal - type-safe)
- **ConfiguracaoPersonalizada** (chave-valor - flexível)
- **Oficina** (entidade existente - reutilizar)

---

## 3. Modelo de Dados (Backend)

### 3.1 Diagrama ER

```
┌─────────────────────────┐
│      Usuario            │
│ (existing table)        │
├─────────────────────────┤
│ id: UUID (PK)          │
│ nome: String           │
│ email: String          │
│ senha: String          │
│ perfil: PerfilUsuario  │
│ ativo: Boolean         │
│ avatar_url: String     │──┐
│ created_at: Timestamp  │  │
│ updated_at: Timestamp  │  │
└─────────────────────────┘  │
                             │
                             │ 1:1
                             ↓
┌──────────────────────────────────────┐
│  PreferenciaUsuario                  │
│  (new table)                         │
├──────────────────────────────────────┤
│ id: UUID (PK)                        │
│ usuario_id: UUID (FK) UNIQUE         │
│ tema: String (LIGHT, DARK, AUTO)     │
│ idioma: String (PT_BR, EN_US)        │
│ densidade: String (COMPACT, NORMAL)  │
│ dashboard_style: String              │
│ notif_email: Boolean                 │
│ notif_push: Boolean                  │
│ notif_whatsapp: Boolean              │
│ created_at: Timestamp                │
│ updated_at: Timestamp                │
└──────────────────────────────────────┘

┌──────────────────────────────────────┐
│         Oficina                      │
│  (existing table - enhance)          │
├──────────────────────────────────────┤
│ id: UUID (PK)                        │
│ nome_fantasia: String                │
│ razao_social: String                 │
│ cnpj_cpf: String                     │
│ inscricao_estadual: String           │
│ inscricao_municipal: String          │
│ regime_tributario: Enum              │
│ tipo_pessoa: Enum                    │
│ // Embedded Objects:                 │
│ endereco: Endereco                   │
│ contato: Contato                     │
│ redes_sociais: RedesSociais          │
│ horario_funcionamento: Horario       │
│ dados_bancarios: DadosBancarios      │
│ logotipo_url: String                 │
│ created_at: Timestamp                │
│ updated_at: Timestamp                │
└──────────────────────────────────────┘

┌──────────────────────────────────────┐
│  ConfiguracaoSistema                 │
│  (new table - singleton)             │
├──────────────────────────────────────┤
│ id: UUID (PK)                        │
│ // Embedded Objects:                 │
│ ordem_servico: ConfigOS              │
│ estoque: ConfigEstoque               │
│ financeiro: ConfigFinanceiro         │
│ notificacao: ConfigNotificacao       │
│ seguranca: ConfigSeguranca           │
│ sistema: ConfigSistema               │
│ created_at: Timestamp                │
│ updated_at: Timestamp                │
└──────────────────────────────────────┘

┌──────────────────────────────────────┐
│  IntegracaoExterna                   │
│  (new table)                         │
├──────────────────────────────────────┤
│ id: UUID (PK)                        │
│ tipo: Enum (EMAIL, WHATSAPP, etc)    │
│ ativa: Boolean                       │
│ provedor: String (TWILIO, GMAIL)     │
│ configuracao: JSONB (encrypted)      │
│ ultima_conexao: Timestamp            │
│ status: String (OK, ERROR)           │
│ created_at: Timestamp                │
│ updated_at: Timestamp                │
└──────────────────────────────────────┘

┌──────────────────────────────────────┐
│  LogAuditoria                        │
│  (new table - append-only)           │
├──────────────────────────────────────┤
│ id: UUID (PK)                        │
│ usuario_id: UUID (FK)                │
│ acao: String (LOGIN, UPDATE_CONFIG)  │
│ entidade: String (CONFIGURACAO)      │
│ entidade_id: UUID                    │
│ dados_antes: JSONB                   │
│ dados_depois: JSONB                  │
│ ip_address: String                   │
│ user_agent: String                   │
│ created_at: Timestamp                │
└──────────────────────────────────────┘
```

### 3.2 Entidades JPA Completas

#### 3.2.1 PreferenciaUsuario.java

```java
package com.pitstop.configuracao.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidade que representa as preferências pessoais de um usuário.
 *
 * Relação 1:1 com Usuario.
 */
@Entity
@Table(
    name = "preferencias_usuario",
    indexes = {
        @Index(name = "idx_pref_usuario_id", columnList = "usuario_id")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_pref_usuario", columnNames = {"usuario_id"})
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class PreferenciaUsuario implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * ID do usuário (FK para usuarios.id)
     */
    @Column(name = "usuario_id", nullable = false, unique = true)
    @NotNull(message = "Usuário é obrigatório")
    private UUID usuarioId;

    /**
     * Tema da interface: LIGHT, DARK, AUTO
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "tema", nullable = false, length = 10)
    @Builder.Default
    private Tema tema = Tema.LIGHT;

    /**
     * Idioma: PT_BR, EN_US (preparado para i18n)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "idioma", nullable = false, length = 10)
    @Builder.Default
    private Idioma idioma = Idioma.PT_BR;

    /**
     * Densidade da interface: COMPACT, NORMAL
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "densidade", nullable = false, length = 10)
    @Builder.Default
    private Densidade densidade = Densidade.NORMAL;

    /**
     * Estilo do dashboard: CARDS, MINIMAL
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "dashboard_style", nullable = false, length = 20)
    @Builder.Default
    private DashboardStyle dashboardStyle = DashboardStyle.CARDS;

    /**
     * Receber notificações por email
     */
    @Column(name = "notif_email", nullable = false)
    @Builder.Default
    private Boolean notifEmail = true;

    /**
     * Receber notificações push no navegador
     */
    @Column(name = "notif_push", nullable = false)
    @Builder.Default
    private Boolean notifPush = true;

    /**
     * Receber notificações via WhatsApp
     */
    @Column(name = "notif_whatsapp", nullable = false)
    @Builder.Default
    private Boolean notifWhatsApp = false;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Enums
    public enum Tema {
        LIGHT("Claro"),
        DARK("Escuro"),
        AUTO("Automático");

        private final String descricao;

        Tema(String descricao) {
            this.descricao = descricao;
        }

        public String getDescricao() {
            return descricao;
        }
    }

    public enum Idioma {
        PT_BR("Português (Brasil)"),
        EN_US("English (US)");

        private final String descricao;

        Idioma(String descricao) {
            this.descricao = descricao;
        }

        public String getDescricao() {
            return descricao;
        }
    }

    public enum Densidade {
        COMPACT("Compacto"),
        NORMAL("Normal");

        private final String descricao;

        Densidade(String descricao) {
            this.descricao = descricao;
        }

        public String getDescricao() {
            return descricao;
        }
    }

    public enum DashboardStyle {
        CARDS("Cards"),
        MINIMAL("Minimalista");

        private final String descricao;

        DashboardStyle(String descricao) {
            this.descricao = descricao;
        }

        public String getDescricao() {
            return descricao;
        }
    }
}
```

#### 3.2.2 ConfiguracaoSistema.java

```java
package com.pitstop.configuracao.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidade singleton que armazena todas as configurações globais do sistema.
 *
 * IMPORTANTE: Deve existir apenas 1 registro desta tabela.
 * Use @PrePersist para validar singularidade.
 */
@Entity
@Table(name = "configuracao_sistema")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class ConfiguracaoSistema implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    // =====================================
    // Configurações de Ordem de Serviço
    // =====================================
    @Embedded
    @Builder.Default
    private ConfiguracaoOS ordemServico = new ConfiguracaoOS();

    // =====================================
    // Configurações de Estoque
    // =====================================
    @Embedded
    @Builder.Default
    private ConfiguracaoEstoque estoque = new ConfiguracaoEstoque();

    // =====================================
    // Configurações Financeiras
    // =====================================
    @Embedded
    @Builder.Default
    private ConfiguracaoFinanceiro financeiro = new ConfiguracaoFinanceiro();

    // =====================================
    // Configurações de Notificação
    // =====================================
    @Embedded
    @Builder.Default
    private ConfiguracaoNotificacao notificacao = new ConfiguracaoNotificacao();

    // =====================================
    // Configurações de Segurança
    // =====================================
    @Embedded
    @Builder.Default
    private ConfiguracaoSeguranca seguranca = new ConfiguracaoSeguranca();

    // =====================================
    // Configurações de Sistema
    // =====================================
    @Embedded
    @Builder.Default
    private ConfiguracaoSistemaTecnico sistema = new ConfiguracaoSistemaTecnico();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Valida que existe apenas 1 registro desta entidade.
     */
    @PrePersist
    public void validarSingleton() {
        // TODO: Implementar validação para garantir singleton
    }
}
```

#### 3.2.3 ConfiguracaoOS.java (Embedded)

```java
package com.pitstop.configuracao.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Configurações de Ordens de Serviço (embedded em ConfiguracaoSistema).
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfiguracaoOS implements Serializable {

    private static final long serialVersionUID = 1L;

    // ===== Numeração =====

    @Enumerated(EnumType.STRING)
    @Column(name = "os_formato_numero", length = 20)
    @Builder.Default
    private FormatoNumeroOS formatoNumero = FormatoNumeroOS.SEQUENCIAL;

    @Column(name = "os_prefixo", length = 10)
    @Builder.Default
    private String prefixo = "OS-";

    @Column(name = "os_proximo_numero")
    @Builder.Default
    private Long proximoNumero = 1L;

    // ===== Workflow =====

    @Column(name = "os_aprovacao_obrigatoria")
    @Builder.Default
    private Boolean aprovacaoObrigatoria = true;

    @Column(name = "os_exigir_pagamento_entrega")
    @Builder.Default
    private Boolean exigirPagamentoEntrega = true;

    @Column(name = "os_permitir_cancelamento_apos_inicio")
    @Builder.Default
    private Boolean permitirCancelamentoAposInicio = false;

    // ===== Valores Padrão =====

    @Column(name = "os_desconto_maximo_percentual", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal descontoMaximoPercentual = new BigDecimal("15.00");

    @Column(name = "os_prazo_conclusao_dias")
    @Builder.Default
    private Integer prazoConclusaoDias = 3;

    // ===== Alertas =====

    @Column(name = "os_alerta_atrasadas_email")
    @Builder.Default
    private Boolean alertaAtrasadasEmail = true;

    @Column(name = "os_alerta_atrasadas_sistema")
    @Builder.Default
    private Boolean alertaAtrasadasSistema = true;

    @Column(name = "os_alerta_aguardando_peca_dias")
    @Builder.Default
    private Integer alertaAguardandoPecaDias = 5;

    // ===== Campos Obrigatórios =====

    @Column(name = "os_exigir_problemas_relatados")
    @Builder.Default
    private Boolean exigirProblemasRelatados = true;

    @Column(name = "os_exigir_diagnostico")
    @Builder.Default
    private Boolean exigirDiagnostico = true;

    @Column(name = "os_permitir_edicao_finalizada")
    @Builder.Default
    private Boolean permitirEdicaoFinalizada = false;

    // Enums

    public enum FormatoNumeroOS {
        SEQUENCIAL("1, 2, 3..."),
        COM_ANO("2025-001"),
        COM_MES("2025-01-001");

        private final String exemplo;

        FormatoNumeroOS(String exemplo) {
            this.exemplo = exemplo;
        }

        public String getExemplo() {
            return exemplo;
        }
    }
}
```

#### 3.2.4 ConfiguracaoEstoque.java (Embedded)

```java
package com.pitstop.configuracao.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.*;

import java.io.Serializable;

/**
 * Configurações de Estoque (embedded em ConfiguracaoSistema).
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfiguracaoEstoque implements Serializable {

    private static final long serialVersionUID = 1L;

    // ===== Alertas de Estoque Baixo =====

    @Enumerated(EnumType.STRING)
    @Column(name = "estoque_criterio_alerta", length = 30)
    @Builder.Default
    private CriterioAlerta criterioAlerta = CriterioAlerta.QUANTIDADE_MINIMA;

    @Column(name = "estoque_antecedencia_alerta_dias")
    @Builder.Default
    private Integer antecedenciaAlertaDias = 7;

    @Column(name = "estoque_alerta_email")
    @Builder.Default
    private Boolean alertaEmail = true;

    @Column(name = "estoque_alerta_sistema")
    @Builder.Default
    private Boolean alertaSistema = true;

    @Column(name = "estoque_alerta_whatsapp")
    @Builder.Default
    private Boolean alertaWhatsApp = false;

    // ===== Movimentações =====

    @Column(name = "estoque_exigir_motivo_ajuste")
    @Builder.Default
    private Boolean exigirMotivoAjuste = true;

    @Column(name = "estoque_permitir_negativo")
    @Builder.Default
    private Boolean permitirEstoqueNegativo = false;

    @Column(name = "estoque_confirmacao_dupla_saida")
    @Builder.Default
    private Boolean confirmacaoDuplaSaida = false;

    // ===== Locais de Armazenamento =====

    @Column(name = "estoque_obrigar_localizacao")
    @Builder.Default
    private Boolean obrigarLocalizacao = false;

    @Column(name = "estoque_niveis_hierarquia")
    @Builder.Default
    private Integer niveisHierarquia = 3;

    // ===== Inventário =====

    @Enumerated(EnumType.STRING)
    @Column(name = "estoque_frequencia_inventario", length = 20)
    @Builder.Default
    private FrequenciaInventario frequenciaInventario = FrequenciaInventario.TRIMESTRAL;

    // Enums

    public enum CriterioAlerta {
        QUANTIDADE_MINIMA("Quantidade Mínima Cadastrada"),
        PERCENTUAL_MAXIMO("Percentual do Estoque Máximo");

        private final String descricao;

        CriterioAlerta(String descricao) {
            this.descricao = descricao;
        }

        public String getDescricao() {
            return descricao;
        }
    }

    public enum FrequenciaInventario {
        MENSAL("Mensal"),
        TRIMESTRAL("Trimestral"),
        SEMESTRAL("Semestral"),
        ANUAL("Anual");

        private final String descricao;

        FrequenciaInventario(String descricao) {
            this.descricao = descricao;
        }

        public String getDescricao() {
            return descricao;
        }
    }
}
```

#### 3.2.5 ConfiguracaoFinanceiro.java (Embedded)

```java
package com.pitstop.configuracao.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Configurações Financeiras (embedded em ConfiguracaoSistema).
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfiguracaoFinanceiro implements Serializable {

    private static final long serialVersionUID = 1L;

    // ===== Formas de Pagamento =====

    @Column(name = "fin_aceita_dinheiro")
    @Builder.Default
    private Boolean aceitaDinheiro = true;

    @Column(name = "fin_aceita_cartao_credito")
    @Builder.Default
    private Boolean aceitaCartaoCredito = true;

    @Column(name = "fin_taxa_cartao_credito", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal taxaCartaoCredito = new BigDecimal("2.99");

    @Column(name = "fin_aceita_cartao_debito")
    @Builder.Default
    private Boolean aceitaCartaoDebito = true;

    @Column(name = "fin_taxa_cartao_debito", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal taxaCartaoDebito = new BigDecimal("1.99");

    @Column(name = "fin_aceita_pix")
    @Builder.Default
    private Boolean aceitaPix = true;

    @Column(name = "fin_aceita_transferencia")
    @Builder.Default
    private Boolean aceitaTransferencia = true;

    @Column(name = "fin_aceita_boleto")
    @Builder.Default
    private Boolean aceitaBoleto = false;

    @Column(name = "fin_boleto_dias_vencimento")
    @Builder.Default
    private Integer boletoDiasVencimento = 3;

    @Column(name = "fin_aceita_cheque")
    @Builder.Default
    private Boolean aceitaCheque = false;

    // ===== Parcelamento =====

    @Column(name = "fin_max_parcelas")
    @Builder.Default
    private Integer maxParcelas = 6;

    @Column(name = "fin_juros_parcelamento", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal jurosParcelamento = BigDecimal.ZERO; // 0% = sem juros

    // ===== Descontos =====

    @Column(name = "fin_desconto_maximo_percentual", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal descontoMaximoPercentual = new BigDecimal("10.00");

    @Enumerated(EnumType.STRING)
    @Column(name = "fin_desconto_aprovacao_perfil", length = 20)
    @Builder.Default
    private PerfilAprovacao descontoAprovacaoPerfil = PerfilAprovacao.ADMIN;

    @Column(name = "fin_desconto_a_vista_percentual", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal descontoAVistaPercentual = new BigDecimal("5.00");

    // ===== Margem de Lucro =====

    @Column(name = "fin_margem_padrao_pecas", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal margemPadraoPecas = new BigDecimal("30.00");

    @Column(name = "fin_margem_minima_pecas", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal margemMinimaPecas = new BigDecimal("10.00");

    // ===== Notas Fiscais (Preparado para Fase 3) =====

    @Column(name = "fin_emitir_nfe_automatico")
    @Builder.Default
    private Boolean emitirNfeAutomatico = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "fin_nfe_tipo_padrao", length = 10)
    @Builder.Default
    private TipoNF nfeTipoPadrao = TipoNF.NFE;

    @Column(name = "fin_nfe_serie_padrao")
    @Builder.Default
    private Integer nfeSeriePadrao = 1;

    @Column(name = "fin_nfe_natureza_operacao", length = 60)
    @Builder.Default
    private String nfeNaturezaOperacao = "Prestação de Serviços";

    @Column(name = "fin_nfe_cfop_padrao", length = 4)
    @Builder.Default
    private String nfeCfopPadrao = "5933";

    // Enums

    public enum PerfilAprovacao {
        ADMIN("Apenas Administrador"),
        GERENTE("Administrador ou Gerente");

        private final String descricao;

        PerfilAprovacao(String descricao) {
            this.descricao = descricao;
        }

        public String getDescricao() {
            return descricao;
        }
    }

    public enum TipoNF {
        NFE("NF-e - Nota Fiscal Eletrônica"),
        NFSE("NFS-e - Nota Fiscal de Serviço"),
        NFCE("NFC-e - Nota Fiscal de Consumidor");

        private final String descricao;

        TipoNF(String descricao) {
            this.descricao = descricao;
        }

        public String getDescricao() {
            return descricao;
        }
    }
}
```

#### 3.2.6 ConfiguracaoNotificacao.java (Embedded)

```java
package com.pitstop.configuracao.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.*;

import java.io.Serializable;
import java.time.LocalTime;

/**
 * Configurações de Notificações (embedded em ConfiguracaoSistema).
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfiguracaoNotificacao implements Serializable {

    private static final long serialVersionUID = 1L;

    // ===== Horário de Envio =====

    @Column(name = "notif_horario_inicio")
    @Builder.Default
    private LocalTime horarioInicio = LocalTime.of(8, 0); // 08:00

    @Column(name = "notif_horario_fim")
    @Builder.Default
    private LocalTime horarioFim = LocalTime.of(20, 0); // 20:00

    // ===== Agrupamento =====

    @Enumerated(EnumType.STRING)
    @Column(name = "notif_agrupamento_email", length = 20)
    @Builder.Default
    private AgrupamentoEmail agrupamentoEmail = AgrupamentoEmail.IMEDIATO;

    @Column(name = "notif_resumo_diario_horario")
    @Builder.Default
    private LocalTime resumoDiarioHorario = LocalTime.of(18, 0); // 18:00

    // ===== Frequência de Alertas Repetidos =====

    @Enumerated(EnumType.STRING)
    @Column(name = "notif_frequencia_repeticao", length = 20)
    @Builder.Default
    private FrequenciaRepeticao frequenciaRepeticao = FrequenciaRepeticao.NAO_REPETIR;

    // ===== Notificações de Ordem de Serviço =====

    @Column(name = "notif_os_criada_email")
    @Builder.Default
    private Boolean osCriadaEmail = true;

    @Column(name = "notif_os_criada_sistema")
    @Builder.Default
    private Boolean osCriadaSistema = true;

    @Column(name = "notif_os_criada_whatsapp")
    @Builder.Default
    private Boolean osCriadaWhatsApp = false;

    @Column(name = "notif_os_criada_telegram")
    @Builder.Default
    private Boolean osCriadaTelegram = false;

    @Column(name = "notif_os_finalizada_email")
    @Builder.Default
    private Boolean osFinalizadaEmail = true;

    @Column(name = "notif_os_finalizada_sistema")
    @Builder.Default
    private Boolean osFinalizadaSistema = true;

    @Column(name = "notif_os_finalizada_whatsapp")
    @Builder.Default
    private Boolean osFinalizadaWhatsApp = true;

    @Column(name = "notif_os_finalizada_telegram")
    @Builder.Default
    private Boolean osFinalizadaTelegram = false;

    // ===== Notificações de Estoque =====

    @Column(name = "notif_estoque_baixo_email")
    @Builder.Default
    private Boolean estoqueBaixoEmail = true;

    @Column(name = "notif_estoque_baixo_sistema")
    @Builder.Default
    private Boolean estoqueBaixoSistema = true;

    @Column(name = "notif_estoque_baixo_whatsapp")
    @Builder.Default
    private Boolean estoqueBaixoWhatsApp = false;

    @Column(name = "notif_estoque_baixo_telegram")
    @Builder.Default
    private Boolean estoqueBaixoTelegram = false;

    // ===== Notificações Financeiras =====

    @Column(name = "notif_pagamento_recebido_email")
    @Builder.Default
    private Boolean pagamentoRecebidoEmail = true;

    @Column(name = "notif_pagamento_recebido_sistema")
    @Builder.Default
    private Boolean pagamentoRecebidoSistema = true;

    @Column(name = "notif_pagamento_recebido_whatsapp")
    @Builder.Default
    private Boolean pagamentoRecebidoWhatsApp = false;

    // Enums

    public enum AgrupamentoEmail {
        IMEDIATO("Imediato"),
        RESUMO_DIARIO("Resumo Diário");

        private final String descricao;

        AgrupamentoEmail(String descricao) {
            this.descricao = descricao;
        }

        public String getDescricao() {
            return descricao;
        }
    }

    public enum FrequenciaRepeticao {
        NAO_REPETIR("Não Repetir"),
        A_CADA_24H("A cada 24 horas"),
        A_CADA_48H("A cada 48 horas");

        private final String descricao;

        FrequenciaRepeticao(String descricao) {
            this.descricao = descricao;
        }

        public String getDescricao() {
            return descricao;
        }
    }
}
```

#### 3.2.7 ConfiguracaoSeguranca.java (Embedded)

```java
package com.pitstop.configuracao.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.*;

import java.io.Serializable;

/**
 * Configurações de Segurança (embedded em ConfiguracaoSistema).
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfiguracaoSeguranca implements Serializable {

    private static final long serialVersionUID = 1L;

    // ===== Autenticação =====

    @Column(name = "seg_tempo_sessao_minutos")
    @Builder.Default
    private Integer tempoSessaoMinutos = 15; // Access token

    @Column(name = "seg_refresh_token_dias")
    @Builder.Default
    private Integer refreshTokenDias = 7;

    @Column(name = "seg_exigir_2fa")
    @Builder.Default
    private Boolean exigir2FA = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "seg_2fa_metodo", length = 20)
    @Builder.Default
    private Metodo2FA metodo2FA = Metodo2FA.EMAIL;

    // ===== Políticas de Senha =====

    @Column(name = "seg_senha_tamanho_minimo")
    @Builder.Default
    private Integer senhaTamanhoMinimo = 8;

    @Column(name = "seg_senha_exigir_maiuscula")
    @Builder.Default
    private Boolean senhaExigirMaiuscula = true;

    @Column(name = "seg_senha_exigir_minuscula")
    @Builder.Default
    private Boolean senhaExigirMinuscula = true;

    @Column(name = "seg_senha_exigir_numero")
    @Builder.Default
    private Boolean senhaExigirNumero = true;

    @Column(name = "seg_senha_exigir_especial")
    @Builder.Default
    private Boolean senhaExigirEspecial = true;

    @Column(name = "seg_senha_expiracao_dias")
    @Builder.Default
    private Integer senhaExpiracaoDias = 0; // 0 = nunca expira

    @Column(name = "seg_senha_historico")
    @Builder.Default
    private Integer senhaHistorico = 3; // Não reutilizar últimas 3 senhas

    // ===== Bloqueio de Conta =====

    @Column(name = "seg_tentativas_login")
    @Builder.Default
    private Integer tentativasLogin = 5;

    @Enumerated(EnumType.STRING)
    @Column(name = "seg_duracao_bloqueio", length = 20)
    @Builder.Default
    private DuracaoBloqueio duracaoBloqueio = DuracaoBloqueio.MIN_30;

    // ===== Logs de Auditoria =====

    @Column(name = "seg_registrar_logins")
    @Builder.Default
    private Boolean registrarLogins = true;

    @Column(name = "seg_registrar_alteracoes_os")
    @Builder.Default
    private Boolean registrarAlteracoesOS = true;

    @Column(name = "seg_registrar_exclusoes")
    @Builder.Default
    private Boolean registrarExclusoes = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "seg_retencao_logs", length = 20)
    @Builder.Default
    private RetencaoLogs retencaoLogs = RetencaoLogs.DIAS_90;

    // ===== IP Whitelist (Futuro) =====

    @Column(name = "seg_ip_whitelist_ativo")
    @Builder.Default
    private Boolean ipWhitelistAtivo = false;

    @Column(name = "seg_ip_whitelist", columnDefinition = "TEXT")
    private String ipWhitelist; // Lista separada por vírgulas

    // Enums

    public enum Metodo2FA {
        EMAIL("Email"),
        SMS("SMS"),
        AUTHENTICATOR("App Authenticator");

        private final String descricao;

        Metodo2FA(String descricao) {
            this.descricao = descricao;
        }

        public String getDescricao() {
            return descricao;
        }
    }

    public enum DuracaoBloqueio {
        MIN_15("15 minutos"),
        MIN_30("30 minutos"),
        HORA_1("1 hora"),
        PERMANENTE("Permanente (requer desbloqueio manual)");

        private final String descricao;

        DuracaoBloqueio(String descricao) {
            this.descricao = descricao;
        }

        public String getDescricao() {
            return descricao;
        }
    }

    public enum RetencaoLogs {
        DIAS_30("30 dias"),
        DIAS_90("90 dias"),
        ANO_1("1 ano"),
        PERMANENTE("Permanente");

        private final String descricao;

        RetencaoLogs(String descricao) {
            this.descricao = descricao;
        }

        public String getDescricao() {
            return descricao;
        }
    }
}
```

#### 3.2.8 ConfiguracaoSistemaTecnico.java (Embedded)

```java
package com.pitstop.configuracao.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Configurações Técnicas do Sistema (embedded em ConfiguracaoSistema).
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfiguracaoSistemaTecnico implements Serializable {

    private static final long serialVersionUID = 1L;

    // ===== Informações (Somente Leitura) =====

    @Column(name = "sys_versao", length = 20)
    @Builder.Default
    private String versao = "1.0.0-MVP";

    @Enumerated(EnumType.STRING)
    @Column(name = "sys_ambiente", length = 20)
    @Builder.Default
    private Ambiente ambiente = Ambiente.DEVELOPMENT;

    @Column(name = "sys_ultima_atualizacao")
    private LocalDateTime ultimaAtualizacao;

    // ===== Manutenção =====

    @Column(name = "sys_modo_manutencao")
    @Builder.Default
    private Boolean modoManutencao = false;

    @Column(name = "sys_mensagem_manutencao", columnDefinition = "TEXT")
    @Builder.Default
    private String mensagemManutencao = "Sistema em manutenção. Voltamos em breve.";

    // ===== Backup =====

    @Column(name = "sys_ultimo_backup")
    private LocalDateTime ultimoBackup;

    @Column(name = "sys_proximo_backup")
    private LocalDateTime proximoBackup;

    @Enumerated(EnumType.STRING)
    @Column(name = "sys_frequencia_backup", length = 20)
    @Builder.Default
    private FrequenciaBackup frequenciaBackup = FrequenciaBackup.DIARIO;

    // ===== Limpeza Automática =====

    @Column(name = "sys_deletar_logs_dias")
    @Builder.Default
    private Integer deletarLogsDias = 90;

    @Column(name = "sys_deletar_notificacoes_dias")
    @Builder.Default
    private Integer deletarNotificacoesDias = 30;

    // ===== Performance =====

    @Column(name = "sys_pool_conexoes_db")
    @Builder.Default
    private Integer poolConexoesDB = 20;

    @Column(name = "sys_cache_ttl_segundos")
    @Builder.Default
    private Integer cacheTTLSegundos = 3600; // 1 hora

    @Column(name = "sys_timeout_requisicoes_segundos")
    @Builder.Default
    private Integer timeoutRequisicoesSegundos = 30;

    // ===== Logs e Monitoramento =====

    @Enumerated(EnumType.STRING)
    @Column(name = "sys_nivel_log", length = 10)
    @Builder.Default
    private NivelLog nivelLog = NivelLog.INFO;

    @Enumerated(EnumType.STRING)
    @Column(name = "sys_destino_log", length = 20)
    @Builder.Default
    private DestinoLog destinoLog = DestinoLog.AMBOS;

    @Column(name = "sys_prometheus_ativo")
    @Builder.Default
    private Boolean prometheusAtivo = false;

    // Enums

    public enum Ambiente {
        DEVELOPMENT("Desenvolvimento"),
        STAGING("Homologação"),
        PRODUCTION("Produção");

        private final String descricao;

        Ambiente(String descricao) {
            this.descricao = descricao;
        }

        public String getDescricao() {
            return descricao;
        }
    }

    public enum FrequenciaBackup {
        DIARIO("Diário"),
        SEMANAL("Semanal"),
        MENSAL("Mensal");

        private final String descricao;

        FrequenciaBackup(String descricao) {
            this.descricao = descricao;
        }

        public String getDescricao() {
            return descricao;
        }
    }

    public enum NivelLog {
        TRACE("Trace"),
        DEBUG("Debug"),
        INFO("Info"),
        WARN("Warn"),
        ERROR("Error");

        private final String descricao;

        NivelLog(String descricao) {
            this.descricao = descricao;
        }

        public String getDescricao() {
            return descricao;
        }
    }

    public enum DestinoLog {
        ARQUIVO("Arquivo"),
        CONSOLE("Console"),
        AMBOS("Ambos");

        private final String descricao;

        DestinoLog(String descricao) {
            this.descricao = descricao;
        }

        public String getDescricao() {
            return descricao;
        }
    }
}
```

#### 3.2.9 IntegracaoExterna.java

```java
package com.pitstop.configuracao.domain;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.Type;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Entidade que armazena configurações de integrações externas.
 *
 * Configuração armazenada em JSONB (criptografado).
 * Suporta múltiplas integrações do mesmo tipo (ex: 2 contas Twilio).
 */
@Entity
@Table(
    name = "integracoes_externas",
    indexes = {
        @Index(name = "idx_int_tipo", columnList = "tipo"),
        @Index(name = "idx_int_ativa", columnList = "ativa")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class IntegracaoExterna implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * Tipo de integração: EMAIL, WHATSAPP, TELEGRAM, MERCADOPAGO
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 20)
    @NotNull(message = "Tipo de integração é obrigatório")
    private TipoIntegracao tipo;

    /**
     * Status: ativa ou inativa
     */
    @Column(name = "ativa", nullable = false)
    @Builder.Default
    private Boolean ativa = true;

    /**
     * Provedor: TWILIO, EVOLUTION_API, GMAIL, AWS_SES, etc.
     */
    @Column(name = "provedor", length = 50)
    @NotBlank(message = "Provedor é obrigatório")
    private String provedor;

    /**
     * Configuração em formato JSON (CRIPTOGRAFADO).
     *
     * Exemplo para TWILIO:
     * {
     *   "accountSid": "ACxxxx",
     *   "authToken": "encrypted_token",
     *   "phoneNumber": "+5511999999999"
     * }
     */
    @Type(JsonBinaryType.class)
    @Column(name = "configuracao", columnDefinition = "jsonb")
    private Map<String, Object> configuracao;

    /**
     * Última conexão bem-sucedida
     */
    @Column(name = "ultima_conexao")
    private LocalDateTime ultimaConexao;

    /**
     * Status da integração: OK, ERROR, PENDING
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    @Builder.Default
    private StatusIntegracao status = StatusIntegracao.PENDING;

    /**
     * Mensagem de erro (se houver)
     */
    @Column(name = "erro", columnDefinition = "TEXT")
    private String erro;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Enums

    public enum TipoIntegracao {
        EMAIL("Email (SMTP)"),
        WHATSAPP("WhatsApp Business"),
        TELEGRAM("Telegram Bot"),
        MERCADOPAGO("Mercado Pago"),
        CLOUD_STORAGE("Cloud Storage (S3, GDrive)");

        private final String descricao;

        TipoIntegracao(String descricao) {
            this.descricao = descricao;
        }

        public String getDescricao() {
            return descricao;
        }
    }

    public enum StatusIntegracao {
        OK("Funcionando"),
        ERROR("Erro"),
        PENDING("Pendente de Teste");

        private final String descricao;

        StatusIntegracao(String descricao) {
            this.descricao = descricao;
        }

        public String getDescricao() {
            return descricao;
        }
    }
}
```

#### 3.2.10 LogAuditoria.java

```java
package com.pitstop.configuracao.domain;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Entidade append-only para logs de auditoria.
 *
 * Registra todas as ações importantes no sistema:
 * - Logins
 * - Alterações em configurações
 * - Exclusões de registros
 * - Mudanças de status em OS
 */
@Entity
@Table(
    name = "logs_auditoria",
    indexes = {
        @Index(name = "idx_audit_usuario", columnList = "usuario_id"),
        @Index(name = "idx_audit_acao", columnList = "acao"),
        @Index(name = "idx_audit_created", columnList = "created_at")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class LogAuditoria implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * ID do usuário que realizou a ação
     */
    @Column(name = "usuario_id")
    private UUID usuarioId;

    /**
     * Nome do usuário (desnormalizado para histórico)
     */
    @Column(name = "usuario_nome", length = 100)
    private String usuarioNome;

    /**
     * Ação realizada: LOGIN, UPDATE_CONFIG, DELETE_CLIENTE, etc.
     */
    @Column(name = "acao", nullable = false, length = 50)
    private String acao;

    /**
     * Tipo de entidade afetada: CONFIGURACAO, CLIENTE, OS, etc.
     */
    @Column(name = "entidade", length = 50)
    private String entidade;

    /**
     * ID da entidade afetada
     */
    @Column(name = "entidade_id")
    private UUID entidadeId;

    /**
     * Dados ANTES da alteração (JSON)
     */
    @Type(JsonBinaryType.class)
    @Column(name = "dados_antes", columnDefinition = "jsonb")
    private Map<String, Object> dadosAntes;

    /**
     * Dados DEPOIS da alteração (JSON)
     */
    @Type(JsonBinaryType.class)
    @Column(name = "dados_depois", columnDefinition = "jsonb")
    private Map<String, Object> dadosDepois;

    /**
     * Endereço IP de origem
     */
    @Column(name = "ip_address", length = 45) // IPv6 suportado
    private String ipAddress;

    /**
     * User-Agent (navegador/dispositivo)
     */
    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    /**
     * Observações adicionais
     */
    @Column(name = "observacao", columnDefinition = "TEXT")
    private String observacao;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
```

---

## 4. Migrações de Banco de Dados

### 4.1 Migration V026 - Create Preferencias Usuario

```sql
-- =====================================================
-- Migration: V026 - Create preferencias_usuario table
-- Description: Personal user preferences (theme, language, etc)
-- Author: PitStop Team
-- Date: 2025-12-01
-- =====================================================

-- Create preferencias_usuario table
CREATE TABLE preferencias_usuario (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id UUID NOT NULL,
    tema VARCHAR(10) NOT NULL DEFAULT 'LIGHT' CHECK (tema IN ('LIGHT', 'DARK', 'AUTO')),
    idioma VARCHAR(10) NOT NULL DEFAULT 'PT_BR' CHECK (idioma IN ('PT_BR', 'EN_US')),
    densidade VARCHAR(10) NOT NULL DEFAULT 'NORMAL' CHECK (densidade IN ('COMPACT', 'NORMAL')),
    dashboard_style VARCHAR(20) NOT NULL DEFAULT 'CARDS' CHECK (dashboard_style IN ('CARDS', 'MINIMAL')),
    notif_email BOOLEAN NOT NULL DEFAULT TRUE,
    notif_push BOOLEAN NOT NULL DEFAULT TRUE,
    notif_whatsapp BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Foreign key
    CONSTRAINT fk_pref_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE,

    -- Unique constraint (1:1 relationship)
    CONSTRAINT uk_pref_usuario UNIQUE (usuario_id)
);

-- Create index
CREATE INDEX idx_pref_usuario_id ON preferencias_usuario(usuario_id);

-- Add comment
COMMENT ON TABLE preferencias_usuario IS 'Preferências pessoais de cada usuário (tema, idioma, notificações)';
```

### 4.2 Migration V027 - Create Configuracao Sistema

```sql
-- =====================================================
-- Migration: V027 - Create configuracao_sistema table
-- Description: Global system configuration (singleton)
-- Author: PitStop Team
-- Date: 2025-12-01
-- =====================================================

-- Create configuracao_sistema table
CREATE TABLE configuracao_sistema (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- ===== Ordem de Serviço =====
    os_formato_numero VARCHAR(20) DEFAULT 'SEQUENCIAL' CHECK (os_formato_numero IN ('SEQUENCIAL', 'COM_ANO', 'COM_MES')),
    os_prefixo VARCHAR(10) DEFAULT 'OS-',
    os_proximo_numero BIGINT DEFAULT 1,
    os_aprovacao_obrigatoria BOOLEAN DEFAULT TRUE,
    os_exigir_pagamento_entrega BOOLEAN DEFAULT TRUE,
    os_permitir_cancelamento_apos_inicio BOOLEAN DEFAULT FALSE,
    os_desconto_maximo_percentual DECIMAL(5,2) DEFAULT 15.00,
    os_prazo_conclusao_dias INTEGER DEFAULT 3,
    os_alerta_atrasadas_email BOOLEAN DEFAULT TRUE,
    os_alerta_atrasadas_sistema BOOLEAN DEFAULT TRUE,
    os_alerta_aguardando_peca_dias INTEGER DEFAULT 5,
    os_exigir_problemas_relatados BOOLEAN DEFAULT TRUE,
    os_exigir_diagnostico BOOLEAN DEFAULT TRUE,
    os_permitir_edicao_finalizada BOOLEAN DEFAULT FALSE,

    -- ===== Estoque =====
    estoque_criterio_alerta VARCHAR(30) DEFAULT 'QUANTIDADE_MINIMA' CHECK (estoque_criterio_alerta IN ('QUANTIDADE_MINIMA', 'PERCENTUAL_MAXIMO')),
    estoque_antecedencia_alerta_dias INTEGER DEFAULT 7,
    estoque_alerta_email BOOLEAN DEFAULT TRUE,
    estoque_alerta_sistema BOOLEAN DEFAULT TRUE,
    estoque_alerta_whatsapp BOOLEAN DEFAULT FALSE,
    estoque_exigir_motivo_ajuste BOOLEAN DEFAULT TRUE,
    estoque_permitir_negativo BOOLEAN DEFAULT FALSE,
    estoque_confirmacao_dupla_saida BOOLEAN DEFAULT FALSE,
    estoque_obrigar_localizacao BOOLEAN DEFAULT FALSE,
    estoque_niveis_hierarquia INTEGER DEFAULT 3,
    estoque_frequencia_inventario VARCHAR(20) DEFAULT 'TRIMESTRAL' CHECK (estoque_frequencia_inventario IN ('MENSAL', 'TRIMESTRAL', 'SEMESTRAL', 'ANUAL')),

    -- ===== Financeiro =====
    fin_aceita_dinheiro BOOLEAN DEFAULT TRUE,
    fin_aceita_cartao_credito BOOLEAN DEFAULT TRUE,
    fin_taxa_cartao_credito DECIMAL(5,2) DEFAULT 2.99,
    fin_aceita_cartao_debito BOOLEAN DEFAULT TRUE,
    fin_taxa_cartao_debito DECIMAL(5,2) DEFAULT 1.99,
    fin_aceita_pix BOOLEAN DEFAULT TRUE,
    fin_aceita_transferencia BOOLEAN DEFAULT TRUE,
    fin_aceita_boleto BOOLEAN DEFAULT FALSE,
    fin_boleto_dias_vencimento INTEGER DEFAULT 3,
    fin_aceita_cheque BOOLEAN DEFAULT FALSE,
    fin_max_parcelas INTEGER DEFAULT 6,
    fin_juros_parcelamento DECIMAL(5,2) DEFAULT 0.00,
    fin_desconto_maximo_percentual DECIMAL(5,2) DEFAULT 10.00,
    fin_desconto_aprovacao_perfil VARCHAR(20) DEFAULT 'ADMIN' CHECK (fin_desconto_aprovacao_perfil IN ('ADMIN', 'GERENTE')),
    fin_desconto_a_vista_percentual DECIMAL(5,2) DEFAULT 5.00,
    fin_margem_padrao_pecas DECIMAL(5,2) DEFAULT 30.00,
    fin_margem_minima_pecas DECIMAL(5,2) DEFAULT 10.00,
    fin_emitir_nfe_automatico BOOLEAN DEFAULT FALSE,
    fin_nfe_tipo_padrao VARCHAR(10) DEFAULT 'NFE' CHECK (fin_nfe_tipo_padrao IN ('NFE', 'NFSE', 'NFCE')),
    fin_nfe_serie_padrao INTEGER DEFAULT 1,
    fin_nfe_natureza_operacao VARCHAR(60) DEFAULT 'Prestação de Serviços',
    fin_nfe_cfop_padrao VARCHAR(4) DEFAULT '5933',

    -- ===== Notificação =====
    notif_horario_inicio TIME DEFAULT '08:00:00',
    notif_horario_fim TIME DEFAULT '20:00:00',
    notif_agrupamento_email VARCHAR(20) DEFAULT 'IMEDIATO' CHECK (notif_agrupamento_email IN ('IMEDIATO', 'RESUMO_DIARIO')),
    notif_resumo_diario_horario TIME DEFAULT '18:00:00',
    notif_frequencia_repeticao VARCHAR(20) DEFAULT 'NAO_REPETIR' CHECK (notif_frequencia_repeticao IN ('NAO_REPETIR', 'A_CADA_24H', 'A_CADA_48H')),
    notif_os_criada_email BOOLEAN DEFAULT TRUE,
    notif_os_criada_sistema BOOLEAN DEFAULT TRUE,
    notif_os_criada_whatsapp BOOLEAN DEFAULT FALSE,
    notif_os_criada_telegram BOOLEAN DEFAULT FALSE,
    notif_os_finalizada_email BOOLEAN DEFAULT TRUE,
    notif_os_finalizada_sistema BOOLEAN DEFAULT TRUE,
    notif_os_finalizada_whatsapp BOOLEAN DEFAULT TRUE,
    notif_os_finalizada_telegram BOOLEAN DEFAULT FALSE,
    notif_estoque_baixo_email BOOLEAN DEFAULT TRUE,
    notif_estoque_baixo_sistema BOOLEAN DEFAULT TRUE,
    notif_estoque_baixo_whatsapp BOOLEAN DEFAULT FALSE,
    notif_estoque_baixo_telegram BOOLEAN DEFAULT FALSE,
    notif_pagamento_recebido_email BOOLEAN DEFAULT TRUE,
    notif_pagamento_recebido_sistema BOOLEAN DEFAULT TRUE,
    notif_pagamento_recebido_whatsapp BOOLEAN DEFAULT FALSE,

    -- ===== Segurança =====
    seg_tempo_sessao_minutos INTEGER DEFAULT 15,
    seg_refresh_token_dias INTEGER DEFAULT 7,
    seg_exigir_2fa BOOLEAN DEFAULT FALSE,
    seg_2fa_metodo VARCHAR(20) DEFAULT 'EMAIL' CHECK (seg_2fa_metodo IN ('EMAIL', 'SMS', 'AUTHENTICATOR')),
    seg_senha_tamanho_minimo INTEGER DEFAULT 8,
    seg_senha_exigir_maiuscula BOOLEAN DEFAULT TRUE,
    seg_senha_exigir_minuscula BOOLEAN DEFAULT TRUE,
    seg_senha_exigir_numero BOOLEAN DEFAULT TRUE,
    seg_senha_exigir_especial BOOLEAN DEFAULT TRUE,
    seg_senha_expiracao_dias INTEGER DEFAULT 0,
    seg_senha_historico INTEGER DEFAULT 3,
    seg_tentativas_login INTEGER DEFAULT 5,
    seg_duracao_bloqueio VARCHAR(20) DEFAULT 'MIN_30' CHECK (seg_duracao_bloqueio IN ('MIN_15', 'MIN_30', 'HORA_1', 'PERMANENTE')),
    seg_registrar_logins BOOLEAN DEFAULT TRUE,
    seg_registrar_alteracoes_os BOOLEAN DEFAULT TRUE,
    seg_registrar_exclusoes BOOLEAN DEFAULT TRUE,
    seg_retencao_logs VARCHAR(20) DEFAULT 'DIAS_90' CHECK (seg_retencao_logs IN ('DIAS_30', 'DIAS_90', 'ANO_1', 'PERMANENTE')),
    seg_ip_whitelist_ativo BOOLEAN DEFAULT FALSE,
    seg_ip_whitelist TEXT,

    -- ===== Sistema =====
    sys_versao VARCHAR(20) DEFAULT '1.0.0-MVP',
    sys_ambiente VARCHAR(20) DEFAULT 'DEVELOPMENT' CHECK (sys_ambiente IN ('DEVELOPMENT', 'STAGING', 'PRODUCTION')),
    sys_ultima_atualizacao TIMESTAMP,
    sys_modo_manutencao BOOLEAN DEFAULT FALSE,
    sys_mensagem_manutencao TEXT DEFAULT 'Sistema em manutenção. Voltamos em breve.',
    sys_ultimo_backup TIMESTAMP,
    sys_proximo_backup TIMESTAMP,
    sys_frequencia_backup VARCHAR(20) DEFAULT 'DIARIO' CHECK (sys_frequencia_backup IN ('DIARIO', 'SEMANAL', 'MENSAL')),
    sys_deletar_logs_dias INTEGER DEFAULT 90,
    sys_deletar_notificacoes_dias INTEGER DEFAULT 30,
    sys_pool_conexoes_db INTEGER DEFAULT 20,
    sys_cache_ttl_segundos INTEGER DEFAULT 3600,
    sys_timeout_requisicoes_segundos INTEGER DEFAULT 30,
    sys_nivel_log VARCHAR(10) DEFAULT 'INFO' CHECK (sys_nivel_log IN ('TRACE', 'DEBUG', 'INFO', 'WARN', 'ERROR')),
    sys_destino_log VARCHAR(20) DEFAULT 'AMBOS' CHECK (sys_destino_log IN ('ARQUIVO', 'CONSOLE', 'AMBOS')),
    sys_prometheus_ativo BOOLEAN DEFAULT FALSE,

    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Add comment
COMMENT ON TABLE configuracao_sistema IS 'Configurações globais do sistema (singleton - deve ter apenas 1 registro)';

-- Insert default configuration (singleton)
INSERT INTO configuracao_sistema (id) VALUES (gen_random_uuid());
```

### 4.3 Migration V028 - Create Integracoes Externas

```sql
-- =====================================================
-- Migration: V028 - Create integracoes_externas table
-- Description: External integrations (Email, WhatsApp, Telegram, etc)
-- Author: PitStop Team
-- Date: 2025-12-01
-- =====================================================

-- Create integracoes_externas table
CREATE TABLE integracoes_externas (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tipo VARCHAR(20) NOT NULL CHECK (tipo IN ('EMAIL', 'WHATSAPP', 'TELEGRAM', 'MERCADOPAGO', 'CLOUD_STORAGE')),
    ativa BOOLEAN NOT NULL DEFAULT TRUE,
    provedor VARCHAR(50) NOT NULL,
    configuracao JSONB, -- Configuração criptografada
    ultima_conexao TIMESTAMP,
    status VARCHAR(20) DEFAULT 'PENDING' CHECK (status IN ('OK', 'ERROR', 'PENDING')),
    erro TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes
CREATE INDEX idx_int_tipo ON integracoes_externas(tipo);
CREATE INDEX idx_int_ativa ON integracoes_externas(ativa);

-- Add comment
COMMENT ON TABLE integracoes_externas IS 'Configurações de integrações externas (Email SMTP, WhatsApp, Telegram, Mercado Pago)';
COMMENT ON COLUMN integracoes_externas.configuracao IS 'Configuração em formato JSON (deve ser criptografada na aplicação)';
```

### 4.4 Migration V029 - Create Logs Auditoria

```sql
-- =====================================================
-- Migration: V029 - Create logs_auditoria table
-- Description: Audit logs (append-only)
-- Author: PitStop Team
-- Date: 2025-12-01
-- =====================================================

-- Create logs_auditoria table
CREATE TABLE logs_auditoria (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id UUID,
    usuario_nome VARCHAR(100),
    acao VARCHAR(50) NOT NULL,
    entidade VARCHAR(50),
    entidade_id UUID,
    dados_antes JSONB,
    dados_depois JSONB,
    ip_address VARCHAR(45), -- IPv6 supported
    user_agent TEXT,
    observacao TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Foreign key
    CONSTRAINT fk_audit_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE SET NULL
);

-- Create indexes
CREATE INDEX idx_audit_usuario ON logs_auditoria(usuario_id);
CREATE INDEX idx_audit_acao ON logs_auditoria(acao);
CREATE INDEX idx_audit_created ON logs_auditoria(created_at);

-- Add comment
COMMENT ON TABLE logs_auditoria IS 'Logs de auditoria do sistema (append-only, não deletar registros)';
COMMENT ON COLUMN logs_auditoria.dados_antes IS 'Estado da entidade ANTES da alteração (JSON)';
COMMENT ON COLUMN logs_auditoria.dados_depois IS 'Estado da entidade DEPOIS da alteração (JSON)';
```

### 4.5 Migration V030 - Enhance Oficina Table

```sql
-- =====================================================
-- Migration: V030 - Enhance oficinas table
-- Description: Add missing fields for complete oficina data
-- Author: PitStop Team
-- Date: 2025-12-01
-- =====================================================

-- Add new columns to oficinas table (if not exists)
DO $$
BEGIN
    -- Logotipo
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name='oficinas' AND column_name='logotipo_url') THEN
        ALTER TABLE oficinas ADD COLUMN logotipo_url VARCHAR(255);
    END IF;

    -- Horário de funcionamento (embedded object stored as JSONB)
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name='oficinas' AND column_name='horario_funcionamento') THEN
        ALTER TABLE oficinas ADD COLUMN horario_funcionamento JSONB;
    END IF;

    -- Website
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name='oficinas' AND column_name='website') THEN
        ALTER TABLE oficinas ADD COLUMN website VARCHAR(255);
    END IF;
END$$;

-- Add comment
COMMENT ON COLUMN oficinas.logotipo_url IS 'URL do logotipo da oficina (usado em PDFs, emails, notas fiscais)';
COMMENT ON COLUMN oficinas.horario_funcionamento IS 'Horário de funcionamento em formato JSON: {segunda: {abre: "08:00", fecha: "18:00"}, ...}';
```

---

**[Continua na Parte 2...]**

*Este é um documento extenso. Vou criar a segunda parte com Backend Implementation, Frontend, Integrações, etc.*

Quer que eu continue criando o restante do documento?
