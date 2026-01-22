# Plano de Implementação - Vendas no Balcão (PDV)

> **Status**: Planejado
> **Prioridade**: Média
> **Estimativa**: Feature completa
> **Data**: 2026-01-20

## 1. Visão Geral

### 1.1 Objetivo
Implementar um módulo de **Vendas no Balcão** (Ponto de Venda - PDV) que permite a venda direta de peças e produtos sem a necessidade de criar uma Ordem de Serviço completa.

### 1.2 Casos de Uso
- Cliente chega na oficina querendo comprar apenas uma peça (filtro de óleo, lâmpada, etc.)
- Venda rápida para consumidor final sem cadastro
- Venda de acessórios e produtos diversos
- Complemento de receita para a oficina

### 1.3 Diferença para Ordem de Serviço

| Aspecto | Ordem de Serviço | Venda no Balcão |
|---------|------------------|-----------------|
| Veículo | Obrigatório | Não aplicável |
| Cliente | Obrigatório | Opcional |
| Serviços | Sim | Não |
| Peças | Sim | Sim |
| Fluxo | ORCAMENTO → APROVADO → ... | ABERTA → FINALIZADA |
| Complexidade | Alta | Baixa (PDV) |

---

## 2. Arquitetura

### 2.1 Diagrama de Entidades

```
┌─────────────────────────────────────────────────────────────┐
│                      VendaBalcao                            │
├─────────────────────────────────────────────────────────────┤
│ id: UUID (PK)                                               │
│ numero: Long (sequencial por oficina)                       │
│ oficina_id: UUID (FK → oficinas) [MULTI-TENANT]             │
│ cliente_id: UUID (FK → clientes) [OPCIONAL]                 │
│ vendedor_id: UUID (FK → usuarios)                           │
│ status: ENUM (ABERTA, FINALIZADA, CANCELADA)                │
│ subtotal: DECIMAL(15,2)                                     │
│ desconto_percentual: DECIMAL(5,2)                           │
│ desconto_valor: DECIMAL(15,2)                               │
│ valor_total: DECIMAL(15,2)                                  │
│ forma_pagamento: ENUM                                       │
│ observacoes: TEXT                                           │
│ data_venda: TIMESTAMP                                       │
│ created_at: TIMESTAMP                                       │
│ updated_at: TIMESTAMP                                       │
└─────────────────────────────────────────────────────────────┘
                              │
                              │ 1:N
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    ItemVendaBalcao                          │
├─────────────────────────────────────────────────────────────┤
│ id: UUID (PK)                                               │
│ venda_balcao_id: UUID (FK → vendas_balcao)                  │
│ peca_id: UUID (FK → pecas)                                  │
│ quantidade: DECIMAL(10,2)                                   │
│ preco_unitario: DECIMAL(15,2)                               │
│ desconto_percentual: DECIMAL(5,2)                           │
│ preco_total: DECIMAL(15,2)                                  │
│ created_at: TIMESTAMP                                       │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 Fluxo de Status

```
    ┌─────────┐
    │ ABERTA  │ ← Venda iniciada (itens sendo adicionados)
    └────┬────┘
         │
         ▼
    ┌──────────────┐
    │ FINALIZADA   │ ← Pagamento confirmado, estoque baixado
    └──────────────┘
         │
         ▼ (se necessário)
    ┌──────────────┐
    │ CANCELADA    │ ← Estoque estornado
    └──────────────┘
```

---

## 3. Backend Implementation

### 3.1 Estrutura de Pacotes

```
src/main/java/com/pitstop/vendabalcao/
├── controller/
│   └── VendaBalcaoController.java
├── domain/
│   ├── VendaBalcao.java
│   ├── ItemVendaBalcao.java
│   └── StatusVendaBalcao.java
├── dto/
│   └── VendaBalcaoDTO.java
├── repository/
│   ├── VendaBalcaoRepository.java
│   └── ItemVendaBalcaoRepository.java
├── service/
│   └── VendaBalcaoService.java
└── mapper/
    └── VendaBalcaoMapper.java
```

### 3.2 Entidades

#### 3.2.1 StatusVendaBalcao.java

```java
package com.pitstop.vendabalcao.domain;

public enum StatusVendaBalcao {
    ABERTA("Aberta"),
    FINALIZADA("Finalizada"),
    CANCELADA("Cancelada");

    private final String descricao;

    StatusVendaBalcao(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
```

#### 3.2.2 VendaBalcao.java

```java
package com.pitstop.vendabalcao.domain;

import com.pitstop.cliente.domain.Cliente;
import com.pitstop.financeiro.domain.FormaPagamento;
import com.pitstop.oficina.domain.Oficina;
import com.pitstop.usuario.domain.Usuario;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "vendas_balcao")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VendaBalcao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private Long numero;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "oficina_id", nullable = false)
    private Oficina oficina;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente; // Opcional - venda avulsa

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendedor_id", nullable = false)
    private Usuario vendedor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private StatusVendaBalcao status = StatusVendaBalcao.ABERTA;

    @OneToMany(mappedBy = "vendaBalcao", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ItemVendaBalcao> itens = new ArrayList<>();

    @Column(precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "desconto_percentual", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal descontoPercentual = BigDecimal.ZERO;

    @Column(name = "desconto_valor", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal descontoValor = BigDecimal.ZERO;

    @Column(name = "valor_total", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal valorTotal = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "forma_pagamento")
    private FormaPagamento formaPagamento;

    @Column(columnDefinition = "TEXT")
    private String observacoes;

    @Column(name = "data_venda")
    private LocalDateTime dataVenda;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // === Métodos de negócio ===

    public void adicionarItem(ItemVendaBalcao item) {
        itens.add(item);
        item.setVendaBalcao(this);
        recalcularTotais();
    }

    public void removerItem(ItemVendaBalcao item) {
        itens.remove(item);
        item.setVendaBalcao(null);
        recalcularTotais();
    }

    public void recalcularTotais() {
        this.subtotal = itens.stream()
            .map(ItemVendaBalcao::getPrecoTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal desconto = BigDecimal.ZERO;
        if (descontoPercentual != null && descontoPercentual.compareTo(BigDecimal.ZERO) > 0) {
            desconto = subtotal.multiply(descontoPercentual).divide(BigDecimal.valueOf(100));
        } else if (descontoValor != null) {
            desconto = descontoValor;
        }

        this.valorTotal = subtotal.subtract(desconto);
    }

    public void finalizar() {
        if (this.status != StatusVendaBalcao.ABERTA) {
            throw new IllegalStateException("Apenas vendas abertas podem ser finalizadas");
        }
        if (this.itens.isEmpty()) {
            throw new IllegalStateException("Venda deve ter pelo menos um item");
        }
        if (this.formaPagamento == null) {
            throw new IllegalStateException("Forma de pagamento é obrigatória");
        }
        this.status = StatusVendaBalcao.FINALIZADA;
        this.dataVenda = LocalDateTime.now();
    }

    public void cancelar() {
        if (this.status == StatusVendaBalcao.CANCELADA) {
            throw new IllegalStateException("Venda já está cancelada");
        }
        this.status = StatusVendaBalcao.CANCELADA;
    }
}
```

#### 3.2.3 ItemVendaBalcao.java

```java
package com.pitstop.vendabalcao.domain;

import com.pitstop.estoque.domain.Peca;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "itens_venda_balcao")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemVendaBalcao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venda_balcao_id", nullable = false)
    private VendaBalcao vendaBalcao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "peca_id", nullable = false)
    private Peca peca;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal quantidade;

    @Column(name = "preco_unitario", nullable = false, precision = 15, scale = 2)
    private BigDecimal precoUnitario;

    @Column(name = "desconto_percentual", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal descontoPercentual = BigDecimal.ZERO;

    @Column(name = "preco_total", nullable = false, precision = 15, scale = 2)
    private BigDecimal precoTotal;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    @PreUpdate
    public void calcularPrecoTotal() {
        BigDecimal total = precoUnitario.multiply(quantidade);
        if (descontoPercentual != null && descontoPercentual.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal desconto = total.multiply(descontoPercentual)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            total = total.subtract(desconto);
        }
        this.precoTotal = total.setScale(2, RoundingMode.HALF_UP);
    }
}
```

### 3.3 DTOs

```java
package com.pitstop.vendabalcao.dto;

import com.pitstop.financeiro.domain.FormaPagamento;
import com.pitstop.vendabalcao.domain.StatusVendaBalcao;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class VendaBalcaoDTO {

    // === Request DTOs ===

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        private UUID clienteId; // Opcional

        @NotEmpty(message = "Venda deve ter pelo menos um item")
        @Valid
        private List<ItemRequest> itens;

        @DecimalMin(value = "0.0", message = "Desconto não pode ser negativo")
        @DecimalMax(value = "100.0", message = "Desconto não pode ser maior que 100%")
        private BigDecimal descontoPercentual;

        private BigDecimal descontoValor;

        @NotNull(message = "Forma de pagamento é obrigatória")
        private FormaPagamento formaPagamento;

        private String observacoes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemRequest {
        @NotNull(message = "Peça é obrigatória")
        private UUID pecaId;

        @NotNull(message = "Quantidade é obrigatória")
        @DecimalMin(value = "0.01", message = "Quantidade deve ser maior que zero")
        private BigDecimal quantidade;

        private BigDecimal precoUnitario; // Se null, usa preço da peça

        @DecimalMin(value = "0.0")
        @DecimalMax(value = "100.0")
        private BigDecimal descontoPercentual;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {
        private UUID clienteId;

        @Valid
        private List<ItemRequest> itens;

        @DecimalMin(value = "0.0")
        @DecimalMax(value = "100.0")
        private BigDecimal descontoPercentual;

        private BigDecimal descontoValor;
        private FormaPagamento formaPagamento;
        private String observacoes;
    }

    // === Response DTOs ===

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private UUID id;
        private Long numero;
        private ClienteResumo cliente;
        private VendedorResumo vendedor;
        private StatusVendaBalcao status;
        private List<ItemResponse> itens;
        private BigDecimal subtotal;
        private BigDecimal descontoPercentual;
        private BigDecimal descontoValor;
        private BigDecimal valorTotal;
        private FormaPagamento formaPagamento;
        private String observacoes;
        private LocalDateTime dataVenda;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ListItem {
        private UUID id;
        private Long numero;
        private String clienteNome; // ou "Consumidor Final"
        private String vendedorNome;
        private StatusVendaBalcao status;
        private int quantidadeItens;
        private BigDecimal valorTotal;
        private FormaPagamento formaPagamento;
        private LocalDateTime dataVenda;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemResponse {
        private UUID id;
        private UUID pecaId;
        private String pecaCodigo;
        private String pecaNome;
        private BigDecimal quantidade;
        private String unidadeMedida;
        private BigDecimal precoUnitario;
        private BigDecimal descontoPercentual;
        private BigDecimal precoTotal;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClienteResumo {
        private UUID id;
        private String nome;
        private String cpfCnpj;
        private String telefone;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VendedorResumo {
        private UUID id;
        private String nome;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Resumo {
        private long totalVendasHoje;
        private BigDecimal faturamentoHoje;
        private long totalVendasMes;
        private BigDecimal faturamentoMes;
        private long vendasAbertas;
    }
}
```

### 3.4 Repository

```java
package com.pitstop.vendabalcao.repository;

import com.pitstop.vendabalcao.domain.StatusVendaBalcao;
import com.pitstop.vendabalcao.domain.VendaBalcao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VendaBalcaoRepository extends JpaRepository<VendaBalcao, UUID> {

    // Busca com filtro multi-tenant
    @Query("SELECT v FROM VendaBalcao v WHERE v.oficina.id = :oficinaId")
    Page<VendaBalcao> findAllByOficinaId(@Param("oficinaId") UUID oficinaId, Pageable pageable);

    // Busca por ID com tenant
    @Query("SELECT v FROM VendaBalcao v WHERE v.id = :id AND v.oficina.id = :oficinaId")
    Optional<VendaBalcao> findByIdAndOficinaId(@Param("id") UUID id, @Param("oficinaId") UUID oficinaId);

    // Busca por status
    @Query("SELECT v FROM VendaBalcao v WHERE v.oficina.id = :oficinaId AND v.status = :status")
    Page<VendaBalcao> findByOficinaIdAndStatus(
        @Param("oficinaId") UUID oficinaId,
        @Param("status") StatusVendaBalcao status,
        Pageable pageable
    );

    // Busca por período
    @Query("SELECT v FROM VendaBalcao v WHERE v.oficina.id = :oficinaId " +
           "AND v.dataVenda BETWEEN :inicio AND :fim")
    Page<VendaBalcao> findByOficinaIdAndPeriodo(
        @Param("oficinaId") UUID oficinaId,
        @Param("inicio") LocalDateTime inicio,
        @Param("fim") LocalDateTime fim,
        Pageable pageable
    );

    // Próximo número sequencial
    @Query("SELECT COALESCE(MAX(v.numero), 0) + 1 FROM VendaBalcao v WHERE v.oficina.id = :oficinaId")
    Long getProximoNumero(@Param("oficinaId") UUID oficinaId);

    // Contagem por status
    @Query("SELECT COUNT(v) FROM VendaBalcao v WHERE v.oficina.id = :oficinaId AND v.status = :status")
    long countByOficinaIdAndStatus(@Param("oficinaId") UUID oficinaId, @Param("status") StatusVendaBalcao status);

    // Faturamento do período
    @Query("SELECT COALESCE(SUM(v.valorTotal), 0) FROM VendaBalcao v " +
           "WHERE v.oficina.id = :oficinaId AND v.status = 'FINALIZADA' " +
           "AND v.dataVenda BETWEEN :inicio AND :fim")
    BigDecimal sumFaturamentoByPeriodo(
        @Param("oficinaId") UUID oficinaId,
        @Param("inicio") LocalDateTime inicio,
        @Param("fim") LocalDateTime fim
    );

    // Contagem do período
    @Query("SELECT COUNT(v) FROM VendaBalcao v " +
           "WHERE v.oficina.id = :oficinaId AND v.status = 'FINALIZADA' " +
           "AND v.dataVenda BETWEEN :inicio AND :fim")
    long countByPeriodo(
        @Param("oficinaId") UUID oficinaId,
        @Param("inicio") LocalDateTime inicio,
        @Param("fim") LocalDateTime fim
    );
}
```

### 3.5 Service

```java
package com.pitstop.vendabalcao.service;

import com.pitstop.cliente.repository.ClienteRepository;
import com.pitstop.estoque.domain.Peca;
import com.pitstop.estoque.domain.TipoMovimentacao;
import com.pitstop.estoque.repository.PecaRepository;
import com.pitstop.estoque.service.MovimentacaoEstoqueService;
import com.pitstop.shared.exception.BusinessException;
import com.pitstop.shared.exception.ResourceNotFoundException;
import com.pitstop.shared.security.TenantContext;
import com.pitstop.usuario.repository.UsuarioRepository;
import com.pitstop.vendabalcao.domain.*;
import com.pitstop.vendabalcao.dto.VendaBalcaoDTO;
import com.pitstop.vendabalcao.repository.VendaBalcaoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VendaBalcaoService {

    private final VendaBalcaoRepository vendaBalcaoRepository;
    private final PecaRepository pecaRepository;
    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final MovimentacaoEstoqueService movimentacaoService;

    @Transactional
    public VendaBalcaoDTO.Response criar(VendaBalcaoDTO.CreateRequest request, UUID vendedorId) {
        UUID oficinaId = TenantContext.getCurrentOficinaId();
        log.info("Criando venda no balcão - Oficina: {}, Vendedor: {}", oficinaId, vendedorId);

        VendaBalcao venda = VendaBalcao.builder()
            .numero(vendaBalcaoRepository.getProximoNumero(oficinaId))
            .oficina(/* buscar oficina */)
            .vendedor(usuarioRepository.findById(vendedorId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendedor não encontrado")))
            .status(StatusVendaBalcao.ABERTA)
            .formaPagamento(request.getFormaPagamento())
            .descontoPercentual(request.getDescontoPercentual())
            .descontoValor(request.getDescontoValor())
            .observacoes(request.getObservacoes())
            .build();

        // Cliente opcional
        if (request.getClienteId() != null) {
            venda.setCliente(clienteRepository.findByIdAndOficinaId(request.getClienteId(), oficinaId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado")));
        }

        // Adicionar itens
        for (VendaBalcaoDTO.ItemRequest itemReq : request.getItens()) {
            Peca peca = pecaRepository.findByIdAndOficinaId(itemReq.getPecaId(), oficinaId)
                .orElseThrow(() -> new ResourceNotFoundException("Peça não encontrada: " + itemReq.getPecaId()));

            // Validar estoque
            if (peca.getQuantidadeAtual().compareTo(itemReq.getQuantidade()) < 0) {
                throw new BusinessException("Estoque insuficiente para peça: " + peca.getNome());
            }

            ItemVendaBalcao item = ItemVendaBalcao.builder()
                .peca(peca)
                .quantidade(itemReq.getQuantidade())
                .precoUnitario(itemReq.getPrecoUnitario() != null ?
                    itemReq.getPrecoUnitario() : peca.getPrecoVenda())
                .descontoPercentual(itemReq.getDescontoPercentual())
                .build();

            venda.adicionarItem(item);
        }

        // Finalizar venda imediatamente (PDV rápido)
        venda.finalizar();

        // Baixar estoque
        baixarEstoque(venda);

        VendaBalcao saved = vendaBalcaoRepository.save(venda);
        log.info("Venda no balcão criada - Número: {}, Total: {}", saved.getNumero(), saved.getValorTotal());

        return toResponse(saved);
    }

    private void baixarEstoque(VendaBalcao venda) {
        for (ItemVendaBalcao item : venda.getItens()) {
            movimentacaoService.registrarSaida(
                item.getPeca().getId(),
                item.getQuantidade(),
                TipoMovimentacao.SAIDA,
                "Venda Balcão #" + venda.getNumero(),
                null, // sem OS
                venda.getId() // referência da venda
            );
        }
    }

    @Transactional
    public void cancelar(UUID vendaId) {
        UUID oficinaId = TenantContext.getCurrentOficinaId();
        VendaBalcao venda = vendaBalcaoRepository.findByIdAndOficinaId(vendaId, oficinaId)
            .orElseThrow(() -> new ResourceNotFoundException("Venda não encontrada"));

        if (venda.getStatus() == StatusVendaBalcao.FINALIZADA) {
            // Estornar estoque
            for (ItemVendaBalcao item : venda.getItens()) {
                movimentacaoService.registrarEntrada(
                    item.getPeca().getId(),
                    item.getQuantidade(),
                    TipoMovimentacao.DEVOLUCAO,
                    "Cancelamento Venda Balcão #" + venda.getNumero(),
                    null,
                    venda.getId()
                );
            }
        }

        venda.cancelar();
        vendaBalcaoRepository.save(venda);
        log.info("Venda no balcão cancelada - Número: {}", venda.getNumero());
    }

    @Transactional(readOnly = true)
    public Page<VendaBalcaoDTO.ListItem> listar(
            StatusVendaBalcao status,
            LocalDate dataInicio,
            LocalDate dataFim,
            Pageable pageable) {
        UUID oficinaId = TenantContext.getCurrentOficinaId();

        // Implementar filtros conforme necessário
        Page<VendaBalcao> vendas = vendaBalcaoRepository.findAllByOficinaId(oficinaId, pageable);

        return vendas.map(this::toListItem);
    }

    @Transactional(readOnly = true)
    public VendaBalcaoDTO.Response buscarPorId(UUID id) {
        UUID oficinaId = TenantContext.getCurrentOficinaId();
        VendaBalcao venda = vendaBalcaoRepository.findByIdAndOficinaId(id, oficinaId)
            .orElseThrow(() -> new ResourceNotFoundException("Venda não encontrada"));
        return toResponse(venda);
    }

    @Transactional(readOnly = true)
    public VendaBalcaoDTO.Resumo getResumo() {
        UUID oficinaId = TenantContext.getCurrentOficinaId();
        LocalDateTime inicioHoje = LocalDate.now().atStartOfDay();
        LocalDateTime fimHoje = LocalDate.now().atTime(LocalTime.MAX);
        LocalDateTime inicioMes = LocalDate.now().withDayOfMonth(1).atStartOfDay();

        return VendaBalcaoDTO.Resumo.builder()
            .totalVendasHoje(vendaBalcaoRepository.countByPeriodo(oficinaId, inicioHoje, fimHoje))
            .faturamentoHoje(vendaBalcaoRepository.sumFaturamentoByPeriodo(oficinaId, inicioHoje, fimHoje))
            .totalVendasMes(vendaBalcaoRepository.countByPeriodo(oficinaId, inicioMes, fimHoje))
            .faturamentoMes(vendaBalcaoRepository.sumFaturamentoByPeriodo(oficinaId, inicioMes, fimHoje))
            .vendasAbertas(vendaBalcaoRepository.countByOficinaIdAndStatus(oficinaId, StatusVendaBalcao.ABERTA))
            .build();
    }

    // === Métodos de conversão (implementar) ===
    private VendaBalcaoDTO.Response toResponse(VendaBalcao venda) {
        // Implementar conversão
        return null;
    }

    private VendaBalcaoDTO.ListItem toListItem(VendaBalcao venda) {
        // Implementar conversão
        return null;
    }
}
```

### 3.6 Controller

```java
package com.pitstop.vendabalcao.controller;

import com.pitstop.shared.security.CustomUserDetails;
import com.pitstop.vendabalcao.domain.StatusVendaBalcao;
import com.pitstop.vendabalcao.dto.VendaBalcaoDTO;
import com.pitstop.vendabalcao.service.VendaBalcaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/vendas-balcao")
@RequiredArgsConstructor
@Tag(name = "Vendas no Balcão", description = "PDV - Venda direta de peças")
@PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE')")
public class VendaBalcaoController {

    private final VendaBalcaoService vendaBalcaoService;

    @PostMapping
    @Operation(summary = "Criar nova venda no balcão")
    public ResponseEntity<VendaBalcaoDTO.Response> criar(
            @Valid @RequestBody VendaBalcaoDTO.CreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.info("POST /api/vendas-balcao - Nova venda");
        VendaBalcaoDTO.Response response = vendaBalcaoService.criar(request, userDetails.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Listar vendas no balcão")
    public ResponseEntity<Page<VendaBalcaoDTO.ListItem>> listar(
            @RequestParam(required = false) StatusVendaBalcao status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        log.debug("GET /api/vendas-balcao - status={}", status);
        Page<VendaBalcaoDTO.ListItem> vendas = vendaBalcaoService.listar(status, dataInicio, dataFim, pageable);
        return ResponseEntity.ok(vendas);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar venda por ID")
    public ResponseEntity<VendaBalcaoDTO.Response> buscarPorId(@PathVariable UUID id) {
        log.debug("GET /api/vendas-balcao/{}", id);
        VendaBalcaoDTO.Response response = vendaBalcaoService.buscarPorId(id);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/cancelar")
    @Operation(summary = "Cancelar venda")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE')")
    public ResponseEntity<Void> cancelar(@PathVariable UUID id) {
        log.info("PATCH /api/vendas-balcao/{}/cancelar", id);
        vendaBalcaoService.cancelar(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/resumo")
    @Operation(summary = "Resumo de vendas")
    public ResponseEntity<VendaBalcaoDTO.Resumo> getResumo() {
        log.debug("GET /api/vendas-balcao/resumo");
        VendaBalcaoDTO.Resumo resumo = vendaBalcaoService.getResumo();
        return ResponseEntity.ok(resumo);
    }
}
```

### 3.7 Migração SQL

```sql
-- V075__create_venda_balcao.sql

-- Tabela principal de vendas no balcão
CREATE TABLE vendas_balcao (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    numero BIGINT NOT NULL,
    oficina_id UUID NOT NULL REFERENCES oficinas(id),
    cliente_id UUID REFERENCES clientes(id),
    vendedor_id UUID NOT NULL REFERENCES usuarios(id),
    status VARCHAR(20) NOT NULL DEFAULT 'ABERTA',
    subtotal DECIMAL(15, 2) NOT NULL DEFAULT 0,
    desconto_percentual DECIMAL(5, 2) DEFAULT 0,
    desconto_valor DECIMAL(15, 2) DEFAULT 0,
    valor_total DECIMAL(15, 2) NOT NULL DEFAULT 0,
    forma_pagamento VARCHAR(30),
    observacoes TEXT,
    data_venda TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_venda_balcao_numero_oficina UNIQUE (numero, oficina_id),
    CONSTRAINT chk_venda_balcao_status CHECK (status IN ('ABERTA', 'FINALIZADA', 'CANCELADA'))
);

-- Índices para performance
CREATE INDEX idx_vendas_balcao_oficina ON vendas_balcao(oficina_id);
CREATE INDEX idx_vendas_balcao_cliente ON vendas_balcao(cliente_id);
CREATE INDEX idx_vendas_balcao_vendedor ON vendas_balcao(vendedor_id);
CREATE INDEX idx_vendas_balcao_status ON vendas_balcao(status);
CREATE INDEX idx_vendas_balcao_data ON vendas_balcao(data_venda);
CREATE INDEX idx_vendas_balcao_created ON vendas_balcao(created_at);

-- Tabela de itens da venda
CREATE TABLE itens_venda_balcao (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    venda_balcao_id UUID NOT NULL REFERENCES vendas_balcao(id) ON DELETE CASCADE,
    peca_id UUID NOT NULL REFERENCES pecas(id),
    quantidade DECIMAL(10, 2) NOT NULL,
    preco_unitario DECIMAL(15, 2) NOT NULL,
    desconto_percentual DECIMAL(5, 2) DEFAULT 0,
    preco_total DECIMAL(15, 2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_item_venda_quantidade CHECK (quantidade > 0),
    CONSTRAINT chk_item_venda_preco CHECK (preco_unitario >= 0)
);

-- Índices para itens
CREATE INDEX idx_itens_venda_balcao_venda ON itens_venda_balcao(venda_balcao_id);
CREATE INDEX idx_itens_venda_balcao_peca ON itens_venda_balcao(peca_id);

-- Comentários
COMMENT ON TABLE vendas_balcao IS 'Vendas diretas no balcão (PDV) sem ordem de serviço';
COMMENT ON TABLE itens_venda_balcao IS 'Itens (peças) de uma venda no balcão';
COMMENT ON COLUMN vendas_balcao.cliente_id IS 'Cliente opcional - NULL para venda consumidor final';
```

---

## 4. Frontend Implementation

### 4.1 Estrutura de Arquivos

```
frontend/src/features/vendas-balcao/
├── components/
│   ├── BuscaPecaInput.tsx       # Busca rápida de peças
│   ├── CarrinhoVenda.tsx        # Lista de itens no carrinho
│   ├── ItemVendaCard.tsx        # Card de item individual
│   ├── ResumoVenda.tsx          # Subtotal, desconto, total
│   └── PagamentoModal.tsx       # Modal para finalizar pagamento
├── hooks/
│   └── useVendaBalcao.ts        # React Query hooks
├── pages/
│   ├── VendasBalcaoPage.tsx     # Lista de vendas
│   ├── PDVPage.tsx              # Tela principal do PDV
│   └── VendaBalcaoDetailPage.tsx # Detalhes da venda
├── services/
│   └── vendaBalcaoService.ts    # Chamadas API
└── types/
    └── vendaBalcao.ts           # TypeScript interfaces
```

### 4.2 Types

```typescript
// types/vendaBalcao.ts

export type StatusVendaBalcao = 'ABERTA' | 'FINALIZADA' | 'CANCELADA';

export interface VendaBalcaoResponse {
  id: string;
  numero: number;
  cliente?: ClienteResumo;
  vendedor: VendedorResumo;
  status: StatusVendaBalcao;
  itens: ItemVendaBalcaoResponse[];
  subtotal: number;
  descontoPercentual: number;
  descontoValor: number;
  valorTotal: number;
  formaPagamento: string;
  observacoes?: string;
  dataVenda?: string;
  createdAt: string;
}

export interface VendaBalcaoListItem {
  id: string;
  numero: number;
  clienteNome: string;
  vendedorNome: string;
  status: StatusVendaBalcao;
  quantidadeItens: number;
  valorTotal: number;
  formaPagamento: string;
  dataVenda?: string;
  createdAt: string;
}

export interface ItemVendaBalcaoResponse {
  id: string;
  pecaId: string;
  pecaCodigo: string;
  pecaNome: string;
  quantidade: number;
  unidadeMedida: string;
  precoUnitario: number;
  descontoPercentual: number;
  precoTotal: number;
}

export interface VendaBalcaoCreateRequest {
  clienteId?: string;
  itens: ItemVendaBalcaoRequest[];
  descontoPercentual?: number;
  descontoValor?: number;
  formaPagamento: string;
  observacoes?: string;
}

export interface ItemVendaBalcaoRequest {
  pecaId: string;
  quantidade: number;
  precoUnitario?: number;
  descontoPercentual?: number;
}

export interface ClienteResumo {
  id: string;
  nome: string;
  cpfCnpj?: string;
  telefone?: string;
}

export interface VendedorResumo {
  id: string;
  nome: string;
}

export interface VendaBalcaoResumo {
  totalVendasHoje: number;
  faturamentoHoje: number;
  totalVendasMes: number;
  faturamentoMes: number;
  vendasAbertas: number;
}

export interface VendaBalcaoFiltros {
  status?: StatusVendaBalcao;
  dataInicio?: string;
  dataFim?: string;
  page?: number;
  size?: number;
}

// Estado local do carrinho (antes de enviar ao backend)
export interface ItemCarrinho {
  peca: {
    id: string;
    codigo: string;
    nome: string;
    precoVenda: number;
    quantidadeAtual: number;
    unidadeMedida: string;
  };
  quantidade: number;
  precoUnitario: number;
  descontoPercentual: number;
  precoTotal: number;
}
```

### 4.3 Service

```typescript
// services/vendaBalcaoService.ts

import { api } from '@/shared/services/api';
import type {
  VendaBalcaoResponse,
  VendaBalcaoListItem,
  VendaBalcaoCreateRequest,
  VendaBalcaoResumo,
  VendaBalcaoFiltros,
} from '../types/vendaBalcao';

const BASE_URL = '/vendas-balcao';

export const vendaBalcaoService = {
  async criar(data: VendaBalcaoCreateRequest): Promise<VendaBalcaoResponse> {
    const response = await api.post<VendaBalcaoResponse>(BASE_URL, data);
    return response.data;
  },

  async buscarPorId(id: string): Promise<VendaBalcaoResponse> {
    const response = await api.get<VendaBalcaoResponse>(`${BASE_URL}/${id}`);
    return response.data;
  },

  async listar(filtros: VendaBalcaoFiltros = {}): Promise<PageResponse<VendaBalcaoListItem>> {
    const params = new URLSearchParams();
    if (filtros.status) params.append('status', filtros.status);
    if (filtros.dataInicio) params.append('dataInicio', filtros.dataInicio);
    if (filtros.dataFim) params.append('dataFim', filtros.dataFim);
    if (filtros.page !== undefined) params.append('page', String(filtros.page));
    if (filtros.size !== undefined) params.append('size', String(filtros.size));

    const response = await api.get<PageResponse<VendaBalcaoListItem>>(
      `${BASE_URL}?${params.toString()}`
    );
    return response.data;
  },

  async cancelar(id: string): Promise<void> {
    await api.patch(`${BASE_URL}/${id}/cancelar`);
  },

  async getResumo(): Promise<VendaBalcaoResumo> {
    const response = await api.get<VendaBalcaoResumo>(`${BASE_URL}/resumo`);
    return response.data;
  },
};

interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}
```

### 4.4 Hooks

```typescript
// hooks/useVendaBalcao.ts

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { vendaBalcaoService } from '../services/vendaBalcaoService';
import type { VendaBalcaoCreateRequest, VendaBalcaoFiltros } from '../types/vendaBalcao';

export const vendaBalcaoKeys = {
  all: ['vendas-balcao'] as const,
  lists: () => [...vendaBalcaoKeys.all, 'list'] as const,
  list: (filtros: VendaBalcaoFiltros) => [...vendaBalcaoKeys.lists(), filtros] as const,
  details: () => [...vendaBalcaoKeys.all, 'detail'] as const,
  detail: (id: string) => [...vendaBalcaoKeys.details(), id] as const,
  resumo: () => [...vendaBalcaoKeys.all, 'resumo'] as const,
};

export const useVendasBalcao = (filtros: VendaBalcaoFiltros = {}) => {
  return useQuery({
    queryKey: vendaBalcaoKeys.list(filtros),
    queryFn: () => vendaBalcaoService.listar(filtros),
    staleTime: 30 * 1000, // 30 segundos
  });
};

export const useVendaBalcao = (id?: string) => {
  return useQuery({
    queryKey: vendaBalcaoKeys.detail(id || ''),
    queryFn: () => vendaBalcaoService.buscarPorId(id!),
    enabled: !!id,
  });
};

export const useVendaBalcaoResumo = () => {
  return useQuery({
    queryKey: vendaBalcaoKeys.resumo(),
    queryFn: () => vendaBalcaoService.getResumo(),
    staleTime: 60 * 1000, // 1 minuto
  });
};

export const useCreateVendaBalcao = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: VendaBalcaoCreateRequest) => vendaBalcaoService.criar(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: vendaBalcaoKeys.lists() });
      queryClient.invalidateQueries({ queryKey: vendaBalcaoKeys.resumo() });
    },
  });
};

export const useCancelarVendaBalcao = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => vendaBalcaoService.cancelar(id),
    onSuccess: (_, id) => {
      queryClient.invalidateQueries({ queryKey: vendaBalcaoKeys.lists() });
      queryClient.invalidateQueries({ queryKey: vendaBalcaoKeys.detail(id) });
      queryClient.invalidateQueries({ queryKey: vendaBalcaoKeys.resumo() });
    },
  });
};
```

### 4.5 Rotas (App.tsx)

```tsx
// Adicionar em App.tsx

import { VendasBalcaoPage } from '@/features/vendas-balcao/pages/VendasBalcaoPage';
import { PDVPage } from '@/features/vendas-balcao/pages/PDVPage';
import { VendaBalcaoDetailPage } from '@/features/vendas-balcao/pages/VendaBalcaoDetailPage';

// Dentro das rotas protegidas:
<Route path="vendas-balcao">
  <Route
    index
    element={
      <ProtectedRoute requiredRoles={[PerfilUsuario.ADMIN, PerfilUsuario.GERENTE, PerfilUsuario.ATENDENTE]}>
        <VendasBalcaoPage />
      </ProtectedRoute>
    }
  />
  <Route
    path="pdv"
    element={
      <ProtectedRoute requiredRoles={[PerfilUsuario.ADMIN, PerfilUsuario.GERENTE, PerfilUsuario.ATENDENTE]}>
        <PDVPage />
      </ProtectedRoute>
    }
  />
  <Route
    path=":id"
    element={
      <ProtectedRoute requiredRoles={[PerfilUsuario.ADMIN, PerfilUsuario.GERENTE, PerfilUsuario.ATENDENTE]}>
        <VendaBalcaoDetailPage />
      </ProtectedRoute>
    }
  />
</Route>
```

### 4.6 Menu (MainLayout.tsx)

```tsx
// Adicionar no array de navegação, após Estoque:

{
  name: 'Vendas',
  icon: ShoppingCart, // importar de lucide-react
  requiredRoles: [PerfilUsuario.ADMIN, PerfilUsuario.GERENTE, PerfilUsuario.ATENDENTE],
  items: [
    {
      name: 'PDV - Balcão',
      href: '/vendas-balcao/pdv',
      icon: Monitor, // importar de lucide-react
      requiredRoles: [PerfilUsuario.ADMIN, PerfilUsuario.GERENTE, PerfilUsuario.ATENDENTE],
    },
    {
      name: 'Histórico',
      href: '/vendas-balcao',
      icon: History, // importar de lucide-react
      requiredRoles: [PerfilUsuario.ADMIN, PerfilUsuario.GERENTE, PerfilUsuario.ATENDENTE],
    },
  ],
},
```

---

## 5. Interface do PDV

### 5.1 Layout Proposto

```
┌─────────────────────────────────────────────────────────────────────────┐
│  🛒 PDV - Venda no Balcão                              [Cliente: ___▼]  │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  ┌─────────────────────────────────────────────┐  ┌──────────────────┐  │
│  │ 🔍 Buscar peça (código ou nome)...          │  │   RESUMO         │  │
│  └─────────────────────────────────────────────┘  │                  │  │
│                                                    │  Itens: 3        │  │
│  ┌─────────────────────────────────────────────┐  │                  │  │
│  │ CARRINHO                                    │  │  Subtotal:       │  │
│  │                                             │  │  R$ 450,00       │  │
│  │  ┌─────────────────────────────────────┐   │  │                  │  │
│  │  │ Filtro de Óleo HB20     2x R$ 45,00 │   │  │  Desconto:       │  │
│  │  │ [- ] [2] [+ ]           = R$ 90,00  │   │  │  R$ 0,00         │  │
│  │  └─────────────────────────────────────┘   │  │                  │  │
│  │                                             │  │  ──────────────  │  │
│  │  ┌─────────────────────────────────────┐   │  │                  │  │
│  │  │ Óleo Motor 5W30         4x R$ 90,00 │   │  │  TOTAL:          │  │
│  │  │ [- ] [4] [+ ]           = R$ 360,00 │   │  │  R$ 450,00       │  │
│  │  └─────────────────────────────────────┘   │  │                  │  │
│  │                                             │  │                  │  │
│  │                                             │  │  [  FINALIZAR  ] │  │
│  │                                             │  │                  │  │
│  └─────────────────────────────────────────────┘  └──────────────────┘  │
│                                                                         │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │ PEÇAS FREQUENTES:  [Óleo 5W30] [Filtro Óleo] [Lâmpada H4] [...]  │  │
│  └──────────────────────────────────────────────────────────────────┘  │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

### 5.2 Fluxo de Uso

1. Atendente acessa PDV (`/vendas-balcao/pdv`)
2. (Opcional) Seleciona cliente ou deixa como "Consumidor Final"
3. Busca peças por código ou nome
4. Adiciona ao carrinho com quantidade desejada
5. Aplica desconto se necessário
6. Seleciona forma de pagamento
7. Finaliza venda
8. Sistema baixa estoque automaticamente
9. Opção de imprimir recibo/cupom

---

## 6. Integrações

### 6.1 Com Estoque
- Validar estoque disponível ao adicionar item
- Baixar estoque ao finalizar venda
- Estornar estoque ao cancelar venda

### 6.2 Com Fluxo de Caixa
- Venda finalizada gera entrada no fluxo de caixa
- Categoria: "Venda Balcão"

### 6.3 Com Clientes (Opcional)
- Busca rápida de cliente por nome/CPF
- Histórico de compras do cliente

### 6.4 Futura: NFC-e
- Emissão de NFC-e para consumidor final
- Integração com SAT (SP) ou contingência

---

## 7. Checklist de Implementação

### Backend
- [ ] Criar enum `StatusVendaBalcao`
- [ ] Criar entidade `VendaBalcao`
- [ ] Criar entidade `ItemVendaBalcao`
- [ ] Criar `VendaBalcaoDTO` (Request/Response)
- [ ] Criar `VendaBalcaoRepository`
- [ ] Criar `ItemVendaBalcaoRepository`
- [ ] Criar `VendaBalcaoService`
- [ ] Criar `VendaBalcaoController`
- [ ] Criar migração `V075__create_venda_balcao.sql`
- [ ] Adicionar em `db.changelog-master.yaml`
- [ ] Testar endpoints no Swagger

### Frontend
- [ ] Criar types em `types/vendaBalcao.ts`
- [ ] Criar service em `services/vendaBalcaoService.ts`
- [ ] Criar hooks em `hooks/useVendaBalcao.ts`
- [ ] Criar `VendasBalcaoPage.tsx` (lista)
- [ ] Criar `PDVPage.tsx` (tela principal)
- [ ] Criar `VendaBalcaoDetailPage.tsx` (detalhes)
- [ ] Criar componentes auxiliares (BuscaPeca, Carrinho, etc.)
- [ ] Adicionar rotas em `App.tsx`
- [ ] Adicionar menu em `MainLayout.tsx`
- [ ] Testar fluxo completo

### Testes
- [ ] Testes unitários do Service
- [ ] Testes de integração do Controller
- [ ] Testes E2E do fluxo de venda

---

## 8. Considerações Finais

### Segurança
- Apenas usuários autenticados podem acessar o PDV
- Roles permitidos: ADMIN, GERENTE, ATENDENTE
- Cancelamento requer ADMIN ou GERENTE
- Multi-tenancy obrigatório (oficina_id)

### Performance
- Busca de peças com debounce (300ms)
- Cache de peças frequentes
- Paginação na listagem de vendas

### UX
- Interface otimizada para velocidade
- Atalhos de teclado (Enter para adicionar, F2 para finalizar)
- Feedback visual de estoque baixo
- Impressão de recibo opcional

---

**Documento criado em**: 2026-01-20
**Última atualização**: 2026-01-20
**Autor**: Claude Code Assistant
