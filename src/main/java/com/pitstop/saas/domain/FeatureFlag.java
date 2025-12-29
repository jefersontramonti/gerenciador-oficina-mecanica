package com.pitstop.saas.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.*;

@Entity
@Table(name = "feature_flags")
public class FeatureFlag {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "codigo", nullable = false, unique = true, length = 100)
    private String codigo;

    @Column(name = "nome", nullable = false, length = 200)
    private String nome;

    @Column(name = "descricao", columnDefinition = "TEXT")
    private String descricao;

    @Column(name = "habilitado_global", nullable = false)
    private Boolean habilitadoGlobal = false;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "habilitado_por_plano", columnDefinition = "jsonb")
    private Map<String, Boolean> habilitadoPorPlano = new HashMap<>();

    @Column(name = "habilitado_por_oficina", columnDefinition = "uuid[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    private UUID[] habilitadoPorOficina = new UUID[0];

    @Column(name = "percentual_rollout")
    private Integer percentualRollout = 0;

    @Column(name = "data_inicio")
    private OffsetDateTime dataInicio;

    @Column(name = "data_fim")
    private OffsetDateTime dataFim;

    @Column(name = "categoria", length = 50)
    private String categoria = "GERAL";

    @Column(name = "requer_autorizacao")
    private Boolean requerAutorizacao = false;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    // Getters and Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public Boolean getHabilitadoGlobal() {
        return habilitadoGlobal;
    }

    public void setHabilitadoGlobal(Boolean habilitadoGlobal) {
        this.habilitadoGlobal = habilitadoGlobal;
    }

    public Map<String, Boolean> getHabilitadoPorPlano() {
        return habilitadoPorPlano;
    }

    public void setHabilitadoPorPlano(Map<String, Boolean> habilitadoPorPlano) {
        this.habilitadoPorPlano = habilitadoPorPlano;
    }

    public UUID[] getHabilitadoPorOficina() {
        return habilitadoPorOficina;
    }

    public void setHabilitadoPorOficina(UUID[] habilitadoPorOficina) {
        this.habilitadoPorOficina = habilitadoPorOficina;
    }

    public Integer getPercentualRollout() {
        return percentualRollout;
    }

    public void setPercentualRollout(Integer percentualRollout) {
        this.percentualRollout = percentualRollout;
    }

    public OffsetDateTime getDataInicio() {
        return dataInicio;
    }

    public void setDataInicio(OffsetDateTime dataInicio) {
        this.dataInicio = dataInicio;
    }

    public OffsetDateTime getDataFim() {
        return dataFim;
    }

    public void setDataFim(OffsetDateTime dataFim) {
        this.dataFim = dataFim;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public Boolean getRequerAutorizacao() {
        return requerAutorizacao;
    }

    public void setRequerAutorizacao(Boolean requerAutorizacao) {
        this.requerAutorizacao = requerAutorizacao;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }

    public UUID getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(UUID updatedBy) {
        this.updatedBy = updatedBy;
    }

    // Helper methods

    public List<UUID> getHabilitadoPorOficinaList() {
        return habilitadoPorOficina != null ? Arrays.asList(habilitadoPorOficina) : Collections.emptyList();
    }

    public void addOficinaHabilitada(UUID oficinaId) {
        Set<UUID> oficinas = new HashSet<>(getHabilitadoPorOficinaList());
        oficinas.add(oficinaId);
        this.habilitadoPorOficina = oficinas.toArray(new UUID[0]);
    }

    public void removeOficinaHabilitada(UUID oficinaId) {
        Set<UUID> oficinas = new HashSet<>(getHabilitadoPorOficinaList());
        oficinas.remove(oficinaId);
        this.habilitadoPorOficina = oficinas.toArray(new UUID[0]);
    }

    public boolean isHabilitadoParaPlano(String plano) {
        if (habilitadoPorPlano == null) return false;
        return Boolean.TRUE.equals(habilitadoPorPlano.get(plano));
    }

    public boolean isHabilitadoParaOficina(UUID oficinaId) {
        if (habilitadoPorOficina == null) return false;
        return Arrays.asList(habilitadoPorOficina).contains(oficinaId);
    }

    public boolean isAtivo() {
        OffsetDateTime now = OffsetDateTime.now();
        if (dataInicio != null && now.isBefore(dataInicio)) return false;
        if (dataFim != null && now.isAfter(dataFim)) return false;
        return true;
    }
}
