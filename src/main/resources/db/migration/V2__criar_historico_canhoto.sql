CREATE TABLE historico_canhoto (
    id                      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    canhoto_id              BIGINT       NOT NULL,
    status_anterior         VARCHAR(20),
    status_novo             VARCHAR(20)  NOT NULL,
    data_recebimento        DATE,
    observacao              VARCHAR(500),
    data_futura_confirmada  BOOLEAN      NOT NULL DEFAULT FALSE,
    registrado_em           TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT fk_historico_canhoto_canhoto FOREIGN KEY (canhoto_id) REFERENCES canhoto (id),
    CONSTRAINT ck_historico_canhoto_status_anterior CHECK (status_anterior IN ('PENDENTE', 'RECEBIDO', 'DIVERGENTE')),
    CONSTRAINT ck_historico_canhoto_status_novo CHECK (status_novo IN ('PENDENTE', 'RECEBIDO', 'DIVERGENTE'))
);

CREATE INDEX ix_historico_canhoto_canhoto_registrado_em ON historico_canhoto (canhoto_id, registrado_em);

INSERT INTO historico_canhoto (canhoto_id, status_novo, data_recebimento, observacao, registrado_em)
SELECT id, status, data_recebimento, observacao, criado_em
FROM canhoto;
