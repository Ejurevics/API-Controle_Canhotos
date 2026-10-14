# API Controle de Canhotos

API REST para controle de coletas, cargas (CT-e) e devolução de canhotos (comprovantes de entrega assinados) em uma transportadora, com histórico de status por carga.

**Versão atual: `v0.2.0`.** O fluxo principal está pronto (coletas, cargas, canhotos e históricos), com listagens filtradas e paginadas. A `develop` já agrupa várias NFs por CT-e (mudança incompatível na API, prevista para a `v0.3.0`; veja o [ADR 0001](docs/adr/0001-agrupar-nfs-por-cte.md)). Autenticação ainda não existe; veja o [Roadmap](#roadmap).

## Modelo de domínio

```mermaid
erDiagram
    COLETA ||--|{ CARGA : agrupa
    CARGA ||--|{ NOTA_FISCAL : contem
    NOTA_FISCAL ||--|| CANHOTO : possui
    CARGA ||--|{ HISTORICO_STATUS : registra
    CANHOTO ||--|{ HISTORICO_CANHOTO : registra

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
        char chave_acesso UK "CT-e, 44 dígitos"
        boolean chave_provisoria
        varchar cidade_destino
        char uf_destino
        varchar status "AGUARDANDO_COLETA | COLETADA | EM_TRANSITO | ENTREGUE | OCORRENCIA | DEVOLVIDA"
        timestamptz criado_em
        timestamptz atualizado_em
    }

    NOTA_FISCAL {
        bigint id PK
        bigint carga_id FK
        char emitente_cnpj UK "UK: carga + emitente + série + número"
        smallint serie_nota_fiscal UK
        integer numero_nota_fiscal UK
        numeric peso_kg
        timestamptz criado_em
        timestamptz atualizado_em
    }

    CANHOTO {
        bigint id PK
        bigint nota_fiscal_id FK,UK
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

    HISTORICO_CANHOTO {
        bigint id PK
        bigint canhoto_id FK
        varchar status_anterior
        varchar status_novo
        date data_recebimento
        varchar observacao
        boolean data_futura_confirmada
        timestamptz registrado_em
    }
```

- Cada carga corresponde a **um CT-e**, identificado pela chave de acesso de 44 dígitos (única no sistema), e agrupa **de 1 a N notas fiscais**. O status e o histórico de status são do CT-e; o peso total é a soma das NFs.
- A NF é identificada por CNPJ do emitente + série + número, **única só dentro do CT-e**: o mesmo número pode se repetir em CT-es diferentes, porque a numeração segue a de cada cliente.
- Se não informados, o CNPJ do emitente da NF assume o do cliente da coleta e a série assume `1`.
- Cada NF tem o seu canhoto, que nasce `PENDENTE` junto com ela. `RECEBIDO` e `DIVERGENTE` exigem data de recebimento; `DIVERGENTE` exige observação.
- O canhoto só pode ser registrado (`RECEBIDO` ou `DIVERGENTE`) com o CT-e (carga) `ENTREGUE`.
- Data de recebimento posterior à data atual (no fuso `America/Sao_Paulo`, independente do fuso do servidor) gera aviso e só é aceita com confirmação explícita, que fica marcada no histórico do canhoto.
- O canhoto pode ser registrado mais de uma vez (ex.: `DIVERGENTE` e depois `RECEBIDO`). Cada registro fica em `historico_canhoto` com status, data e observação; o canhoto guarda só o estado atual.
- Toda mudança de status da carga gera um registro em `historico_status`.
- Cargas criadas antes do agrupamento por CT-e foram migradas com uma chave de acesso **provisória** (prefixo `99` + id, `chaveProvisoria: true`), para ser corrigida depois.

### Transições de status da carga

| De | Pode ir para |
|---|---|
| `AGUARDANDO_COLETA` | `COLETADA`, `OCORRENCIA` |
| `COLETADA` | `EM_TRANSITO`, `OCORRENCIA` |
| `EM_TRANSITO` | `ENTREGUE`, `OCORRENCIA`, `DEVOLVIDA` |
| `OCORRENCIA` | `AGUARDANDO_COLETA`, `COLETADA`, `EM_TRANSITO`, `ENTREGUE`, `DEVOLVIDA` |
| `ENTREGUE` | `OCORRENCIA` (retrocesso sempre passa por ocorrência) |
| `DEVOLVIDA` | — (final) |

Registrar `OCORRENCIA` exige motivo.

### Transições de situação da coleta

| De | Pode ir para |
|---|---|
| `PROGRAMADA` | `EM_ANDAMENTO`, `CANCELADA` |
| `EM_ANDAMENTO` | `CONCLUIDA`, `CANCELADA` |
| `CONCLUIDA`, `CANCELADA` | — (finais) |

Concluir a coleta exige que todas as cargas estejam `ENTREGUE` ou `DEVOLVIDA`. Caso contrário, responde `422` listando os CT-es pendentes (chave de acesso) e seus status (ex.: `35261011111111000111570010000012341000012345 (EM_TRANSITO)`). O status do canhoto não é considerado. Cancelar não tem essa exigência.

## Endpoints

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/coletas` | Cria uma coleta com seus CT-es (cargas) e as NFs de cada um (`201` + `Location`) |
| `GET` | `/coletas` | Lista coletas com filtros e paginação |
| `GET` | `/coletas/{id}` | Busca a coleta com seus CT-es e NFs |
| `PATCH` | `/coletas/{id}/situacao` | Altera a situação da coleta (`{"situacao": "..."}`) |
| `GET` | `/cargas` | Lista cargas (CT-es) com filtros e paginação |
| `GET` | `/cargas/{id}` | Busca a carga com as NFs e o histórico de status |
| `PATCH` | `/cargas/{id}/status` | Altera o status da carga (`{"status": "...", "motivo": "..."}`); `motivo` é obrigatório para `OCORRENCIA` |
| `GET` | `/canhotos` | Lista canhotos, um por NF (ex.: pendentes por cliente ou período), com paginação |
| `GET` | `/notas-fiscais/{id}/canhoto` | Busca o canhoto da NF com o histórico de registros |
| `POST` | `/notas-fiscais/{id}/canhoto/registros` | Registra o canhoto da NF como `RECEBIDO` ou `DIVERGENTE` |

### Listagens

Todos os filtros são opcionais e combinados com `E`. Datas no formato `AAAA-MM-DD`.

| Rota | Filtros |
|---|---|
| `GET /coletas` | `situacao`, `clienteCnpj`, `dataInicio`, `dataFim` (sobre a data programada) |
| `GET /cargas` | `status`, `coletaId` |
| `GET /canhotos` | `status`, `clienteCnpj`, `dataInicio`, `dataFim` (sobre a data programada da coleta) |

Paginação: `pagina` (começa em `0`, padrão `0`) e `tamanho` (padrão `20`, máximo `100`). A ordenação é fixa: coletas pela data programada (mais recente primeiro), cargas e canhotos por `id`. A resposta traz `content` e `page` (`size`, `number`, `totalElements`, `totalPages`). Na listagem de cargas, cada item traz `quantidadeNotasFiscais` e `canhotosPendentes` em vez das NFs; use `GET /cargas/{id}` para o detalhe.

Exemplo, canhotos pendentes de um cliente em novembro:

```
GET /canhotos?status=PENDENTE&clienteCnpj=11111111000111&dataInicio=2026-11-01&dataFim=2026-11-30
```

Filtro com valor inválido (`situacao=XYZ`, data malformada) responde `400`; período invertido e `pagina`/`tamanho` fora do limite respondem `422`.

Registro do canhoto de uma NF (`observacao` obrigatória na `DIVERGENTE`; `dataFuturaConfirmada` opcional, padrão `false`):

```json
{ "status": "RECEBIDO", "dataRecebimento": "2026-10-14", "dataFuturaConfirmada": false }
```

Se a data for posterior a hoje (fuso `America/Sao_Paulo`), a primeira tentativa responde `409` com `confirmacaoNecessaria: true`. O cliente avisa o usuário e reenvia com `dataFuturaConfirmada: true`.

Exemplo de `POST /coletas` com um CT-e de duas NFs (`emitenteCnpj` e `serieNotaFiscal` são opcionais em cada NF):

```json
{
  "clienteNome": "Cliente A",
  "clienteCnpj": "11111111000111",
  "dataProgramada": "2026-10-20",
  "cargas": [
    {
      "chaveAcesso": "35261011111111000111570010000012341000012345",
      "cidadeDestino": "Campinas",
      "ufDestino": "SP",
      "notasFiscais": [
        { "numeroNotaFiscal": 1001, "pesoKg": 150.5 },
        { "numeroNotaFiscal": 1002, "pesoKg": 49.5 }
      ]
    }
  ]
}
```

Erros seguem o formato `ProblemDetail` (RFC 9457):

| Status | Quando |
|---|---|
| `400` | Dados inválidos (campo a campo em `erros`) |
| `404` | Recurso não encontrado |
| `409` | Confirmação necessária (`confirmacaoNecessaria: true`) ou violação de integridade |
| `422` | Regra de negócio violada (ex.: CT-e já cadastrado, NF repetida no mesmo CT-e) |

## Stack

Java 21 · Spring Boot 4.1 · Spring Data JPA (Hibernate 7) · PostgreSQL 18 · Flyway · springdoc-openapi · JUnit 5 + Testcontainers

## Requisitos

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) — para subir a aplicação e rodar os testes de integração
- Java 21+ — só para rodar fora do Docker ou executar os testes (o Maven vem pelo wrapper `mvnw`)

## Instalação e uso

### Tudo no Docker

```bash
git clone https://github.com/Ejurevics/API-Controle_Canhotos.git
cd API-Controle_Canhotos
docker compose up --build
```

| O quê | URL |
|---|---|
| Swagger UI | http://localhost:8080/swagger-ui.html |
| OpenAPI (JSON) | http://localhost:8080/v3/api-docs |
| Health check | http://localhost:8080/actuator/health |

As tabelas são criadas automaticamente pelo Flyway na subida. Para parar: `docker compose down` (adicione `-v` para apagar também os dados do banco).

### Desenvolvimento local

Sobe só o banco no Docker e roda a aplicação na máquina (perfil `dev`, com SQL no log):

```bash
docker compose up -d db
./mvnw spring-boot:run        # Windows: .\mvnw.cmd spring-boot:run
```

### Testes

Com o Docker rodando:

```bash
./mvnw test                   # Windows: .\mvnw.cmd test
```

Os testes de integração sobem um PostgreSQL descartável via Testcontainers.

O GitHub Actions (`.github/workflows/ci.yml`) roda `./mvnw clean verify` em todo PR e push para `develop` e `main`.

## Configuração

Todas as variáveis têm valor padrão, então o projeto sobe sem configurar nada. Para mudar, copie `.env.example` para `.env` e preencha. O `docker compose` lê o `.env` automaticamente.

| Variável | Padrão | Uso |
|---|---|---|
| `DB_NAME` | `canhotos` | Nome do banco |
| `DB_USERNAME` | `canhotos` | Usuário do banco |
| `DB_PASSWORD` | `canhotos` | Senha do banco |
| `DB_PORT` | `5432` | Porta do PostgreSQL exposta na máquina |
| `APP_PORT` | `8080` | Porta da API exposta na máquina |

Perfis Spring:

- `dev` (padrão): aplicação local apontando para `localhost:5432`, com log de SQL.
- `docker`: usado pelo compose; exige `DB_URL`, `DB_USERNAME` e `DB_PASSWORD` (sem valor padrão, falha rápido se faltar).

## Estrutura

```
src/main/java/br/com/controlecanhotos/
├── coleta/        Coleta, situação, service, controller e DTOs
├── carga/         Carga (CT-e), status, regras de transição e DTOs
├── notafiscal/    Nota fiscal (de 1 a N por CT-e) e DTOs
├── canhoto/       Canhoto (um por NF), status, service, controllers e DTOs
├── historico/     Histórico de status da carga e de registros do canhoto
└── shared/        Exceções, tratamento global de erros (ApiExceptionHandler) e paginação
src/main/resources/
├── application.yml
└── db/migration/  Migrations Flyway (V1__, V2__...)
.github/workflows/ci.yml  CI: build e testes a cada PR
Dockerfile         Build multi-stage (JDK para compilar, JRE para rodar)
docker-compose.yml App + PostgreSQL com healthcheck
```

## Fluxo de desenvolvimento

- `main`: versões estáveis, marcadas com tag (`v0.2.0`). Só recebe a `develop` via PR de release, com merge commit.
- `develop`: integração. Recebe os PRs de feature, com squash.
- Branches de trabalho nascem da `develop` (`feat/`, `fix/`, `chore/`, `docs/`, `refactor/`) e são apagadas após o merge.
- Commits seguem Conventional Commits (`feat(coleta): adiciona ...`).
- Todo PR roda o CI (`./mvnw -B clean verify`). Mudanças que alteram uso, setup ou estrutura atualizam este README no mesmo PR.

## Roadmap

- [x] Listagens com filtros e paginação (coletas por situação, cliente e período; cargas por status; canhotos pendentes).
- [x] Testes de service.
- [x] Regra para concluir uma coleta com base no status das cargas.
- [ ] Autenticação.
- [x] Agrupar NFs por CT-e (uma carga com várias NFs). Desenho em [ADR 0001](docs/adr/0001-agrupar-nfs-por-cte.md); sai na `v0.3.0`.
