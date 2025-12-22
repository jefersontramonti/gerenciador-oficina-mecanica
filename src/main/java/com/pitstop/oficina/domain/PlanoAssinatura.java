package com.pitstop.oficina.domain;

import java.math.BigDecimal;

/**
 * Subscription plans available for workshops in PitStop system.
 *
 * <p><b>Plan Features Comparison:</b></p>
 * <table border="1">
 *   <tr>
 *     <th>Feature</th>
 *     <th>Econômico (R$ 160)</th>
 *     <th>Profissional (R$ 250)</th>
 *     <th>Turbinado (Custom)</th>
 *   </tr>
 *   <tr>
 *     <td>Users</td>
 *     <td>1</td>
 *     <td>3</td>
 *     <td>Unlimited</td>
 *   </tr>
 *   <tr>
 *     <td>Invoice Emission (NF-e/NFS-e/NFC-e)</td>
 *     <td>NO</td>
 *     <td>YES</td>
 *     <td>YES</td>
 *   </tr>
 *   <tr>
 *     <td>WhatsApp Automation</td>
 *     <td>NO</td>
 *     <td>NO</td>
 *     <td>YES</td>
 *   </tr>
 *   <tr>
 *     <td>Preventive Maintenance</td>
 *     <td>NO</td>
 *     <td>NO</td>
 *     <td>YES</td>
 *   </tr>
 *   <tr>
 *     <td>Image/Document Attachments</td>
 *     <td>NO</td>
 *     <td>NO</td>
 *     <td>YES</td>
 *   </tr>
 * </table>
 *
 * <p><b>All plans include:</b> Cash control, automatic backup, stock control,
 * service order control, financial control, Motorok app, reports, online support,
 * remanufacturing, automatic emails, online training, store sales.</p>
 *
 * @since 1.0.0
 */
public enum PlanoAssinatura {

    /**
     * Economic plan - R$ 160.00/month
     * <ul>
     *   <li>1 user</li>
     *   <li>NO invoice emission</li>
     *   <li>NO WhatsApp automation</li>
     *   <li>NO preventive maintenance</li>
     *   <li>NO image/document attachments</li>
     *   <li>All basic features included</li>
     * </ul>
     */
    ECONOMICO(
        "Econômico",
        new BigDecimal("160.00"),
        1,              // maxUsuarios
        -1,             // maxOrdensServico (unlimited)
        -1,             // maxClientes (unlimited)
        false,          // emiteNotaFiscal
        false,          // whatsappAutomatizado
        false,          // manutencaoPreventiva
        false           // anexoImagensDocumentos
    ),

    /**
     * Professional plan - R$ 250.00/month (Most Popular)
     * <ul>
     *   <li>3 users</li>
     *   <li>WITH invoice emission (NF-e, NFS-e, NFC-e)</li>
     *   <li>NO WhatsApp automation</li>
     *   <li>NO preventive maintenance</li>
     *   <li>NO image/document attachments</li>
     *   <li>All basic features included</li>
     * </ul>
     */
    PROFISSIONAL(
        "Profissional",
        new BigDecimal("250.00"),
        3,              // maxUsuarios
        -1,             // maxOrdensServico (unlimited)
        -1,             // maxClientes (unlimited)
        true,           // emiteNotaFiscal
        false,          // whatsappAutomatizado
        false,          // manutencaoPreventiva
        false           // anexoImagensDocumentos
    ),

    /**
     * Turbinado plan - Custom pricing (contact sales)
     * <ul>
     *   <li>Unlimited users</li>
     *   <li>WITH invoice emission (NF-e, NFS-e, NFC-e)</li>
     *   <li>WITH WhatsApp automation</li>
     *   <li>WITH preventive maintenance</li>
     *   <li>WITH image/document attachments</li>
     *   <li>All features + customizations</li>
     * </ul>
     */
    TURBINADO(
        "Turbinado",
        BigDecimal.ZERO, // Custom pricing - to be negotiated
        -1,             // maxUsuarios (unlimited)
        -1,             // maxOrdensServico (unlimited)
        -1,             // maxClientes (unlimited)
        true,           // emiteNotaFiscal
        true,           // whatsappAutomatizado
        true,           // manutencaoPreventiva
        true            // anexoImagensDocumentos
    );

    private final String nome;
    private final BigDecimal valorMensal;
    private final int maxUsuarios;
    private final int maxOrdensServico;
    private final int maxClientes;
    private final boolean emiteNotaFiscal;
    private final boolean whatsappAutomatizado;
    private final boolean manutencaoPreventiva;
    private final boolean anexoImagensDocumentos;

    PlanoAssinatura(String nome, BigDecimal valorMensal, int maxUsuarios,
                    int maxOrdensServico, int maxClientes, boolean emiteNotaFiscal,
                    boolean whatsappAutomatizado, boolean manutencaoPreventiva,
                    boolean anexoImagensDocumentos) {
        this.nome = nome;
        this.valorMensal = valorMensal;
        this.maxUsuarios = maxUsuarios;
        this.maxOrdensServico = maxOrdensServico;
        this.maxClientes = maxClientes;
        this.emiteNotaFiscal = emiteNotaFiscal;
        this.whatsappAutomatizado = whatsappAutomatizado;
        this.manutencaoPreventiva = manutencaoPreventiva;
        this.anexoImagensDocumentos = anexoImagensDocumentos;
    }

    public String getNome() {
        return nome;
    }

    public BigDecimal getValorMensal() {
        return valorMensal;
    }

    public int getMaxUsuarios() {
        return maxUsuarios;
    }

    public int getMaxOrdensServico() {
        return maxOrdensServico;
    }

    public int getMaxClientes() {
        return maxClientes;
    }

    public boolean isEmiteNotaFiscal() {
        return emiteNotaFiscal;
    }

    public boolean isWhatsappAutomatizado() {
        return whatsappAutomatizado;
    }

    public boolean isManutencaoPreventiva() {
        return manutencaoPreventiva;
    }

    public boolean isAnexoImagensDocumentos() {
        return anexoImagensDocumentos;
    }

    /**
     * Checks if the plan has unlimited users.
     *
     * @return true if maxUsuarios is -1 (unlimited)
     */
    public boolean isUsuariosIlimitados() {
        return maxUsuarios == -1;
    }

    /**
     * Checks if the user limit has been reached for this plan.
     *
     * @param currentUserCount current number of users
     * @return true if limit reached, false if within limit or unlimited
     */
    public boolean isLimiteUsuariosAtingido(int currentUserCount) {
        if (isUsuariosIlimitados()) {
            return false;
        }
        return currentUserCount >= maxUsuarios;
    }

    /**
     * Gets the description of the plan for display purposes.
     *
     * @return formatted description with price and key features
     */
    public String getDescricao() {
        if (this == TURBINADO) {
            return nome + " - Sob consulta (Usuários ilimitados + todas as features)";
        }
        return nome + " - R$ " + valorMensal + "/mês (" + maxUsuarios + " usuário" + (maxUsuarios > 1 ? "s" : "") + ")";
    }
}
