-- A chave provisória só é corrigida uma vez (vira definitiva e não muda mais), então a carga guarda a
-- correção em colunas próprias: a chave provisória anterior, quando e por quem.

ALTER TABLE carga ADD COLUMN chave_provisoria_anterior CHAR(44);
ALTER TABLE carga ADD COLUMN chave_corrigida_em TIMESTAMPTZ;
ALTER TABLE carga ADD COLUMN chave_corrigida_por VARCHAR(50);

ALTER TABLE carga
    ADD CONSTRAINT fk_carga_chave_corrigida_por FOREIGN KEY (chave_corrigida_por) REFERENCES usuario (login);
ALTER TABLE carga
    ADD CONSTRAINT ck_carga_correcao_chave CHECK (
        (chave_provisoria_anterior IS NULL) = (chave_corrigida_em IS NULL)
        AND (chave_corrigida_por IS NULL OR chave_corrigida_em IS NOT NULL));
