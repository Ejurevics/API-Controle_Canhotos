# ADR 0005: Cache curto da credencial na checagem do token

- **Status:** aceita
- **Data:** 2026-10-21

## Contexto

O [ADR 0004](0004-revogar-tokens-por-versao-de-credencial.md) passou a conferir, a cada requisição autenticada, se o usuário existe, está ativo e se a versão da credencial no token é a atual. Isso custa uma consulta ao banco por requisição, uma consequência que o próprio ADR deixou anotada ("se pesar, cabe um cache curto"). Não há medição mostrando que a consulta pese hoje; o cache entra para tirar esse custo do caminho de toda requisição, mantendo a revogação imediata.

## Decisão

1. **`CacheCredenciais` guarda, por login, só o que a checagem precisa**: se o usuário está ativo e a versão da credencial. Não guarda senha nem dados pessoais.
2. **Prazo curto e configurável:** `JWT_CACHE_CREDENCIAL`, padrão `PT30S`. `PT0S` desliga o cache (volta a consultar a cada requisição).
3. **Invalidação explícita:** desativar, reativar, trocar a senha (própria ou pelo ADMIN) e trocar o perfil descartam a entrada do usuário. Assim a revogação continua imediata nesta instância. Reativar também invalida, senão um "inativo" guardado recusaria o token novo até vencer.
4. **Descarta agora e de novo depois do commit:** uma requisição que chegue entre a mudança e o commit ainda lê o valor antigo do banco e o guardaria de volta; a segunda descarta resolve isso.
5. **Usuário inexistente não é guardado.**
6. **Em memória, sem dependência nova**, como o limite de login ([ADR 0003](0003-limite-tentativas-login.md)). Entradas vencidas são descartadas quando o mapa passa de 10 mil entradas.

## Consequências

- **Positivas:** uma requisição autenticada só vai ao banco para a credencial uma vez a cada 30 s por usuário; a revogação pela API continua imediata.
- **Negativas e riscos aceitos:**
  - **Mudanças feitas por fora da API** (alteração direta no banco) levam até o prazo do cache para valer.
  - **Com várias instâncias**, a invalidação só acontece na instância que atendeu a mudança; nas outras, a revogação pode levar até o prazo do cache. Hoje há uma instância só. Com várias, o caminho seria um cache compartilhado (Redis) ou um aviso entre instâncias, ou desligar o cache.
  - Mais uma peça para lembrar: toda operação nova que mude ativo, senha ou perfil precisa chamar `invalidar`.

## Alternativas descartadas

- **Spring Cache com Caffeine:** dá prazo e limite de tamanho prontos, mas acrescenta dependência para um mapa com prazo.
- **Cache sem invalidação (só o prazo):** mais simples, mas desativar ou trocar a senha passaria a levar até 30 s para derrubar o token, o que desfaz o ganho do ADR 0004.
- **Guardar a entidade `Usuario` inteira:** carrega dados que a checagem não usa (hash da senha, histórico) e pode ser lida fora de uma sessão do Hibernate.
- **Não fazer nada:** a consulta é pela chave única do login e é barata; fica registrada como alternativa válida (`JWT_CACHE_CREDENCIAL=PT0S`).
