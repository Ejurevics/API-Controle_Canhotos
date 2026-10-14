-- ADR 0002: usuários da API, com senha em BCrypt e perfil ADMIN ou OPERADOR.

CREATE TABLE usuario (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    login          VARCHAR(50)  NOT NULL,
    nome           VARCHAR(150) NOT NULL,
    senha_hash     VARCHAR(100) NOT NULL,
    perfil         VARCHAR(20)  NOT NULL,
    ativo          BOOLEAN      NOT NULL DEFAULT TRUE,
    criado_em      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    atualizado_em  TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT uk_usuario_login UNIQUE (login),
    CONSTRAINT ck_usuario_login CHECK (login ~ '^[a-z0-9._-]{3,50}$'),
    CONSTRAINT ck_usuario_perfil CHECK (perfil IN ('ADMIN', 'OPERADOR'))
);
