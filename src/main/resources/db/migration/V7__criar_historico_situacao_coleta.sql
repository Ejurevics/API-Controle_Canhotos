-- Histórico de situação da coleta, com quem fez cada mudança (como historico_status, da carga).

CREATE TABLE historico_situacao_coleta (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    coleta_id           BIGINT       NOT NULL,
    situacao_anterior   VARCHAR(20),
    situacao_nova       VARCHAR(20)  NOT NULL,
    alterado_em         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    alterado_por        VARCHAR(50),

    CONSTRAINT fk_historico_situacao_coleta_coleta FOREIGN KEY (coleta_id) REFERENCES coleta (id),
    CONSTRAINT fk_historico_situacao_coleta_usuario FOREIGN KEY (alterado_por) REFERENCES usuario (login),
    CONSTRAINT ck_historico_situacao_coleta_anterior CHECK (situacao_anterior IN ('PROGRAMADA', 'EM_ANDAMENTO', 'CONCLUIDA', 'CANCELADA')),
    CONSTRAINT ck_historico_situacao_coleta_nova CHECK (situacao_nova IN ('PROGRAMADA', 'EM_ANDAMENTO', 'CONCLUIDA', 'CANCELADA'))
);

CREATE INDEX ix_historico_situacao_coleta_coleta_alterado_em ON historico_situacao_coleta (coleta_id, alterado_em);

-- Coletas existentes: as mudanças anteriores não foram guardadas, então cada uma ganha um registro com a
-- situação atual, sem situação anterior e sem usuário, na data da última alteração.
INSERT INTO historico_situacao_coleta (coleta_id, situacao_nova, alterado_em)
SELECT id, situacao, atualizado_em
FROM coleta;
