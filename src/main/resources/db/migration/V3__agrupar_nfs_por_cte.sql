-- ADR 0001: a carga passa a ser o CT-e e ganha de 1 a N notas fiscais; o canhoto passa a ser por NF.

CREATE TABLE nota_fiscal (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    carga_id            BIGINT        NOT NULL,
    emitente_cnpj       CHAR(14)      NOT NULL,
    serie_nota_fiscal   SMALLINT      NOT NULL,
    numero_nota_fiscal  INTEGER       NOT NULL,
    peso_kg             NUMERIC(10,3) NOT NULL,
    criado_em           TIMESTAMPTZ   NOT NULL DEFAULT now(),
    atualizado_em       TIMESTAMPTZ   NOT NULL DEFAULT now(),

    CONSTRAINT fk_nota_fiscal_carga FOREIGN KEY (carga_id) REFERENCES carga (id),
    CONSTRAINT uk_nota_fiscal_carga_nota UNIQUE (carga_id, emitente_cnpj, serie_nota_fiscal, numero_nota_fiscal),
    CONSTRAINT ck_nota_fiscal_emitente_cnpj CHECK (emitente_cnpj ~ '^[0-9]{14}$'),
    CONSTRAINT ck_nota_fiscal_serie CHECK (serie_nota_fiscal BETWEEN 0 AND 999),
    CONSTRAINT ck_nota_fiscal_numero CHECK (numero_nota_fiscal BETWEEN 1 AND 999999999),
    CONSTRAINT ck_nota_fiscal_peso_kg CHECK (peso_kg > 0)
);

CREATE INDEX ix_nota_fiscal_carga_id ON nota_fiscal (carga_id);
CREATE INDEX ix_nota_fiscal_numero ON nota_fiscal (numero_nota_fiscal);

-- Cada carga atual vira um CT-e com a sua única NF.
INSERT INTO nota_fiscal (carga_id, emitente_cnpj, serie_nota_fiscal, numero_nota_fiscal, peso_kg,
                         criado_em, atualizado_em)
SELECT id, emitente_cnpj, serie_nota_fiscal, numero_nota_fiscal, peso_kg, criado_em, atualizado_em
FROM carga;

-- O canhoto passa a apontar para a NF (1:1), não mais para a carga.
ALTER TABLE canhoto ADD COLUMN nota_fiscal_id BIGINT;

UPDATE canhoto
SET nota_fiscal_id = nf.id
FROM nota_fiscal nf
WHERE nf.carga_id = canhoto.carga_id;

ALTER TABLE canhoto ALTER COLUMN nota_fiscal_id SET NOT NULL;
ALTER TABLE canhoto ADD CONSTRAINT fk_canhoto_nota_fiscal FOREIGN KEY (nota_fiscal_id) REFERENCES nota_fiscal (id);
ALTER TABLE canhoto ADD CONSTRAINT uk_canhoto_nota_fiscal UNIQUE (nota_fiscal_id);
ALTER TABLE canhoto DROP COLUMN carga_id;

-- A carga ganha a chave de acesso do CT-e (44 dígitos). As cargas atuais não têm CT-e informado:
-- recebem uma chave provisória (prefixo 99 + id) marcada em chave_provisoria, para ser corrigida depois.
ALTER TABLE carga ADD COLUMN chave_acesso CHAR(44);
ALTER TABLE carga ADD COLUMN chave_provisoria BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE carga
SET chave_acesso = '99' || lpad(id::text, 42, '0'),
    chave_provisoria = TRUE;

ALTER TABLE carga ALTER COLUMN chave_acesso SET NOT NULL;
ALTER TABLE carga ADD CONSTRAINT uk_carga_chave_acesso UNIQUE (chave_acesso);
ALTER TABLE carga ADD CONSTRAINT ck_carga_chave_acesso CHECK (chave_acesso ~ '^[0-9]{44}$');

-- Os dados da NF saíram da carga; a unicidade global da NF deixa de existir.
ALTER TABLE carga DROP COLUMN emitente_cnpj;
ALTER TABLE carga DROP COLUMN serie_nota_fiscal;
ALTER TABLE carga DROP COLUMN numero_nota_fiscal;
ALTER TABLE carga DROP COLUMN peso_kg;
