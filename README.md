# API Controle de Canhotos

API REST para controle de coletas, cargas e devolução de canhotos (comprovantes de entrega assinados) em uma transportadora, com histórico de status por carga.

> Projeto em construção.

## Modelo de domínio

```mermaid
erDiagram
    COLETA ||--|{ CARGA : agrupa
    CARGA ||--|| CANHOTO : possui
    CARGA ||--|{ HISTORICO_STATUS : registra

    COLETA {
        bigint id PK
        varchar cliente_nome
        char cliente_cnpj
        date data_programada
        varchar situacao "PROGRAMADA | EM_ANDAMENTO | CONCLUIDA | CANCELADA"
        timestamptz criado_em
        timestamptz atualizado_em
    }

    CARGA {
        bigint id PK
        bigint coleta_id FK
        char chave_nfe UK "chave de acesso da NF-e (44 dígitos)"
        integer numero_nota_fiscal
        numeric peso_kg
        varchar cidade_destino
        char uf_destino
        varchar status "AGUARDANDO_COLETA | COLETADA | EM_TRANSITO | ENTREGUE | OCORRENCIA | DEVOLVIDA"
        timestamptz criado_em
        timestamptz atualizado_em
    }

    CANHOTO {
        bigint id PK
        bigint carga_id FK,UK
        varchar status "PENDENTE | RECEBIDO | DIVERGENTE"
        date data_recebimento
        varchar observacao
        timestamptz criado_em
        timestamptz atualizado_em
    }

    HISTORICO_STATUS {
        bigint id PK
        bigint carga_id FK
        varchar status_anterior
        varchar status_novo
        timestamptz alterado_em
        varchar motivo
    }
```

- Cada carga corresponde a **uma NF-e**, identificada pela chave de acesso.
- O canhoto nasce `PENDENTE` junto com a carga. `RECEBIDO` e `DIVERGENTE` exigem data de recebimento; `DIVERGENTE` exige observação.
- Toda mudança de status da carga gera um registro em `historico_status`.
- Evolução prevista: agrupar NFs por CT-e (uma carga com várias NFs).

## Requisitos

- Java 21+
- Docker Desktop (para subir a aplicação e rodar os testes de integração)

## Instalação

```bash
git clone https://github.com/Ejurevics/API-Controle_Canhotos.git
cd API-Controle_Canhotos
```

## Uso

_Em breve._
