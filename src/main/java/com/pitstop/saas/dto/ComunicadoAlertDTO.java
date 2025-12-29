package com.pitstop.saas.dto;

import java.util.List;

public record ComunicadoAlertDTO(
    long totalNaoLidos,
    int pendentesConfirmacao,
    List<ComunicadoOficinaDTO> urgentes
) {
    public boolean temAlerta() {
        return totalNaoLidos > 0 || pendentesConfirmacao > 0;
    }
}
