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
            // Templates de OS
            case OS_CREATED -> criarCorpoOSCriada(tipoNotificacao);
            case OS_WAITING_APPROVAL -> criarCorpoOSAguardandoAprovacao(tipoNotificacao);
            case OS_APPROVED -> criarCorpoOSAprovada(tipoNotificacao);
            case OS_IN_PROGRESS -> criarCorpoOSEmAndamento(tipoNotificacao);
            case OS_WAITING_PART -> criarCorpoOSAguardandoPeca(tipoNotificacao);
            case OS_COMPLETED -> criarCorpoOSFinalizada(tipoNotificacao);
            case OS_DELIVERED -> criarCorpoOSEntregue(tipoNotificacao);
            case PAYMENT_PENDING -> criarCorpoPagamentoPendente(tipoNotificacao);
            case REMINDER_PICKUP -> criarCorpoLembreteRetirada(tipoNotificacao);
            case REMINDER_MAINTENANCE -> criarCorpoLembreteRevisao(tipoNotificacao);
            case TEST -> criarCorpoTeste(tipoNotificacao);
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

    // ===== TEMPLATES DE ORDEM DE SERVICO =====

    private String criarCorpoOSCriada(TipoNotificacao tipo) {
        if (tipo == TipoNotificacao.EMAIL) {
            return """
                <h1>Orcamento Criado - OS #{numeroOS}</h1>
                <p>Ola {nomeCliente},</p>
                <p>Seu orcamento foi criado com sucesso!</p>
                <p><strong>Veiculo:</strong> {veiculoModelo} - {veiculoPlaca}</p>
                <p><strong>Valor Estimado:</strong> R$ {valorTotal}</p>
                <div style="margin: 30px 0; text-align: center;">
                    <a href="{linkAprovacao}" style="display: inline-block; padding: 16px 40px; background: linear-gradient(135deg, #28a745 0%, #20c997 100%); color: #ffffff; text-decoration: none; border-radius: 6px; font-size: 16px; font-weight: 600; box-shadow: 0 4px 6px rgba(40, 167, 69, 0.4);">Aprovar Orcamento</a>
                </div>
                <p style="font-size: 14px; color: #666;">Ou copie e cole o link abaixo no seu navegador:</p>
                <p style="font-size: 12px; padding: 12px; background-color: #f8f9fa; border-left: 4px solid #28a745; word-break: break-all; font-family: monospace;">{linkAprovacao}</p>
                <p>Atenciosamente,<br/>{nomeOficina}</p>
                """;
        }
        return """
            🔧 *Orcamento Criado - OS #{numeroOS}*

            Ola {nomeCliente}!

            Seu orcamento foi criado com sucesso.

            🚗 Veiculo: {veiculoModelo} - {veiculoPlaca}
            💰 Valor: R$ {valorTotal}

            ✅ Para aprovar, acesse: {linkAprovacao}

            {nomeOficina}
            """;
    }

    private String criarCorpoOSAguardandoAprovacao(TipoNotificacao tipo) {
        if (tipo == TipoNotificacao.EMAIL) {
            return """
                <h1>Orcamento Aguardando Aprovacao - OS #{numeroOS}</h1>
                <p>Ola {nomeCliente},</p>
                <p>Seu orcamento esta aguardando aprovacao.</p>
                <p><strong>Valor Total:</strong> R$ {valorTotal}</p>
                <p>Por favor, entre em contato para aprovar.</p>
                <p>Atenciosamente,<br/>{nomeOficina}</p>
                """;
        }
        return """
            ⏳ *Orcamento Aguardando Aprovacao*

            Ola {nomeCliente}!

            Seu orcamento (OS #{numeroOS}) esta aguardando sua aprovacao.

            💰 Valor: R$ {valorTotal}

            Entre em contato para aprovar!

            {nomeOficina}
            """;
    }

    private String criarCorpoOSAprovada(TipoNotificacao tipo) {
        if (tipo == TipoNotificacao.EMAIL) {
            return """
                <h1>Orcamento Aprovado - OS #{numeroOS}</h1>
                <p>Ola {nomeCliente},</p>
                <p>Seu orcamento foi aprovado com sucesso!</p>
                <p>Em breve iniciaremos os servicos.</p>
                <p>Atenciosamente,<br/>{nomeOficina}</p>
                """;
        }
        return """
            ✅ *Orcamento Aprovado - OS #{numeroOS}*

            Ola {nomeCliente}!

            Seu orcamento foi aprovado com sucesso!

            Em breve iniciaremos os servicos.

            {nomeOficina}
            """;
    }

    private String criarCorpoOSEmAndamento(TipoNotificacao tipo) {
        if (tipo == TipoNotificacao.EMAIL) {
            return """
                <h1>Servico Iniciado - OS #{numeroOS}</h1>
                <p>Ola {nomeCliente},</p>
                <p>O servico do seu veiculo {veiculoModelo} ({veiculoPlaca}) foi iniciado!</p>
                <p><strong>Mecanico responsavel:</strong> {mecanico}</p>
                <p><strong>Previsao de conclusao:</strong> {dataPrevisao}</p>
                <p>Atenciosamente,<br/>{nomeOficina}</p>
                """;
        }
        return """
            🔧 *Servico Iniciado - OS #{numeroOS}*

            Ola {nomeCliente}!

            O servico do seu veiculo foi iniciado!

            🚗 {veiculoModelo} - {veiculoPlaca}
            👨‍🔧 Mecanico: {mecanico}
            📅 Previsao: {dataPrevisao}

            {nomeOficina}
            """;
    }

    private String criarCorpoOSAguardandoPeca(TipoNotificacao tipo) {
        if (tipo == TipoNotificacao.EMAIL) {
            return """
                <h1>Aguardando Peca - OS #{numeroOS}</h1>
                <p>Ola {nomeCliente},</p>
                <p>O servico do seu veiculo esta aguardando a chegada de uma peca.</p>
                <p><strong>Peca:</strong> {pecaAguardada}</p>
                <p><strong>Previsao de chegada:</strong> {previsaoChegada}</p>
                <p>Atenciosamente,<br/>{nomeOficina}</p>
                """;
        }
        return """
            ⏳ *Aguardando Peca - OS #{numeroOS}*

            Ola {nomeCliente}!

            O servico esta aguardando uma peca.

            🔩 Peca: {pecaAguardada}
            📅 Previsao: {previsaoChegada}

            {nomeOficina}
            """;
    }

    private String criarCorpoOSFinalizada(TipoNotificacao tipo) {
        if (tipo == TipoNotificacao.EMAIL) {
            return """
                <h1>Veiculo Pronto - OS #{numeroOS}</h1>
                <p>Ola {nomeCliente},</p>
                <p>Otima noticia! O servico do seu veiculo {veiculoModelo} ({veiculoPlaca}) foi finalizado!</p>
                <p><strong>Servicos realizados:</strong> {servicosRealizados}</p>
                <p><strong>Valor Total:</strong> R$ {valorTotal}</p>
                <p>Seu veiculo esta pronto para retirada.</p>
                <p>Atenciosamente,<br/>{nomeOficina}</p>
                """;
        }
        return """
            🎉 *Veiculo Pronto - OS #{numeroOS}*

            Ola {nomeCliente}!

            Otima noticia! O servico foi finalizado!

            🚗 {veiculoModelo} - {veiculoPlaca}
            🔧 Servicos: {servicosRealizados}
            💰 Valor: R$ {valorTotal}

            Seu veiculo esta pronto para retirada!

            {nomeOficina}
            """;
    }

    private String criarCorpoOSEntregue(TipoNotificacao tipo) {
        if (tipo == TipoNotificacao.EMAIL) {
            return """
                <h1>Veiculo Entregue - OS #{numeroOS}</h1>
                <p>Ola {nomeCliente},</p>
                <p>Confirmamos a entrega do seu veiculo {veiculoPlaca}.</p>
                <p>Obrigado pela preferencia!</p>
                <p>Atenciosamente,<br/>{nomeOficina}</p>
                """;
        }
        return """
            ✅ *Veiculo Entregue - OS #{numeroOS}*

            Ola {nomeCliente}!

            Confirmamos a entrega do seu veiculo ({veiculoPlaca}).

            Obrigado pela preferencia! 🚗

            {nomeOficina}
            """;
    }

    private String criarCorpoPagamentoPendente(TipoNotificacao tipo) {
        if (tipo == TipoNotificacao.EMAIL) {
            return """
                <h1>Pagamento Pendente - OS #{numeroOS}</h1>
                <p>Ola {nomeCliente},</p>
                <p>Identificamos que ha um pagamento pendente.</p>
                <p><strong>Valor:</strong> R$ {valorPendente}</p>
                <p><strong>Vencimento:</strong> {dataVencimento}</p>
                <p>Atenciosamente,<br/>{nomeOficina}</p>
                """;
        }
        return """
            💳 *Pagamento Pendente - OS #{numeroOS}*

            Ola {nomeCliente}!

            Ha um pagamento pendente.

            💰 Valor: R$ {valorPendente}
            📅 Vencimento: {dataVencimento}

            {nomeOficina}
            """;
    }

    private String criarCorpoLembreteRetirada(TipoNotificacao tipo) {
        if (tipo == TipoNotificacao.EMAIL) {
            return """
                <h1>Lembrete: Seu Veiculo Esta Pronto!</h1>
                <p>Ola {nomeCliente},</p>
                <p>Seu veiculo ({veiculoPlaca}) esta pronto ha {diasEsperando} dias.</p>
                <p>Por favor, agende a retirada.</p>
                <p>Atenciosamente,<br/>{nomeOficina}</p>
                """;
        }
        return """
            ⏰ *Lembrete: Veiculo Pronto!*

            Ola {nomeCliente}!

            Seu veiculo ({veiculoPlaca}) esta pronto ha {diasEsperando} dias.

            Por favor, agende a retirada!

            {nomeOficina}
            """;
    }

    private String criarCorpoLembreteRevisao(TipoNotificacao tipo) {
        if (tipo == TipoNotificacao.EMAIL) {
            return """
                <h1>Hora da Revisao!</h1>
                <p>Ola {nomeCliente},</p>
                <p>Esta na hora de fazer a revisao do seu {veiculoModelo} ({veiculoPlaca}).</p>
                <p><strong>Quilometragem atual:</strong> {quilometragemAtual} km</p>
                <p><strong>Proxima revisao:</strong> {proximaRevisao}</p>
                <p>Agende sua visita!</p>
                <p>Atenciosamente,<br/>{nomeOficina}</p>
                """;
        }
        return """
            🔧 *Hora da Revisao!*

            Ola {nomeCliente}!

            Seu {veiculoModelo} ({veiculoPlaca}) precisa de revisao.

            🚗 KM atual: {quilometragemAtual}
            📅 Proxima revisao: {proximaRevisao}

            Agende sua visita!

            {nomeOficina}
            """;
    }

    private String criarCorpoTeste(TipoNotificacao tipo) {
        if (tipo == TipoNotificacao.EMAIL) {
            return """
                <h1>Teste de Notificacao - PitStop</h1>
                <p>Esta e uma mensagem de teste.</p>
                <p>Se voce recebeu este e-mail, a configuracao de e-mail esta funcionando corretamente!</p>
                <p>Atenciosamente,<br/>{nomeOficina}</p>
                """;
        }
        return """
            🧪 *Teste de Notificacao - PitStop*

            Esta e uma mensagem de teste.

            Se voce recebeu esta mensagem, a configuracao esta funcionando corretamente!

            ✅ {nomeOficina}
            """;
    }
}
