package com.pitstop.saas.dto;

import com.pitstop.saas.domain.PrioridadeComunicado;
import com.pitstop.saas.domain.StatusComunicado;
import com.pitstop.saas.domain.TipoComunicado;

public record ComunicadoFilterRequest(
    StatusComunicado status,
    TipoComunicado tipo,
    PrioridadeComunicado prioridade,
    String busca,
    int page,
    int size
) {
    public ComunicadoFilterRequest {
        if (page < 0) page = 0;
        if (size <= 0 || size > 100) size = 20;
    }
}
