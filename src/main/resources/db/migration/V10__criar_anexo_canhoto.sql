-- Comprovante do canhoto (ADR 0006): dados do anexo numa tabela e bytes em outra, para listar sem ler os arquivos.
-- As colunas de remoção já entram aqui; a remoção lógica usa removido_em, removido_por e motivo_remocao.

CREATE TABLE anexo_canhoto (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    canhoto_id      BIGINT        NOT NULL,
    nome_arquivo    VARCHAR(255)  NOT NULL,
    tipo            VARCHAR(10)   NOT NULL,
    tamanho_bytes   INTEGER       NOT NULL,
    sha256          CHAR(64)      NOT NULL,
    enviado_em      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    enviado_por     VARCHAR(50)   NOT NULL,
    removido_em     TIMESTAMPTZ,
    removido_por    VARCHAR(50),
    motivo_remocao  VARCHAR(500),

    CONSTRAINT fk_anexo_canhoto_canhoto FOREIGN KEY (canhoto_id) REFERENCES canhoto (id),
    CONSTRAINT fk_anexo_canhoto_enviado_por FOREIGN KEY (enviado_por) REFERENCES usuario (login),
    CONSTRAINT fk_anexo_canhoto_removido_por FOREIGN KEY (removido_por) REFERENCES usuario (login),
    CONSTRAINT ck_anexo_canhoto_tipo CHECK (tipo IN ('JPEG', 'PNG', 'PDF')),
    CONSTRAINT ck_anexo_canhoto_tamanho CHECK (tamanho_bytes > 0),
    CONSTRAINT ck_anexo_canhoto_remocao CHECK (
        (removido_em IS NULL AND removido_por IS NULL AND motivo_remocao IS NULL)
        OR (removido_em IS NOT NULL AND removido_por IS NOT NULL AND motivo_remocao IS NOT NULL))
);

CREATE INDEX ix_anexo_canhoto_canhoto ON anexo_canhoto (canhoto_id);

-- O mesmo arquivo não entra duas vezes entre os anexos ativos de um canhoto (também conferido no domínio).
CREATE UNIQUE INDEX ux_anexo_canhoto_ativo_sha256 ON anexo_canhoto (canhoto_id, sha256) WHERE removido_em IS NULL;

CREATE TABLE anexo_canhoto_conteudo (
    anexo_id  BIGINT  PRIMARY KEY,
    conteudo  BYTEA   NOT NULL,

    CONSTRAINT fk_anexo_canhoto_conteudo_anexo FOREIGN KEY (anexo_id) REFERENCES anexo_canhoto (id)
);
