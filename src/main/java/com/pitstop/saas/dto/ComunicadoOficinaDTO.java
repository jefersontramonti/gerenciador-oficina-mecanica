package com.pitstop.saas.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.pitstop.saas.domain.Comunicado;
import com.pitstop.saas.domain.ComunicadoLeitura;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ComunicadoOficinaDTO(
    UUID id,
    String titulo,
    String resumo,
    String tipo,
    String tipoDescricao,
    String prioridade,
    String prioridadeDescricao,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    OffsetDateTime dataEnvio,
    boolean visualizado,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    OffsetDateTime dataVisualizacao,
    boolean confirmado,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    OffsetDateTime dataConfirmacao,
    boolean requerConfirmacao,
    String autorNome
) {
    public static ComunicadoOficinaDTO fromEntity(ComunicadoLeitura leitura) {
        Comunicado c = leitura.getComunicado();
        return new ComunicadoOficinaDTO(
            c.getId(),
            c.getTitulo(),
            c.getResumo(),
            c.getTipo().name(),
            getDescricaoTipo(c.getTipo().name()),
            c.getPrioridade().name(),
            getDescricaoPrioridade(c.getPrioridade().name()),
            c.getDataEnvio(),
            leitura.getVisualizado(),
            leitura.getDataVisualizacao(),
            leitura.getConfirmado(),
            leitura.getDataConfirmacao(),
            c.getRequerConfirmacao(),
            c.getAutorNome()
        );
    }

    private static String getDescricaoTipo(String tipo) {
        return switch (tipo) {
            case "NOVIDADE" -> "Novidade";
            case "ATUALIZACAO" -> "Atualização";
            case "MANUTENCAO" -> "Manutenção";
            case "PROMOCAO" -> "Promoção";
            case "ALERTA" -> "Alerta";
            case "OUTRO" -> "Outro";
            default -> tipo;
        };
    }

    private static String getDescricaoPrioridade(String prioridade) {
        return switch (prioridade) {
            case "BAIXA" -> "Baixa";
            case "NORMAL" -> "Normal";
            case "ALTA" -> "Alta";
            case "URGENTE" -> "Urgente";
            default -> prioridade;
        };
    }
}
