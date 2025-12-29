package com.pitstop.saas.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Request for executing mass actions on defaulting workshops.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AcaoMassaInadimplenciaRequest {

    @NotEmpty(message = "Lista de oficinas é obrigatória")
    private List<UUID> oficinaIds;

    @NotNull(message = "Ação é obrigatória")
    private AcaoInadimplencia acao;

    private String mensagemCustomizada;

    /**
     * Available mass actions.
     */
    public enum AcaoInadimplencia {
        NOTIFICAR("Enviar Notificação"),
        NOTIFICAR_URGENTE("Enviar Notificação Urgente"),
        SUSPENDER("Suspender Acesso"),
        REATIVAR("Reativar Acesso"),
        CANCELAR("Cancelar Assinatura");

        private final String label;

        AcaoInadimplencia(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }
}
