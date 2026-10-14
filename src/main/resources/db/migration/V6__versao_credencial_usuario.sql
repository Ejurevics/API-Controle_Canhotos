-- ADR 0004: o token leva a versão da credencial do usuário; desativar o usuário ou trocar a senha incrementa
-- a versão, e os tokens emitidos antes deixam de valer.

ALTER TABLE usuario ADD COLUMN versao_credencial INTEGER NOT NULL DEFAULT 0;
