-- Migration: Recalcular valores das OS existentes
-- Description: Atualiza valorPecas, valorTotal e valorFinal para OS existentes
--              que foram criadas com valores incorretos (apenas mão de obra)

-- Primeiro, vamos atualizar o valorTotal de cada item_os
UPDATE item_os
SET valor_total = (quantidade * valor_unitario) - COALESCE(desconto, 0)
WHERE valor_total = 0 OR valor_total IS NULL;

-- Agora, recalcular os valores de todas as OS
-- valorPecas = soma dos itens do tipo PECA
-- valorTotal = valorMaoObra + valorPecas + valorServicos
-- valorFinal = valorTotal - descontos

WITH valores_calculados AS (
    SELECT
        os.id,
        os.valor_mao_obra,
        COALESCE(os.desconto_percentual, 0) as desconto_percentual,
        COALESCE(os.desconto_valor, 0) as desconto_valor,
        -- Soma das peças
        COALESCE(
            (SELECT SUM(i.valor_total)
             FROM item_os i
             WHERE i.ordem_servico_id = os.id
             AND i.tipo = 'PECA'),
            0
        ) as valor_pecas_calc,
        -- Soma dos serviços
        COALESCE(
            (SELECT SUM(i.valor_total)
             FROM item_os i
             WHERE i.ordem_servico_id = os.id
             AND i.tipo = 'SERVICO'),
            0
        ) as valor_servicos_calc
    FROM ordem_servico os
),
valores_finais AS (
    SELECT
        id,
        valor_pecas_calc as novo_valor_pecas,
        (valor_mao_obra + valor_pecas_calc + valor_servicos_calc) as novo_valor_total,
        -- Calcula desconto total
        CASE
            WHEN desconto_percentual > 0 THEN
                ((valor_mao_obra + valor_pecas_calc + valor_servicos_calc) * desconto_percentual / 100) + desconto_valor
            ELSE
                desconto_valor
        END as desconto_total_calc,
        (valor_mao_obra + valor_pecas_calc + valor_servicos_calc) as base_valor
    FROM valores_calculados
)
UPDATE ordem_servico os
SET
    valor_pecas = vf.novo_valor_pecas,
    valor_total = vf.novo_valor_total,
    valor_final = GREATEST(vf.base_valor - vf.desconto_total_calc, 0),
    updated_at = NOW()
FROM valores_finais vf
WHERE os.id = vf.id;

-- Log para confirmar atualização
DO $$
DECLARE
    updated_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO updated_count FROM ordem_servico;
    RAISE NOTICE 'Valores recalculados para % ordens de serviço', updated_count;
END $$;
