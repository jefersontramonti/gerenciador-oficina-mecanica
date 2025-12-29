package com.pitstop.saas.dto;

import java.util.List;
import java.util.UUID;

public record ToggleFeatureFlagRequest(
    Boolean habilitadoGlobal,
    List<String> planosHabilitar,
    List<String> planosDesabilitar,
    List<UUID> oficinasHabilitar,
    List<UUID> oficinasDesabilitar,
    Integer percentualRollout
) {}
