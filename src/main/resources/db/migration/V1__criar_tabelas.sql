CREATE TABLE coleta (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    cliente_nome     VARCHAR(150) NOT NULL,
    cliente_cnpj     CHAR(14)     NOT NULL,
    data_programada  DATE         NOT NULL,
    situacao         VARCHAR(20)  NOT NULL,
    criado_em        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    atualizado_em    TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT ck_coleta_cliente_cnpj CHECK (cliente_cnpj ~ '^[0-9]{14}$'),
    CONSTRAINT ck_coleta_situacao CHECK (situacao IN ('PROGRAMADA', 'EM_ANDAMENTO', 'CONCLUIDA', 'CANCELADA'))
);

CREATE INDEX ix_coleta_data_programada ON coleta (data_programada);
CREATE INDEX ix_coleta_cliente_cnpj ON coleta (cliente_cnpj);

CREATE TABLE carga (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    coleta_id           BIGINT        NOT NULL,
    emitente_cnpj       CHAR(14)      NOT NULL,
    serie_nota_fiscal   SMALLINT      NOT NULL,
    numero_nota_fiscal  INTEGER       NOT NULL,
    peso_kg             NUMERIC(10,3) NOT NULL,
    cidade_destino      VARCHAR(100)  NOT NULL,
    uf_destino          CHAR(2)       NOT NULL,
    status              VARCHAR(20)   NOT NULL,
    criado_em           TIMESTAMPTZ   NOT NULL DEFAULT now(),
    atualizado_em       TIMESTAMPTZ   NOT NULL DEFAULT now(),

    CONSTRAINT fk_carga_coleta FOREIGN KEY (coleta_id) REFERENCES coleta (id),
    CONSTRAINT uk_carga_nota_fiscal UNIQUE (emitente_cnpj, serie_nota_fiscal, numero_nota_fiscal),
    CONSTRAINT ck_carga_emitente_cnpj CHECK (emitente_cnpj ~ '^[0-9]{14}$'),
    CONSTRAINT ck_carga_serie_nota_fiscal CHECK (serie_nota_fiscal BETWEEN 0 AND 999),
    CONSTRAINT ck_carga_numero_nota_fiscal CHECK (numero_nota_fiscal BETWEEN 1 AND 999999999),
    CONSTRAINT ck_carga_peso_kg CHECK (peso_kg > 0),
    CONSTRAINT ck_carga_uf_destino CHECK (uf_destino ~ '^[A-Z]{2}$'),
    CONSTRAINT ck_carga_status CHECK (status IN ('AGUARDANDO_COLETA', 'COLETADA', 'EM_TRANSITO', 'ENTREGUE', 'OCORRENCIA', 'DEVOLVIDA'))
);

CREATE INDEX ix_carga_coleta_id ON carga (coleta_id);
CREATE INDEX ix_carga_numero_nota_fiscal ON carga (numero_nota_fiscal);
CREATE INDEX ix_carga_status ON carga (status);

CREATE TABLE canhoto (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    carga_id          BIGINT       NOT NULL,
    status            VARCHAR(20)  NOT NULL,
    data_recebimento  DATE,
    observacao        VARCHAR(500),
    criado_em         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    atualizado_em     TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT fk_canhoto_carga FOREIGN KEY (carga_id) REFERENCES carga (id),
    CONSTRAINT uk_canhoto_carga UNIQUE (carga_id),
    CONSTRAINT ck_canhoto_status CHECK (status IN ('PENDENTE', 'RECEBIDO', 'DIVERGENTE')),
    CONSTRAINT ck_canhoto_data_recebimento CHECK ((status = 'PENDENTE') = (data_recebimento IS NULL)),
    CONSTRAINT ck_canhoto_observacao_divergente CHECK (status <> 'DIVERGENTE' OR observacao IS NOT NULL)
);

CREATE INDEX ix_canhoto_status ON canhoto (status);

CREATE TABLE historico_status (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    carga_id         BIGINT       NOT NULL,
    status_anterior  VARCHAR(20),
    status_novo      VARCHAR(20)  NOT NULL,
    alterado_em      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    motivo           VARCHAR(255),

    CONSTRAINT fk_historico_status_carga FOREIGN KEY (carga_id) REFERENCES carga (id),
    CONSTRAINT ck_historico_status_anterior CHECK (status_anterior IN ('AGUARDANDO_COLETA', 'COLETADA', 'EM_TRANSITO', 'ENTREGUE', 'OCORRENCIA', 'DEVOLVIDA')),
    CONSTRAINT ck_historico_status_novo CHECK (status_novo IN ('AGUARDANDO_COLETA', 'COLETADA', 'EM_TRANSITO', 'ENTREGUE', 'OCORRENCIA', 'DEVOLVIDA'))
);

CREATE INDEX ix_historico_status_carga_alterado_em ON historico_status (carga_id, alterado_em);
