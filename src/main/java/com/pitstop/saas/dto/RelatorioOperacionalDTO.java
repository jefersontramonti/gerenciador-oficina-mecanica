package com.pitstop.saas.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * DTO para relatório operacional do SaaS.
 */
public record RelatorioOperacionalDTO(
    // Período do relatório
    LocalDate dataInicio,
    LocalDate dataFim,

    // Resumo Geral
    Integer totalOficinas,
    Integer oficinasAtivas,
    Integer oficinasEmTrial,
    Integer oficinasSuspensas,
    Integer oficinasCanceladas,

    // Usuários
    Integer totalUsuarios,
    Integer usuariosAtivos,
    Integer loginsPeriodo,
    Double mediaLoginsPorOficina,

    // Uso do Sistema
    Long totalOrdensServico,
    Long ordensServicoPeriodo,
    Long totalClientes,
    Long clientesPeriodo,
    Long totalVeiculos,
    Long veiculosPeriodo,

    // Métricas de Uso
    Double mediaOSPorOficina,
    Double mediaClientesPorOficina,
    Double mediaUsuariosPorOficina,

    // Distribuição por Plano
    List<DistribuicaoPlano> distribuicaoPlanos,

    // Distribuição por Status
    List<DistribuicaoStatus> distribuicaoStatus,

    // Oficinas Mais Ativas
    List<OficinaAtividade> oficinaMaisAtivas,

    // Oficinas com Menor Atividade
    List<OficinaAtividade> oficinaMenosAtivas,

    // Evolução Mensal
    List<EvolucaoOperacional> evolucaoMensal
) {
    public record DistribuicaoPlano(
        String planoNome,
        String planoCodigo,
        Integer quantidade,
        Double percentual
    ) {}

    public record DistribuicaoStatus(
        String status,
        Integer quantidade,
        Double percentual
    ) {}

    public record OficinaAtividade(
        String oficinaId,
        String nomeFantasia,
        String plano,
        Integer ordensServico,
        Integer clientes,
        Integer usuarios,
        Integer loginsMes
    ) {}

    public record EvolucaoOperacional(
        String mesAno,
        Integer oficinasAtivas,
        Long ordensServico,
        Long clientes,
        Integer usuarios
    ) {}
}
