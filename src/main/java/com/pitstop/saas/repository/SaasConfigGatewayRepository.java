package com.pitstop.saas.repository;

import com.pitstop.saas.domain.ConfiguracaoGateway;
import com.pitstop.saas.domain.TipoGateway;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for SaaS payment gateway configuration.
 * Used by SUPER_ADMIN to receive payments from workshops.
 *
 * Named differently from financeiro's ConfiguracaoGatewayRepository
 * to avoid bean naming conflict.
 */
@Repository
public interface SaasConfigGatewayRepository extends JpaRepository<ConfiguracaoGateway, UUID> {

    /**
     * Find configuration by gateway type.
     */
    Optional<ConfiguracaoGateway> findByTipo(TipoGateway tipo);

    /**
     * Find active configuration by gateway type.
     */
    Optional<ConfiguracaoGateway> findByTipoAndAtivoTrue(TipoGateway tipo);

    /**
     * Check if a gateway type is configured and active.
     */
    boolean existsByTipoAndAtivoTrue(TipoGateway tipo);
}
