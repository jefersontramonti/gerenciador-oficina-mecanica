package com.pitstop.financeiro.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Categorias de despesas operacionais da oficina.
 */
@Getter
@RequiredArgsConstructor
public enum CategoriaDespesa {

    // Despesas com Pessoal
    SALARIOS("Salários e Ordenados", "PESSOAL", "#3B82F6"),
    ENCARGOS_SOCIAIS("Encargos Sociais (INSS, FGTS)", "PESSOAL", "#60A5FA"),
    BENEFICIOS("Benefícios (VT, VR, Plano Saúde)", "PESSOAL", "#93C5FD"),
    PROLABORE("Pró-labore", "PESSOAL", "#2563EB"),

    // Despesas com Instalações
    ALUGUEL("Aluguel", "INSTALACOES", "#22C55E"),
    CONDOMINIO("Condomínio", "INSTALACOES", "#4ADE80"),
    IPTU("IPTU", "INSTALACOES", "#86EFAC"),
    MANUTENCAO_PREDIAL("Manutenção Predial", "INSTALACOES", "#16A34A"),

    // Utilidades
    ENERGIA_ELETRICA("Energia Elétrica", "UTILIDADES", "#F59E0B"),
    AGUA("Água e Esgoto", "UTILIDADES", "#FBBF24"),
    GAS("Gás", "UTILIDADES", "#FCD34D"),
    TELEFONE("Telefone", "UTILIDADES", "#F97316"),
    INTERNET("Internet", "UTILIDADES", "#FB923C"),

    // Despesas Operacionais
    COMPRA_PECAS("Compra de Peças (Estoque)", "OPERACIONAL", "#EF4444"),
    FERRAMENTAS("Ferramentas e Equipamentos", "OPERACIONAL", "#F87171"),
    MATERIAL_CONSUMO("Material de Consumo", "OPERACIONAL", "#FCA5A5"),
    MATERIAL_LIMPEZA("Material de Limpeza", "OPERACIONAL", "#FECACA"),
    DESCARTE_RESIDUOS("Descarte de Resíduos", "OPERACIONAL", "#DC2626"),

    // Despesas Administrativas
    CONTABILIDADE("Contabilidade", "ADMINISTRATIVO", "#8B5CF6"),
    ADVOCACIA("Advocacia", "ADMINISTRATIVO", "#A78BFA"),
    SISTEMAS_SOFTWARE("Sistemas e Software", "ADMINISTRATIVO", "#C4B5FD"),
    MATERIAL_ESCRITORIO("Material de Escritório", "ADMINISTRATIVO", "#7C3AED"),
    TAXAS_BANCARIAS("Taxas Bancárias", "ADMINISTRATIVO", "#6D28D9"),

    // Marketing e Vendas
    PUBLICIDADE("Publicidade e Propaganda", "MARKETING", "#EC4899"),
    MARKETING_DIGITAL("Marketing Digital", "MARKETING", "#F472B6"),
    BRINDES("Brindes e Cortesias", "MARKETING", "#F9A8D4"),

    // Impostos e Taxas
    IMPOSTOS_FEDERAIS("Impostos Federais", "IMPOSTOS", "#6B7280"),
    IMPOSTOS_ESTADUAIS("Impostos Estaduais", "IMPOSTOS", "#9CA3AF"),
    IMPOSTOS_MUNICIPAIS("Impostos Municipais", "IMPOSTOS", "#D1D5DB"),
    TAXAS_LICENCAS("Taxas e Licenças", "IMPOSTOS", "#4B5563"),

    // Veículos da Oficina
    COMBUSTIVEL("Combustível", "VEICULOS", "#14B8A6"),
    MANUTENCAO_VEICULOS("Manutenção de Veículos", "VEICULOS", "#2DD4BF"),
    SEGURO_VEICULOS("Seguro de Veículos", "VEICULOS", "#5EEAD4"),

    // Seguros
    SEGURO_ESTABELECIMENTO("Seguro do Estabelecimento", "SEGUROS", "#0EA5E9"),
    SEGURO_RESPONSABILIDADE("Seguro Responsabilidade Civil", "SEGUROS", "#38BDF8"),

    // Despesas Financeiras
    JUROS_EMPRESTIMOS("Juros de Empréstimos", "FINANCEIRO_DESPESA", "#78716C"),
    TARIFAS_CARTAO("Tarifas de Cartão", "FINANCEIRO_DESPESA", "#A8A29E"),
    MULTAS_ATRASOS("Multas por Atraso", "FINANCEIRO_DESPESA", "#D6D3D1"),

    // Receitas Financeiras (valores negativos representam receita)
    JUROS_RECEBIDOS("Juros Recebidos de Clientes", "FINANCEIRO_RECEITA", "#22C55E"),
    DESCONTOS_OBTIDOS("Descontos Obtidos de Fornecedores", "FINANCEIRO_RECEITA", "#4ADE80"),
    RENDIMENTOS_APLICACAO("Rendimentos de Aplicação", "FINANCEIRO_RECEITA", "#86EFAC"),

    // Outras Receitas (valores negativos representam receita)
    OUTRAS_RECEITAS("Outras Receitas", "RECEITAS", "#10B981"),

    // Outras Despesas
    OUTRAS_DESPESAS("Outras Despesas", "OUTROS", "#71717A");

    private final String descricao;
    private final String grupo;
    private final String cor;
}
