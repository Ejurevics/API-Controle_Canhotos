# ADR 0002: Autenticação com JWT próprio e perfis ADMIN e OPERADOR

- **Status:** aceita
- **Data:** 2026-10-14

## Contexto

A API não tinha autenticação: qualquer um alcançando a porta podia criar coletas, mudar status e registrar canhotos. É um projeto de portfólio, usado por pessoas (pelo Swagger ou por um front-end), sem provedor de identidade disponível.

## Decisão

1. **Login próprio com JWT.** `POST /auth/login` recebe login e senha e devolve um token JWT, enviado nas demais requisições em `Authorization: Bearer <token>`. A sessão é sem estado (sem cookie, sem CSRF).
2. **Emissão e validação pelo Spring Security** (`oauth2-resource-server` com Nimbus), sem código próprio de parsing de JWT. Assinatura HS256 com segredo de pelo menos 32 bytes; o token traz emissor (`controle-canhotos`), login (`sub`), perfil e expiração (padrão 1 hora). Token adulterado, expirado ou de outro emissor responde `401`.
3. **Dois perfis:** `OPERADOR` faz o dia a dia (coletas, cargas, canhotos); `ADMIN` pode tudo, inclusive corrigir a chave de acesso provisória e (no próximo passo) gerenciar usuários.
4. **Usuários no banco** (tabela `usuario`, migration V4), com senha em **BCrypt** e login normalizado (sem espaços nas pontas, minúsculo).
5. **Primeiro ADMIN por variável de ambiente.** Na subida, se não houver nenhum ADMIN, o sistema cria um com `ADMIN_LOGIN` e `ADMIN_SENHA`. Depois disso, essas variáveis são ignoradas.
6. **Erros no formato da API:** `401` (sem token ou token inválido, com `WWW-Authenticate: Bearer`) e `403` (perfil sem permissão) saem como `ProblemDetail`, como os demais erros.
7. **Login com erro não revela o motivo:** login inexistente, usuário inativo e senha errada respondem igual (`401`, "Login ou senha inválidos"), e o login inexistente também passa pelo BCrypt, para o tempo de resposta não denunciar quais logins existem.
8. **Segredos fora do repositório.** O `docker compose` exige `JWT_SECRET` e `ADMIN_SENHA` no `.env` e não sobe sem eles. Só o perfil `dev` (máquina local e testes) tem valores de desenvolvimento.

Rotas públicas: `POST /auth/login`, `/actuator/health` e a documentação (`/swagger-ui.html`, `/v3/api-docs`). O Swagger ganha o botão "Authorize" para colar o token.

## Consequências

- **Positivas:** toda operação exige usuário identificado; permissões por perfil; nenhuma dependência externa.
- **Negativas:** o `docker compose` deixa de subir sem configuração (é preciso copiar `.env.example` para `.env` e preencher dois valores); os clientes da API passam a precisar de login.
- **Riscos aceitos:**
  - Token não é revogável antes de expirar: desativar um usuário bloqueia novos logins, mas um token já emitido vale até a expiração (1 hora por padrão).
  - Sem limite de tentativas de login (rate limiting) nesta versão. Tratado depois no [ADR 0003](0003-limite-tentativas-login.md).
  - Segredo simétrico (HS256): quem valida também consegue emitir. Aceitável com um único serviço.

## Alternativas descartadas

- **Provedor externo (Keycloak, Google, Entra ID):** mais robusto e com login único, mas exige subir e configurar outro serviço, desproporcional para o projeto.
- **Chave de API fixa:** simples, mas não identifica quem fez cada ação e é ruim para pessoas.
- **Sessão com cookie:** exigiria CSRF e estado no servidor, sem ganho para uma API consumida por token.
- **Implementar JWT à mão:** risco de erro em validação de assinatura e datas; o Spring Security já resolve.
- **Três perfis (com CONSULTA):** sem uso real hoje; pode ser adicionado depois sem quebrar os dois atuais.

## Plano de entrega

1. Este ADR e o núcleo da autenticação: usuário, admin inicial, login, proteção das rotas e perfis.
2. Gestão de usuários pelo ADMIN (criar, listar, desativar, trocar senha).

### Regras da gestão de usuários (entrega 2)

- `/usuarios` é só do ADMIN: criar, listar (filtros `perfil` e `ativo`), buscar, desativar ou reativar (`PATCH /usuarios/{id}/ativo`) e definir nova senha (`PATCH /usuarios/{id}/senha`).
- Qualquer usuário logado troca a própria senha em `PATCH /auth/senha`, informando a atual. O ADMIN inicial usa isso para sair da senha do `.env`.
- Usuários não são apagados, só desativados, para não perder a referência de quem existiu.
- Ninguém desativa o próprio usuário, e o último ADMIN ativo não pode ser desativado: sem ele ninguém mais gerencia usuários, e o ADMIN inicial só é recriado quando não existe ADMIN nenhum.
- Senha de 8 a 72 caracteres e no máximo 72 bytes em UTF-8, o limite do BCrypt.
- Trocar o perfil de um usuário fica de fora por enquanto (não estava no escopo).
