# ADR 0003: Limite de tentativas de login por login e por IP

- **Status:** aceita
- **Data:** 2026-10-14

## Contexto

O [ADR 0002](0002-autenticacao-jwt.md) aceitou, como risco, não limitar as tentativas de login. Sem limite, quem alcança a API pode testar senhas sem parar, numa conta só (força bruta) ou com uma senha comum em muitas contas (*password spraying*). O BCrypt deixa cada tentativa lenta, mas não impede o ataque.

## Decisão

1. **Dois contadores de falhas, em janelas fixas** (padrão: 15 minutos a partir da primeira falha):
   - **por login:** 5 falhas bloqueiam aquele login, venha a tentativa de qualquer IP;
   - **por IP:** 20 falhas bloqueiam o IP, em qualquer login.
2. **Bloqueado, responde `429 Too Many Requests`** em `ProblemDetail`, com `Retry-After` em segundos até a janela acabar. A senha nem é conferida: nem a senha certa entra durante o bloqueio.
3. **Login inexistente, usuário inativo e senha errada contam igual**, como já respondem igual. Assim o bloqueio não revela quais logins existem.
4. **Login certo zera só o contador do login.** O do IP continua; senão, quem tem uma conta válida poderia entrar nela entre as tentativas para zerar o IP e seguir testando senhas em outras contas.
5. **Contadores em memória**, sem dependência nova. As janelas vencidas são descartadas quando o mapa passa de 10 mil entradas.
6. **Limites configuráveis** por variável de ambiente: `LOGIN_MAX_FALHAS_POR_LOGIN`, `LOGIN_MAX_FALHAS_POR_IP` e `LOGIN_JANELA`.
7. **IP = endereço da conexão** (`remoteAddr`). O cabeçalho `X-Forwarded-For` não é usado, porque qualquer cliente pode forjá-lo.

## Consequências

- **Positivas:** força bruta numa conta fica limitada a 5 tentativas a cada 15 minutos; *spraying* de um mesmo IP, a 20.
- **Negativas e riscos aceitos:**
  - **Bloqueio proposital:** quem sabe um login consegue bloqueá-lo por 15 minutos errando a senha de propósito. Aceitável: o bloqueio é curto e acaba sozinho.
  - **Atrás de proxy ou NAT, vários clientes podem ter o mesmo IP.** Com o `docker compose` no Docker Desktop, por exemplo, as conexões podem chegar pelo gateway da rede do Docker, e o limite por IP passa a valer para todos juntos. Atrás de um proxy confiável, configurar `server.forward-headers-strategy=native` para o Spring usar o IP real; ou aumentar `LOGIN_MAX_FALHAS_POR_IP`.
  - **Contadores zeram ao reiniciar e não são compartilhados entre instâncias.** Basta para uma instância; com várias, o contador iria para um armazenamento comum (Redis, banco).
  - Um ataque distribuído (muitos IPs) contra várias contas ao mesmo tempo continua limitado só pelo contador por login.

## Alternativas descartadas

- **Só por IP:** não protege uma conta atacada de vários IPs.
- **Só por login:** não pega o *spraying* (uma senha em muitas contas).
- **Contador no banco (colunas no usuário):** sobrevive a reinícios, mas só existe para logins cadastrados, e tratar o inexistente de outro jeito revelaria quais existem; além de uma escrita no banco a cada falha.
- **Biblioteca de *rate limiting* (Bucket4j, Resilience4j):** resolve, mas acrescenta dependência para um contador simples.
- **Atraso crescente em vez de bloqueio:** prende uma thread do servidor a cada tentativa, o que facilita negação de serviço.
- **Bloqueio permanente até um ADMIN desbloquear:** tornaria o bloqueio proposital muito mais danoso.
