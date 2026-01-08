package com.pitstop.ia.service;

import com.pitstop.ia.dto.DiagnosticoIAResponse;
import com.pitstop.ia.dto.DiagnosticoIAResponse.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Service de pré-validação que detecta problemas comuns e retorna templates pré-definidos.
 * Evita chamadas à API de IA para problemas rotineiros.
 */
@Service
@Slf4j
public class PreValidacaoService {

    /**
     * Mapa de padrões de problemas comuns para templates de diagnóstico.
     */
    private static final List<TemplateProblema> TEMPLATES = List.of(
            // ===== TROCA DE ÓLEO =====
            new TemplateProblema(
                    List.of("troca de [oó]leo", "trocar [oó]leo", "troca [oó]leo", "[oó]leo motor"),
                    criarDiagnosticoTrocaOleo()
            ),
            // ===== REVISÃO =====
            new TemplateProblema(
                    List.of("revis[aã]o", "revis[aã]o completa", "revis[aã]o geral", "check.?up"),
                    criarDiagnosticoRevisao()
            ),
            // ===== FREIO COM BARULHO =====
            new TemplateProblema(
                    List.of("barulho.{0,10}freio", "freio.{0,10}barulho", "rangendo.{0,10}freio",
                            "chiando.{0,10}freio", "freio.{0,10}chiando", "pastilha.{0,10}freio"),
                    criarDiagnosticoFreioBarulho()
            ),
            // ===== BATERIA =====
            new TemplateProblema(
                    List.of("bateria.{0,10}(fraca|descarreg|não pega)", "não liga.{0,10}bateria",
                            "carro não pega", "motor não liga"),
                    criarDiagnosticoBateria()
            ),
            // ===== PNEU =====
            new TemplateProblema(
                    List.of("trocar pneu", "pneu careca", "pneu gast", "calibrar pneu",
                            "alinhamento", "balanceamento"),
                    criarDiagnosticoPneu()
            ),
            // ===== AR CONDICIONADO =====
            new TemplateProblema(
                    List.of("ar.?condicionado.{0,10}(não gela|fraco|barulho)",
                            "ar.?condicionado.{0,10}não funciona", "recarga.{0,10}ar"),
                    criarDiagnosticoArCondicionado()
            ),
            // ===== SUSPENSÃO =====
            new TemplateProblema(
                    List.of("suspens[aã]o.{0,10}barulho", "barulho.{0,10}suspens",
                            "amortecedor", "carro.{0,10}(tremendo|balançando)"),
                    criarDiagnosticoSuspensao()
            ),
            // ===== EMBREAGEM =====
            new TemplateProblema(
                    List.of("embreagem.{0,10}(patinando|dura|mole)", "pedal.{0,10}embreagem"),
                    criarDiagnosticoEmbreagem()
            ),
            // ===== CORREIA DENTADA =====
            new TemplateProblema(
                    List.of("correia dentada", "trocar correia", "barulho.{0,10}correia"),
                    criarDiagnosticoCorreiaDentada()
            ),
            // ===== VELA / IGNIÇÃO =====
            new TemplateProblema(
                    List.of("vela.{0,10}(ignição|troca)", "motor.{0,10}falhando",
                            "carro.{0,10}engasgando", "falha.{0,10}ignição"),
                    criarDiagnosticoVelaIgnicao()
            )
    );

    // ===== SINTOMAS QUE INDICAM PROBLEMA COMPLEXO (NUNCA usar template) =====
    private static final List<Pattern> SINTOMAS_COMPLEXOS = List.of(
            // Motor/performance
            Pattern.compile("falhando|falha|engasga|morre|morreu|apaga|desliga sozinho", Pattern.CASE_INSENSITIVE),
            Pattern.compile("superaquec|esquent|aquecendo|temperatura (alta|subindo)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("fumaça|fumacento|fumaceira", Pattern.CASE_INSENSITIVE),
            Pattern.compile("vazando|vaza|vazamento", Pattern.CASE_INSENSITIVE),
            Pattern.compile("barulho forte|pancada|batendo|estalo|estourou", Pattern.CASE_INSENSITIVE),
            Pattern.compile("não acelera|sem força|perdeu força|sem potência", Pattern.CASE_INSENSITIVE),
            Pattern.compile("treme|vibra(ção)?\\s+forte|sacudindo", Pattern.CASE_INSENSITIVE),
            Pattern.compile("luz acesa|painel aceso|alerta|luz do motor", Pattern.CASE_INSENSITIVE),
            Pattern.compile("consumo (alto|aumentou|muito)|gastando muito|bebendo muito", Pattern.CASE_INSENSITIVE),
            Pattern.compile("queimando|cheiro de queimado|fedendo", Pattern.CASE_INSENSITIVE),
            // Múltiplos problemas
            Pattern.compile("vários problemas|diversos problemas|múltiplos|além disso|também (tem|está)", Pattern.CASE_INSENSITIVE),
            // Urgência
            Pattern.compile("urgente|emergência|travou|parou de funcionar|não funciona mais", Pattern.CASE_INSENSITIVE)
    );

    // ===== CONTEXTOS QUE INDICAM MENÇÃO HISTÓRICA (não é pedido) =====
    private static final List<Pattern> CONTEXTO_HISTORICO = List.of(
            Pattern.compile("sempre (fiz|fazia|faço|faço)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("já (fiz|fez|troquei|levei)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("última (revisão|troca|vez)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("costum(o|ava)|antes|antigamente", Pattern.CASE_INSENSITIVE),
            Pattern.compile("na concessionária|na autorizada|no mecânico", Pattern.CASE_INSENSITIVE),
            Pattern.compile("ano passado|mês passado|semana passada", Pattern.CASE_INSENSITIVE)
    );

    /**
     * Tenta encontrar um template pré-definido para o problema.
     *
     * REGRAS DE SEGURANÇA:
     * 1. Texto longo (>120 chars) = problema complexo → IA
     * 2. Sintomas graves detectados → IA
     * 3. Contexto histórico sem pedido explícito → IA
     * 4. Apenas pedidos SIMPLES e CLAROS usam template
     *
     * @param problema Descrição do problema
     * @return Optional com diagnóstico se encontrar match SEGURO
     */
    public Optional<DiagnosticoIAResponse> tentarResolverSemIA(String problema) {
        if (problema == null || problema.isBlank()) {
            return Optional.empty();
        }

        String problemaLower = problema.toLowerCase().trim();
        log.debug("Pré-validação iniciada para: {}", truncar(problemaLower, 80));

        // ===== REGRA 1: Texto muito longo = problema complexo =====
        if (problemaLower.length() > 120) {
            log.debug("Texto muito longo ({} chars) - enviando para IA", problemaLower.length());
            return Optional.empty();
        }

        // ===== REGRA 2: Detectar SINTOMAS GRAVES =====
        for (Pattern sintoma : SINTOMAS_COMPLEXOS) {
            if (sintoma.matcher(problemaLower).find()) {
                log.info("Sintoma complexo detectado: '{}' - enviando para IA", sintoma.pattern());
                return Optional.empty();
            }
        }

        // ===== REGRA 3: Verificar contexto histórico =====
        boolean temContextoHistorico = CONTEXTO_HISTORICO.stream()
                .anyMatch(ctx -> ctx.matcher(problemaLower).find());

        if (temContextoHistorico) {
            log.debug("Contexto histórico detectado - enviando para IA para análise completa");
            return Optional.empty();
        }

        // ===== REGRA 4: Buscar templates apenas para pedidos SIMPLES =====
        for (TemplateProblema template : TEMPLATES) {
            for (String padrao : template.padroes()) {
                Pattern pattern = Pattern.compile(padrao, Pattern.CASE_INSENSITIVE);
                if (pattern.matcher(problemaLower).find()) {
                    log.info("Template encontrado para pedido simples: '{}' -> padrão: {}",
                            truncar(problema, 50), padrao);
                    return Optional.of(template.diagnostico());
                }
            }
        }

        log.debug("Nenhum template aplicável - enviando para IA");
        return Optional.empty();
    }

    /**
     * Trunca texto para log.
     */
    private String truncar(String texto, int max) {
        if (texto == null) return "";
        return texto.length() > max ? texto.substring(0, max) + "..." : texto;
    }

    // ===== TEMPLATES PRÉ-DEFINIDOS =====

    private static DiagnosticoIAResponse criarDiagnosticoTrocaOleo() {
        return new DiagnosticoIAResponse(
                "Manutenção preventiva de lubrificação do motor. A troca de óleo é essencial para o bom funcionamento e longevidade do motor.",
                List.of(
                        new CausaPossivel("Quilometragem atingida para troca de óleo (5.000-10.000 km)", 95, Gravidade.BAIXA),
                        new CausaPossivel("Óleo degradado pelo tempo de uso", 80, Gravidade.BAIXA)
                ),
                List.of(
                        "Drenar óleo usado do cárter",
                        "Substituir filtro de óleo",
                        "Adicionar óleo novo conforme especificação do fabricante",
                        "Verificar nível e possíveis vazamentos",
                        "Atualizar adesivo de próxima troca"
                ),
                List.of(
                        new PecaProvavel("Óleo de motor (sintético/semissintético)", "OLE001", Urgencia.ALTA, new BigDecimal("150.00")),
                        new PecaProvavel("Filtro de óleo", "FIL001", Urgencia.ALTA, new BigDecimal("35.00")),
                        new PecaProvavel("Arruela do bujão do cárter", "ARR001", Urgencia.MEDIA, new BigDecimal("5.00"))
                ),
                "30-45 minutos",
                new FaixaCusto(new BigDecimal("150.00"), new BigDecimal("350.00"), "BRL"),
                new MetadadosDiagnostico(Origem.TEMPLATE, null, 0, BigDecimal.ZERO, 0L)
        );
    }

    private static DiagnosticoIAResponse criarDiagnosticoRevisao() {
        return new DiagnosticoIAResponse(
                "Revisão preventiva geral do veículo. Inclui verificação completa dos sistemas principais.",
                List.of(
                        new CausaPossivel("Manutenção preventiva periódica", 100, Gravidade.BAIXA),
                        new CausaPossivel("Possíveis desgastes naturais de componentes", 60, Gravidade.BAIXA)
                ),
                List.of(
                        "Trocar óleo e filtro de óleo",
                        "Verificar e substituir filtro de ar se necessário",
                        "Verificar filtro de combustível",
                        "Verificar filtro de ar condicionado",
                        "Checar nível e condição de fluidos (freio, direção, arrefecimento)",
                        "Inspecionar correias e mangueiras",
                        "Verificar sistema de freios (pastilhas e discos)",
                        "Checar suspensão e direção",
                        "Verificar iluminação completa",
                        "Calibrar pneus e verificar desgaste"
                ),
                List.of(
                        new PecaProvavel("Kit revisão (óleo + filtros)", "KIT001", Urgencia.ALTA, new BigDecimal("300.00")),
                        new PecaProvavel("Filtro de ar do motor", "FIL002", Urgencia.MEDIA, new BigDecimal("45.00")),
                        new PecaProvavel("Filtro de ar condicionado", "FIL003", Urgencia.MEDIA, new BigDecimal("55.00"))
                ),
                "2-3 horas",
                new FaixaCusto(new BigDecimal("400.00"), new BigDecimal("800.00"), "BRL"),
                new MetadadosDiagnostico(Origem.TEMPLATE, null, 0, BigDecimal.ZERO, 0L)
        );
    }

    private static DiagnosticoIAResponse criarDiagnosticoFreioBarulho() {
        return new DiagnosticoIAResponse(
                "Problema no sistema de freios com ruído anormal. Requer inspeção imediata por questões de segurança.",
                List.of(
                        new CausaPossivel("Pastilhas de freio gastas", 85, Gravidade.ALTA),
                        new CausaPossivel("Discos de freio desgastados ou empenados", 60, Gravidade.ALTA),
                        new CausaPossivel("Falta de lubrificação nas guias de pinça", 40, Gravidade.MEDIA),
                        new CausaPossivel("Chapa anti-ruído danificada", 30, Gravidade.BAIXA)
                ),
                List.of(
                        "Remover rodas para inspeção visual",
                        "Medir espessura das pastilhas de freio",
                        "Verificar espessura e empenamento dos discos",
                        "Inspecionar pinças de freio e guias",
                        "Verificar fluido de freio",
                        "Substituir componentes desgastados",
                        "Testar sistema após reparo"
                ),
                List.of(
                        new PecaProvavel("Jogo de pastilhas dianteiras", "PAS001", Urgencia.ALTA, new BigDecimal("180.00")),
                        new PecaProvavel("Par de discos de freio dianteiro", "DIS001", Urgencia.MEDIA, new BigDecimal("350.00")),
                        new PecaProvavel("Fluido de freio DOT4", "FLU001", Urgencia.BAIXA, new BigDecimal("35.00"))
                ),
                "1-2 horas",
                new FaixaCusto(new BigDecimal("200.00"), new BigDecimal("700.00"), "BRL"),
                new MetadadosDiagnostico(Origem.TEMPLATE, null, 0, BigDecimal.ZERO, 0L)
        );
    }

    private static DiagnosticoIAResponse criarDiagnosticoBateria() {
        return new DiagnosticoIAResponse(
                "Problema no sistema de partida/elétrico. Bateria possivelmente fraca ou sistema de carga com defeito.",
                List.of(
                        new CausaPossivel("Bateria descarregada ou no fim da vida útil", 75, Gravidade.ALTA),
                        new CausaPossivel("Alternador com defeito (não carrega)", 40, Gravidade.ALTA),
                        new CausaPossivel("Motor de arranque com defeito", 30, Gravidade.ALTA),
                        new CausaPossivel("Terminais de bateria oxidados", 25, Gravidade.MEDIA)
                ),
                List.of(
                        "Testar carga da bateria",
                        "Verificar tensão de carga do alternador",
                        "Inspecionar terminais e cabos de bateria",
                        "Testar motor de arranque se necessário",
                        "Limpar terminais ou substituir bateria"
                ),
                List.of(
                        new PecaProvavel("Bateria automotiva 60Ah", "BAT001", Urgencia.ALTA, new BigDecimal("450.00")),
                        new PecaProvavel("Terminais de bateria", "TER001", Urgencia.BAIXA, new BigDecimal("25.00"))
                ),
                "30 minutos - 2 horas",
                new FaixaCusto(new BigDecimal("100.00"), new BigDecimal("600.00"), "BRL"),
                new MetadadosDiagnostico(Origem.TEMPLATE, null, 0, BigDecimal.ZERO, 0L)
        );
    }

    private static DiagnosticoIAResponse criarDiagnosticoPneu() {
        return new DiagnosticoIAResponse(
                "Serviço relacionado a pneus e geometria. Importante para segurança e economia de combustível.",
                List.of(
                        new CausaPossivel("Pneus desgastados além do limite", 70, Gravidade.ALTA),
                        new CausaPossivel("Desalinhamento da direção", 60, Gravidade.MEDIA),
                        new CausaPossivel("Desbalanceamento das rodas", 50, Gravidade.MEDIA),
                        new CausaPossivel("Calibragem incorreta", 40, Gravidade.BAIXA)
                ),
                List.of(
                        "Verificar profundidade do sulco dos pneus",
                        "Realizar alinhamento de direção",
                        "Fazer balanceamento das rodas",
                        "Calibrar todos os pneus conforme especificação",
                        "Inspecionar rodas quanto a deformações",
                        "Verificar estado do estepe"
                ),
                List.of(
                        new PecaProvavel("Pneu novo (unidade)", "PNE001", Urgencia.ALTA, new BigDecimal("400.00"))
                ),
                "1-2 horas",
                new FaixaCusto(new BigDecimal("100.00"), new BigDecimal("2000.00"), "BRL"),
                new MetadadosDiagnostico(Origem.TEMPLATE, null, 0, BigDecimal.ZERO, 0L)
        );
    }

    private static DiagnosticoIAResponse criarDiagnosticoArCondicionado() {
        return new DiagnosticoIAResponse(
                "Problema no sistema de ar condicionado. Pode ser falta de gás ou defeito em componentes.",
                List.of(
                        new CausaPossivel("Gás refrigerante insuficiente (vazamento)", 70, Gravidade.MEDIA),
                        new CausaPossivel("Compressor do A/C com defeito", 40, Gravidade.ALTA),
                        new CausaPossivel("Filtro de cabine entupido", 35, Gravidade.BAIXA),
                        new CausaPossivel("Condensador sujo ou bloqueado", 30, Gravidade.MEDIA)
                ),
                List.of(
                        "Verificar pressão do sistema de A/C",
                        "Inspecionar vazamentos com detector",
                        "Substituir filtro de cabine",
                        "Limpar condensador",
                        "Fazer recarga de gás se necessário",
                        "Testar funcionamento do compressor"
                ),
                List.of(
                        new PecaProvavel("Gás refrigerante R134a", "GAS001", Urgencia.ALTA, new BigDecimal("150.00")),
                        new PecaProvavel("Filtro de ar condicionado", "FIL003", Urgencia.MEDIA, new BigDecimal("55.00")),
                        new PecaProvavel("Óleo para compressor", "OLE002", Urgencia.BAIXA, new BigDecimal("40.00"))
                ),
                "1-3 horas",
                new FaixaCusto(new BigDecimal("150.00"), new BigDecimal("800.00"), "BRL"),
                new MetadadosDiagnostico(Origem.TEMPLATE, null, 0, BigDecimal.ZERO, 0L)
        );
    }

    private static DiagnosticoIAResponse criarDiagnosticoSuspensao() {
        return new DiagnosticoIAResponse(
                "Problema no sistema de suspensão. Afeta conforto, estabilidade e segurança do veículo.",
                List.of(
                        new CausaPossivel("Amortecedores gastos ou vazando", 65, Gravidade.ALTA),
                        new CausaPossivel("Buchas de suspensão desgastadas", 55, Gravidade.MEDIA),
                        new CausaPossivel("Pivôs ou terminais de direção folados", 45, Gravidade.ALTA),
                        new CausaPossivel("Molas quebradas ou cedidas", 30, Gravidade.ALTA)
                ),
                List.of(
                        "Realizar inspeção visual na suspensão",
                        "Verificar vazamentos nos amortecedores",
                        "Testar folgas em pivôs e terminais",
                        "Inspecionar buchas e coxins",
                        "Verificar estado das molas",
                        "Substituir componentes defeituosos",
                        "Realizar alinhamento após serviço"
                ),
                List.of(
                        new PecaProvavel("Par de amortecedores dianteiros", "AMO001", Urgencia.ALTA, new BigDecimal("500.00")),
                        new PecaProvavel("Kit de buchas dianteiras", "BUC001", Urgencia.MEDIA, new BigDecimal("150.00")),
                        new PecaProvavel("Pivô de suspensão", "PIV001", Urgencia.MEDIA, new BigDecimal("120.00"))
                ),
                "2-4 horas",
                new FaixaCusto(new BigDecimal("400.00"), new BigDecimal("1500.00"), "BRL"),
                new MetadadosDiagnostico(Origem.TEMPLATE, null, 0, BigDecimal.ZERO, 0L)
        );
    }

    private static DiagnosticoIAResponse criarDiagnosticoEmbreagem() {
        return new DiagnosticoIAResponse(
                "Problema no sistema de embreagem. Componentes com desgaste ou necessidade de regulagem.",
                List.of(
                        new CausaPossivel("Disco de embreagem gasto", 70, Gravidade.ALTA),
                        new CausaPossivel("Platô da embreagem com defeito", 50, Gravidade.ALTA),
                        new CausaPossivel("Cabo ou cilindro de embreagem com defeito", 40, Gravidade.MEDIA),
                        new CausaPossivel("Rolamento de embreagem com ruído", 35, Gravidade.MEDIA)
                ),
                List.of(
                        "Verificar curso e folga do pedal",
                        "Testar ponto de engate da embreagem",
                        "Inspecionar cilindro mestre e auxiliar (se hidráulico)",
                        "Verificar cabo de embreagem (se mecânico)",
                        "Substituir kit de embreagem se necessário"
                ),
                List.of(
                        new PecaProvavel("Kit de embreagem completo", "EMB001", Urgencia.ALTA, new BigDecimal("800.00")),
                        new PecaProvavel("Cilindro auxiliar de embreagem", "CIL001", Urgencia.MEDIA, new BigDecimal("180.00")),
                        new PecaProvavel("Fluido de embreagem", "FLU002", Urgencia.BAIXA, new BigDecimal("30.00"))
                ),
                "3-6 horas",
                new FaixaCusto(new BigDecimal("800.00"), new BigDecimal("2000.00"), "BRL"),
                new MetadadosDiagnostico(Origem.TEMPLATE, null, 0, BigDecimal.ZERO, 0L)
        );
    }

    private static DiagnosticoIAResponse criarDiagnosticoCorreiaDentada() {
        return new DiagnosticoIAResponse(
                "Serviço de correia dentada. Manutenção preventiva crítica - falha pode causar danos graves ao motor.",
                List.of(
                        new CausaPossivel("Correia dentada no limite de quilometragem/tempo", 90, Gravidade.CRITICA),
                        new CausaPossivel("Tensor ou rolamentos auxiliares desgastados", 60, Gravidade.ALTA),
                        new CausaPossivel("Bomba d'água com vazamento ou ruído", 45, Gravidade.ALTA)
                ),
                List.of(
                        "Remover proteções e componentes de acesso",
                        "Verificar estado da correia atual",
                        "Substituir correia dentada",
                        "Trocar tensor e rolamentos auxiliares",
                        "Substituir bomba d'água (recomendado)",
                        "Verificar e ajustar ponto do motor",
                        "Testar funcionamento"
                ),
                List.of(
                        new PecaProvavel("Kit correia dentada com tensor", "COR001", Urgencia.ALTA, new BigDecimal("450.00")),
                        new PecaProvavel("Bomba d'água", "BOM001", Urgencia.ALTA, new BigDecimal("250.00")),
                        new PecaProvavel("Líquido de arrefecimento", "LIQ001", Urgencia.MEDIA, new BigDecimal("60.00"))
                ),
                "4-6 horas",
                new FaixaCusto(new BigDecimal("800.00"), new BigDecimal("1800.00"), "BRL"),
                new MetadadosDiagnostico(Origem.TEMPLATE, null, 0, BigDecimal.ZERO, 0L)
        );
    }

    private static DiagnosticoIAResponse criarDiagnosticoVelaIgnicao() {
        return new DiagnosticoIAResponse(
                "Problema no sistema de ignição. Motor com falhas ou funcionamento irregular.",
                List.of(
                        new CausaPossivel("Velas de ignição gastas ou sujas", 75, Gravidade.MEDIA),
                        new CausaPossivel("Cabos de vela com defeito", 50, Gravidade.MEDIA),
                        new CausaPossivel("Bobina de ignição com defeito", 40, Gravidade.ALTA),
                        new CausaPossivel("Bicos injetores sujos", 35, Gravidade.MEDIA)
                ),
                List.of(
                        "Verificar e substituir velas de ignição",
                        "Inspecionar cabos de vela",
                        "Testar bobinas de ignição",
                        "Verificar códigos de erro na central",
                        "Limpar bicos injetores se necessário",
                        "Testar compressão dos cilindros"
                ),
                List.of(
                        new PecaProvavel("Jogo de velas de ignição", "VEL001", Urgencia.ALTA, new BigDecimal("120.00")),
                        new PecaProvavel("Jogo de cabos de vela", "CAB001", Urgencia.MEDIA, new BigDecimal("150.00")),
                        new PecaProvavel("Bobina de ignição", "BOB001", Urgencia.MEDIA, new BigDecimal("280.00"))
                ),
                "1-2 horas",
                new FaixaCusto(new BigDecimal("200.00"), new BigDecimal("600.00"), "BRL"),
                new MetadadosDiagnostico(Origem.TEMPLATE, null, 0, BigDecimal.ZERO, 0L)
        );
    }

    /**
     * Estrutura interna para mapear padrões a templates.
     */
    private record TemplateProblema(
            List<String> padroes,
            DiagnosticoIAResponse diagnostico
    ) {}
}
