package com.pitstop.financeiro.domain;

/**
 * Status específicos para pagamentos via gateway online.
 */
public enum StatusPagamentoOnline {

    PENDENTE("Pendente", "Aguardando pagamento"),
    PROCESSANDO("Processando", "Pagamento em processamento"),
    APROVADO("Aprovado", "Pagamento aprovado"),
    AUTORIZADO("Autorizado", "Pagamento autorizado"),
    EM_ANALISE("Em Análise", "Pagamento em análise de fraude"),
    REJEITADO("Rejeitado", "Pagamento rejeitado"),
    CANCELADO("Cancelado", "Pagamento cancelado"),
    ESTORNADO("Estornado", "Pagamento estornado"),
    DEVOLVIDO("Devolvido", "Pagamento devolvido"),
    EXPIRADO("Expirado", "Pagamento expirado");

    private final String descricao;
    private final String detalhe;

    StatusPagamentoOnline(String descricao, String detalhe) {
        this.descricao = descricao;
        this.detalhe = detalhe;
    }

    public String getDescricao() {
        return descricao;
    }

    public String getDetalhe() {
        return detalhe;
    }

    public boolean isFinal() {
        return this == APROVADO || this == REJEITADO ||
               this == CANCELADO || this == ESTORNADO ||
               this == DEVOLVIDO || this == EXPIRADO;
    }

    public boolean isAprovado() {
        return this == APROVADO || this == AUTORIZADO;
    }
}
