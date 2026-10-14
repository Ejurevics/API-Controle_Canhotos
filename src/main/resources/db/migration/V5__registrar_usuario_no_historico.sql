-- Quem fez cada mudança: login do usuário autenticado (ADR 0002). Fica nulo nos registros anteriores à
-- autenticação, que não têm como ser atribuídos a ninguém. O login nunca muda e usuários não são apagados,
-- então a FK pelo login é estável.

ALTER TABLE historico_status ADD COLUMN alterado_por VARCHAR(50);
ALTER TABLE historico_status
    ADD CONSTRAINT fk_historico_status_usuario FOREIGN KEY (alterado_por) REFERENCES usuario (login);

ALTER TABLE historico_canhoto ADD COLUMN registrado_por VARCHAR(50);
ALTER TABLE historico_canhoto
    ADD CONSTRAINT fk_historico_canhoto_usuario FOREIGN KEY (registrado_por) REFERENCES usuario (login);
