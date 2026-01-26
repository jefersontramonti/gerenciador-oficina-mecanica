--liquibase formatted sql

--changeset pitstop:085-update-planos-limites
--comment: Atualiza limites de usuários e OS/mês conforme tabela de planos

-- Econômico: 3 usuários, 100 OS/mês, R$ 160
UPDATE planos SET
    limite_usuarios = 3,
    limite_os_mes = 100,
    valor_mensal = 160.00
WHERE codigo = 'ECONOMICO';

-- Profissional: 10 usuários, 500 OS/mês, R$ 260
UPDATE planos SET
    limite_usuarios = 10,
    limite_os_mes = 500,
    valor_mensal = 260.00
WHERE codigo = 'PROFISSIONAL';

-- Turbinado: ilimitado (-1), ilimitado (-1), R$ 600
UPDATE planos SET
    limite_usuarios = -1,
    limite_os_mes = -1,
    valor_mensal = 600.00
WHERE codigo = 'TURBINADO';

--rollback UPDATE planos SET limite_usuarios = 1, limite_os_mes = -1 WHERE codigo = 'ECONOMICO';
--rollback UPDATE planos SET limite_usuarios = 3, limite_os_mes = -1 WHERE codigo = 'PROFISSIONAL';
--rollback UPDATE planos SET limite_usuarios = -1, limite_os_mes = -1 WHERE codigo = 'TURBINADO';
