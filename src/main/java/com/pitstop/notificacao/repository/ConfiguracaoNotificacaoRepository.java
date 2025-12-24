package com.pitstop.notificacao.repository;

import com.pitstop.notificacao.domain.ConfiguracaoNotificacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository para configuracoes de notificacao por oficina.
 *
 * @author PitStop Team
 */
@Repository
public interface ConfiguracaoNotificacaoRepository extends JpaRepository<ConfiguracaoNotificacao, UUID> {

    /**
     * Busca a configuracao de uma oficina.
     * Cada oficina tem no maximo uma configuracao.
     *
     * @param oficinaId ID da oficina
     * @return Configuracao se encontrada
     */
    Optional<ConfiguracaoNotificacao> findByOficinaId(UUID oficinaId);

    /**
     * Busca configuracao ativa de uma oficina.
     *
     * @param oficinaId ID da oficina
     * @return Configuracao ativa se encontrada
     */
    Optional<ConfiguracaoNotificacao> findByOficinaIdAndAtivoTrue(UUID oficinaId);

    /**
     * Verifica se oficina tem configuracao.
     *
     * @param oficinaId ID da oficina
     * @return true se existe
     */
    boolean existsByOficinaId(UUID oficinaId);

    /**
     * Lista todas as oficinas com WhatsApp habilitado.
     *
     * @return Lista de configuracoes
     */
    @Query("""
        SELECT c FROM ConfiguracaoNotificacao c
        WHERE c.whatsappHabilitado = true
          AND c.ativo = true
        """)
    List<ConfiguracaoNotificacao> findAllWithWhatsappEnabled();

    /**
     * Lista todas as oficinas com Evolution API configurada.
     *
     * @return Lista de configuracoes
     */
    @Query("""
        SELECT c FROM ConfiguracaoNotificacao c
        WHERE c.evolutionApiUrl IS NOT NULL
          AND c.evolutionApiToken IS NOT NULL
          AND c.evolutionInstanceName IS NOT NULL
          AND c.ativo = true
        """)
    List<ConfiguracaoNotificacao> findAllWithEvolutionApiConfigured();

    /**
     * Conta oficinas com WhatsApp habilitado.
     *
     * @return Quantidade
     */
    @Query("""
        SELECT COUNT(c) FROM ConfiguracaoNotificacao c
        WHERE c.whatsappHabilitado = true
          AND c.ativo = true
        """)
    long countWithWhatsappEnabled();

    /**
     * Conta oficinas com email habilitado.
     *
     * @return Quantidade
     */
    @Query("""
        SELECT COUNT(c) FROM ConfiguracaoNotificacao c
        WHERE c.emailHabilitado = true
          AND c.ativo = true
        """)
    long countWithEmailEnabled();

    /**
     * Lista oficinas em modo simulacao.
     *
     * @return Lista de configuracoes em modo simulacao
     */
    List<ConfiguracaoNotificacao> findByModoSimulacaoTrueAndAtivoTrue();

    /**
     * Busca oficinas com SMTP proprio configurado.
     *
     * @return Lista de configuracoes
     */
    @Query("""
        SELECT c FROM ConfiguracaoNotificacao c
        WHERE c.smtpHost IS NOT NULL
          AND c.smtpUsername IS NOT NULL
          AND c.ativo = true
        """)
    List<ConfiguracaoNotificacao> findAllWithCustomSmtp();
}
