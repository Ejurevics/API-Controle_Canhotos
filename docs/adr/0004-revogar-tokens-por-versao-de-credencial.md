# ADR 0004: Revogar tokens por versão de credencial

- **Status:** aceita
- **Data:** 2026-10-14

## Contexto

O [ADR 0002](0002-autenticacao-jwt.md) aceitou, como risco, que o token não fosse revogável: desativar um usuário ou trocar a senha impedia novos logins, mas os tokens já emitidos continuavam valendo até expirar (1 hora por padrão). Um usuário desligado ou uma senha vazada seguiam com acesso nesse intervalo.

## Decisão

1. **Versão da credencial no usuário** (`usuario.versao_credencial`, migration V6, começa em 0).
2. **O token leva a versão** no claim `ver`.
3. **A cada requisição autenticada**, depois das validações de assinatura, data e emissor, a API consulta o usuário pelo login (`sub`) e só aceita o token se o usuário existe, está ativo e a versão do token é a atual. Senão, responde `401`.
4. **Incrementam a versão:** desativar o usuário, trocar a senha (pelo próprio usuário em `PATCH /auth/senha` ou pelo ADMIN em `PATCH /usuarios/{id}/senha`) e trocar o perfil (`PATCH /usuarios/{id}/perfil`), já que o token leva o perfil. Reativar não incrementa nem devolve os tokens antigos: o usuário entra de novo.
5. **Trocar a própria senha derruba também o token usado na troca**, e o usuário entra de novo com a senha nova. A resposta continua `204`, sem token novo.
6. **Token sem `ver`** (emitido antes desta mudança) é recusado: todos entram de novo uma vez, depois da atualização.

## Consequências

- **Positivas:** desligar alguém ou trocar uma senha vazada corta o acesso na hora, sem esperar a expiração; tudo com uma coluna e sem armazenamento novo.
- **Negativas:**
  - Uma consulta ao banco por requisição autenticada (busca pela chave única do login). Aceitável no volume do projeto; se pesar, cabe um cache curto. Adotado depois no [ADR 0005](0005-cache-curto-da-credencial.md).
  - Não dá para derrubar um token específico, só todos os do usuário.
  - Quem troca a própria senha precisa entrar de novo.

## Alternativas descartadas

- **Lista de tokens revogados (por `jti`):** permite revogar um token só, mas exige guardar a lista até cada token expirar e consultá-la a cada requisição, sem ganho para os casos de hoje.
- **Só checar se o usuário está ativo:** cobre a desativação, mas não a troca de senha.
- **Token curto com *refresh token*:** reduz a janela sem consultar o banco a cada requisição, mas acrescenta um fluxo inteiro (emissão, armazenamento e revogação do *refresh*) e ainda deixa uma janela.
- **Devolver um token novo ao trocar a própria senha:** evita o novo login, mas muda a resposta de `PATCH /auth/senha` (hoje `204`) e não traz ganho de segurança.
