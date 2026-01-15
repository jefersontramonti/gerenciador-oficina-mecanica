package com.pitstop.shared.security.feature;

import java.lang.annotation.*;

/**
 * Annotation para marcar métodos ou classes que requerem uma feature flag específica.
 *
 * <p>Quando aplicada, o {@link FeatureGateAspect} verifica automaticamente se a
 * feature está habilitada para a oficina do usuário atual antes de executar o método.</p>
 *
 * <p>Exemplos de uso:</p>
 * <pre>
 * // Em um método específico
 * {@literal @}RequiresFeature("EMAIL_MARKETING")
 * public void enviarCampanhaEmail(CampanhaEmail campanha) {
 *     // ...
 * }
 *
 * // Em toda a classe (controller ou service)
 * {@literal @}RequiresFeature(value = "EMISSAO_NFE", name = "Emissão de NF-e")
 * {@literal @}RestController
 * public class NfeController {
 *     // ...
 * }
 *
 * // Com plano sugerido para upgrade
 * {@literal @}RequiresFeature(
 *     value = "RELATORIOS_GERENCIAIS",
 *     name = "Relatórios Gerenciais",
 *     requiredPlan = "PROFISSIONAL"
 * )
 * public void gerarRelatorioCompleto() {
 *     // ...
 * }
 * </pre>
 *
 * <p><b>Importante:</b> Esta annotation funciona em conjunto com o TenantContext.
 * Se o TenantContext não estiver definido (ex: SUPER_ADMIN), a verificação é ignorada.</p>
 *
 * @see FeatureGateAspect
 * @see FeatureNotEnabledException
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequiresFeature {

    /**
     * Código da feature flag (ex: "EMAIL_MARKETING", "EMISSAO_NFE").
     * Este é o identificador único da feature no banco de dados.
     */
    String value();

    /**
     * Nome amigável da feature para exibição em mensagens de erro.
     * Opcional - se não fornecido, usa o código da feature.
     */
    String name() default "";

    /**
     * Plano mínimo necessário para ter acesso à feature.
     * Usado para sugerir upgrade na mensagem de erro.
     * Valores possíveis: "ECONOMICO", "PROFISSIONAL", "TURBINADO"
     */
    String requiredPlan() default "";
}
