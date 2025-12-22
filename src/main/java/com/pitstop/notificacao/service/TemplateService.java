package com.pitstop.notificacao.service;

import com.pitstop.notificacao.domain.TemplateCustomizado;
import com.pitstop.notificacao.domain.TemplateNotificacao;
import com.pitstop.notificacao.domain.TipoNotificacao;
import com.pitstop.notificacao.repository.TemplateCustomizadoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Serviço para gerenciamento de templates de notificação.
 *
 * Implementa lógica de fallback:
 * 1. Busca template customizado da oficina
 * 2. Se não encontrar, busca template padrão do sistema
 * 3. Se não encontrar, usa template hardcoded (fallback final)
 *
 * @author PitStop Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TemplateService {

    private final TemplateCustomizadoRepository templateRepository;

    /**
     * Obtém template para uma oficina específica.
     *
     * Lógica de fallback:
     * 1. Template customizado da oficina (se existir)
     * 2. Template padrão do sistema (se existir)
     * 3. Template hardcoded (sempre existe)
     *
     * @param oficinaId ID da oficina (pode ser null para usar apenas padrões)
     * @param tipoTemplate Tipo de template
     * @param tipoNotificacao Canal de notificação
     * @return Template encontrado ou criado
     */
    public TemplateCustomizado obterTemplate(
        UUID oficinaId,
        TemplateNotificacao tipoTemplate,
        TipoNotificacao tipoNotificacao
    ) {
        // 1. Tenta buscar template customizado da oficina
        if (oficinaId != null) {
            Optional<TemplateCustomizado> customizado = templateRepository
                .findByOficinaIdAndTipoTemplateAndTipoNotificacaoAndAtivoTrue(
                    oficinaId,
                    tipoTemplate,
                    tipoNotificacao
                );

            if (customizado.isPresent()) {
                log.debug("Usando template customizado da oficina {} para {}",
                    oficinaId, tipoTemplate);
                return customizado.get();
            }
        }

        // 2. Tenta buscar template padrão do sistema
        Optional<TemplateCustomizado> padrao = templateRepository
            .findTemplatePadrao(tipoTemplate, tipoNotificacao);

        if (padrao.isPresent()) {
            log.debug("Usando template padrão do sistema para {}", tipoTemplate);
            return padrao.get();
        }

        // 3. Fallback: cria template hardcoded em memória (não salva no BD)
        log.debug("Usando template hardcoded para {}", tipoTemplate);
        return criarTemplateHardcoded(tipoTemplate, tipoNotificacao);
    }

    /**
     * Processa template substituindo variáveis.
     *
     * @param template Template a processar
     * @param variaveis Variáveis a substituir
     * @return Conteúdo processado
     */
    public String processarCorpo(TemplateCustomizado template, Map<String, Object> variaveis) {
        return template.processarCorpo(variaveis);
    }

    /**
     * Processa assunto substituindo variáveis.
     *
     * @param template Template
     * @param variaveis Variáveis
     * @return Assunto processado
     */
    public String processarAssunto(TemplateCustomizado template, Map<String, Object> variaveis) {
        return template.processarAssunto(variaveis);
    }

    /**
     * Cria template hardcoded como fallback final.
     *
     * Garante que sempre há um template disponível, mesmo que
     * não esteja cadastrado no banco de dados.
     *
     * @param tipoTemplate Tipo de template
     * @param tipoNotificacao Canal
     * @return Template hardcoded
     */
    private TemplateCustomizado criarTemplateHardcoded(
        TemplateNotificacao tipoTemplate,
        TipoNotificacao tipoNotificacao
    ) {
        String corpo = switch (tipoTemplate) {
            case OFICINA_WELCOME -> criarCorpoWelcome(tipoNotificacao);
            case TRIAL_EXPIRING -> criarCorpoTrialExpiring(tipoNotificacao);
            case TRIAL_EXPIRED -> criarCorpoTrialExpired(tipoNotificacao);
            case PAYMENT_OVERDUE -> criarCorpoPaymentOverdue(tipoNotificacao);
            case PAYMENT_CONFIRMED -> criarCorpoPaymentConfirmed(tipoNotificacao);
            case OFICINA_SUSPENDED -> criarCorpoSuspended(tipoNotificacao);
            case OFICINA_ACTIVATED -> criarCorpoActivated(tipoNotificacao);
            case DAILY_METRICS, SYSTEM_ALERT -> "Mensagem: {mensagem}";
        };

        return TemplateCustomizado.builder()
            .oficinaId(null)
            .tipoTemplate(tipoTemplate)
            .tipoNotificacao(tipoNotificacao)
            .assunto(tipoTemplate.getSubject())
            .corpo(corpo)
            .ativo(true)
            .observacoes("Template hardcoded (fallback)")
            .build();
    }

    private String criarCorpoWelcome(TipoNotificacao tipo) {
        if (tipo == TipoNotificacao.EMAIL) {
            return """
                <h1>Bem-vindo ao PitStop, {nomeOficina}!</h1>
                <p>Seu período trial de 7 dias começou. Explore todas as funcionalidades!</p>
                <p>Data de vencimento: {dataVencimento}</p>
                """;
        }
        return """
            🚗 *Bem-vindo ao PitStop, {nomeOficina}!*

            Seu período trial de 7 dias começou! 🎉
            Explore todas as funcionalidades do sistema.

            ⏰ Vencimento: {dataVencimento}
            """;
    }

    private String criarCorpoTrialExpiring(TipoNotificacao tipo) {
        if (tipo == TipoNotificacao.EMAIL) {
            return """
                <h1>Seu período trial está terminando</h1>
                <p>Faltam apenas {diasRestantes} dias para o fim do seu trial.</p>
                <p>Escolha um plano para continuar usando o PitStop!</p>
                """;
        }
        return """
            ⏰ *Seu período trial está terminando!*

            Faltam apenas *{diasRestantes} dias* para o fim do trial.

            Escolha um plano para continuar aproveitando o PitStop! 🚀
            """;
    }

    private String criarCorpoTrialExpired(TipoNotificacao tipo) {
        if (tipo == TipoNotificacao.EMAIL) {
            return """
                <h1>Seu período trial expirou</h1>
                <p>Para continuar usando o PitStop, escolha um de nossos planos.</p>
                """;
        }
        return """
            ⚠️ *Seu período trial expirou*

            Para continuar usando o PitStop, escolha um de nossos planos.
            """;
    }

    private String criarCorpoPaymentOverdue(TipoNotificacao tipo) {
        if (tipo == TipoNotificacao.EMAIL) {
            return """
                <h1>Pagamento em Atraso</h1>
                <p>Identificamos que o pagamento da sua mensalidade está em atraso.</p>
                <p>Valor: {valor}</p>
                <p>Vencimento: {dataVencimento}</p>
                <p>Dias em atraso: {diasAtraso}</p>
                """;
        }
        return """
            🔴 *Pagamento em Atraso*

            Valor: R$ {valor}
            Vencimento: {dataVencimento}
            Dias em atraso: {diasAtraso}

            Regularize o quanto antes para evitar suspensão!
            """;
    }

    private String criarCorpoPaymentConfirmed(TipoNotificacao tipo) {
        if (tipo == TipoNotificacao.EMAIL) {
            return """
                <h1>Pagamento Confirmado!</h1>
                <p>Seu pagamento de {valor} foi confirmado com sucesso.</p>
                <p>Referência: {referencia}</p>
                """;
        }
        return """
            ✅ *Pagamento Confirmado!*

            Valor: R$ {valor}
            Referência: {referencia}

            Obrigado por escolher o PitStop! 🚗
            """;
    }

    private String criarCorpoSuspended(TipoNotificacao tipo) {
        if (tipo == TipoNotificacao.EMAIL) {
            return """
                <h1>Conta Suspensa</h1>
                <p>Sua conta foi suspensa devido a pagamento em atraso.</p>
                <p>Regularize o pagamento para reativar o acesso.</p>
                """;
        }
        return """
            🔒 *Conta Suspensa*

            Sua conta foi suspensa devido a pagamento em atraso.

            Regularize o pagamento para reativar o acesso.
            """;
    }

    private String criarCorpoActivated(TipoNotificacao tipo) {
        if (tipo == TipoNotificacao.EMAIL) {
            return """
                <h1>Conta Reativada!</h1>
                <p>Sua conta foi reativada com sucesso. Bem-vindo de volta!</p>
                """;
        }
        return """
            ✅ *Conta Reativada!*

            Sua conta foi reativada com sucesso.

            Bem-vindo de volta ao PitStop! 🚗
            """;
    }
}
