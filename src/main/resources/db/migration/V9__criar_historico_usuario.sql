-- Histórico da gestão de usuários: quem criou, desativou, reativou, mudou o perfil ou a senha de cada um.

CREATE TABLE historico_usuario (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    usuario_id    BIGINT       NOT NULL,
    acao          VARCHAR(20)  NOT NULL,
    detalhe       VARCHAR(100),
    alterado_em   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    alterado_por  VARCHAR(50),

    CONSTRAINT fk_historico_usuario_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id),
    CONSTRAINT fk_historico_usuario_alterado_por FOREIGN KEY (alterado_por) REFERENCES usuario (login),
    CONSTRAINT ck_historico_usuario_acao CHECK (acao IN ('CRIADO', 'DESATIVADO', 'REATIVADO', 'PERFIL_ALTERADO',
                                                         'SENHA_REDEFINIDA', 'SENHA_TROCADA'))
);

CREATE INDEX ix_historico_usuario_usuario_alterado_em ON historico_usuario (usuario_id, alterado_em);

-- Usuários existentes: as mudanças anteriores não foram guardadas, então cada um ganha só o registro de
-- criação, na data de criação e sem autor.
INSERT INTO historico_usuario (usuario_id, acao, alterado_em)
SELECT id, 'CRIADO', criado_em
FROM usuario;
