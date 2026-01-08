package com.pitstop.ia.domain;

/**
 * Provedores de IA suportados pelo sistema.
 */
public enum ProvedorIA {
    /**
     * Anthropic (Claude).
     */
    ANTHROPIC("Anthropic", "Claude"),

    /**
     * OpenAI (GPT) - Para expansão futura.
     */
    OPENAI("OpenAI", "GPT");

    private final String nome;
    private final String modeloBase;

    ProvedorIA(String nome, String modeloBase) {
        this.nome = nome;
        this.modeloBase = modeloBase;
    }

    public String getNome() {
        return nome;
    }

    public String getModeloBase() {
        return modeloBase;
    }
}
