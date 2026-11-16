# ADR 0006: Anexar o comprovante do canhoto

- **Status:** aceita
- **Data:** 2026-11-16

## Contexto

Hoje o sistema registra que o canhoto voltou (`RECEBIDO` ou `DIVERGENTE`, com data, observação e quem registrou), mas não guarda o comprovante: a foto ou o PDF do canhoto assinado. Quem precisa provar a entrega a um cliente procura o papel ou o arquivo fora do sistema. A divergência também perde a evidência (rasura, assinatura faltando, carimbo errado).

O comprovante chega de formas diferentes: foto tirada pelo motorista no celular (JPEG), print ou imagem escaneada (PNG) e digitalização em lote (PDF). Às vezes chega mais de um arquivo para o mesmo canhoto (frente e verso, uma foto ruim e outra boa), e às vezes alguém envia o arquivo errado.

## Decisão

1. **Anexo ligado ao canhoto, por rota própria.** O anexo pertence ao canhoto da NF e não a um registro do histórico. O envio é `multipart/form-data` numa rota separada, e o `POST /notas-fiscais/{id}/canhoto/registros` continua em JSON, sem mudança:

   | Método | Rota | Descrição |
   |---|---|---|
   | `POST` | `/notas-fiscais/{id}/canhoto/anexos` | Envia um arquivo (campo `arquivo`); `201` + `Location` com os dados do anexo |
   | `GET` | `/notas-fiscais/{id}/canhoto/anexos/{anexoId}/arquivo` | Baixa o arquivo |
   | `POST` | `/notas-fiscais/{id}/canhoto/anexos/{anexoId}/remocao` | Remove o anexo (`{"motivo": "..."}`); `204` |

   `GET /notas-fiscais/{id}/canhoto` passa a trazer `anexos`: os dados dos anexos ativos (id, nome, tipo, tamanho, quando e quem enviou), sem o conteúdo.

2. **Vários anexos por canhoto**, até 10 ativos. Cada um guarda quem enviou (`enviadoPor`, pela auditoria do Spring Data, como os históricos) e quando.

3. **Envio só com a carga `ENTREGUE`**, a mesma regra do registro do canhoto. Dá para anexar antes ou depois de registrar `RECEBIDO` ou `DIVERGENTE`: o operador pode ter o arquivo antes de conferir o canhoto, ou receber a foto boa dias depois. Carga em outro status responde `422`.

4. **Tipos aceitos: JPEG, PNG e PDF, conferidos pelo conteúdo.** O tipo vem da assinatura no início do arquivo (`FF D8 FF`, `89 50 4E 47 0D 0A 1A 0A`, `%PDF-`), não do `Content-Type` nem da extensão enviados pelo cliente, que podem mentir. O tipo guardado é o detectado. Arquivo vazio ou de outro tipo responde `422`.

5. **Até 5 MB por arquivo**, configurável por `ANEXO_TAMANHO_MAXIMO`. O limite também vale no multipart do Spring, para recusar o envio antes de ler tudo; acima dele, a resposta é `413`. Uma foto de celular fica entre 2 e 4 MB, e um PDF de uma página escaneada, abaixo de 1 MB.

6. **O mesmo arquivo não entra duas vezes no mesmo canhoto.** Cada anexo guarda o SHA-256 do conteúdo, e um arquivo idêntico a um anexo ativo do canhoto responde `422`. Isso evita a duplicata do clique duplo ou do reenvio.

7. **Conteúdo no PostgreSQL, em tabela separada.** `anexo_canhoto` guarda os dados (canhoto, nome, tipo, tamanho, SHA-256, envio e remoção), e `anexo_canhoto_conteudo` guarda só os bytes (`bytea`), com chave igual à do anexo. A listagem e o `GET` do canhoto leem só a primeira tabela; os bytes só são lidos no download. Arquivo e dados são gravados na mesma transação e entram no mesmo backup. O acesso ao conteúdo passa pelo repositório do conteúdo, para trocar por um armazenamento de objetos (S3) sem mexer nas regras.

8. **Remoção lógica, com motivo.** Remover marca o anexo com quem removeu, quando e o motivo (obrigatório, até 500 caracteres). O anexo some do `GET` do canhoto e o download responde `404`, mas os dados e os bytes ficam no banco. Anexo removido não conta no limite de 10 nem na checagem de duplicata.

9. **Quem pode remover:** quem enviou o anexo ou um ADMIN. Outro OPERADOR recebe `403`. A remoção não depende do status da carga.

10. **Download seguro.** A resposta leva o tipo guardado, `Content-Disposition: attachment` com o nome original (codificado conforme a RFC 6266) e `X-Content-Type-Options: nosniff`, para o navegador não interpretar o arquivo como página. O nome original é guardado sem caminho (só o que vem depois da última `/` ou `\`), com até 255 caracteres.

## Consequências

- **Positivas:** o comprovante fica junto do canhoto, com quem enviou e quando; a divergência ganha evidência. Não há infraestrutura nova: o compose, o backup e os testes (Testcontainers com PostgreSQL) continuam os mesmos.
- **Negativas e riscos aceitos:**
  - **O banco cresce com os arquivos.** Na ordem de 100 canhotos por dia com 1 MB cada, são cerca de 3 GB por mês. O backup e a restauração do banco ficam mais lentos. Se pesar, o conteúdo migra para um armazenamento de objetos (S3 ou MinIO) atrás do mesmo repositório, com uma migração que copie os bytes.
  - **O arquivo inteiro passa pela memória** no envio e no download (até 5 MB por requisição). Com poucos usuários simultâneos é aceitável; com muitos, o caminho é o envio direto para o armazenamento de objetos com URL assinada.
  - **Remoção lógica não libera espaço.** Apagar os bytes de anexos removidos há muito tempo fica para quando for preciso.
  - **Não há antivírus.** A checagem do tipo pelo conteúdo e o download como `attachment` com `nosniff` reduzem o risco, mas um PDF malicioso continua sendo um PDF válido. Quem abre o arquivo depende do leitor de PDF.
  - **O sistema não confere se a imagem é mesmo do canhoto daquela NF.** A responsabilidade é de quem envia; o anexo guarda quem foi.

## Alternativas descartadas

- **Arquivo em disco (volume Docker), com o caminho no banco:** deixa o banco leve, mas banco e disco podem divergir (arquivo sem registro ou registro sem arquivo) e o backup passa a ser em dois lugares.
- **S3 compatível (MinIO no compose):** é o mais próximo de produção em nuvem, mas acrescenta serviço, SDK e Testcontainers a mais sem necessidade no volume atual. Fica como evolução (item 7).
- **Bytes na mesma tabela dos dados:** sem *bytecode enhancement* do Hibernate, um atributo `byte[]` é carregado junto com a entidade, e listar os anexos leria todos os arquivos.
- **Anexo ligado a cada registro do histórico, enviado no `POST /registros`:** deixaria claro qual arquivo embasou cada registro, mas tornaria o registro `multipart` (mudança incompatível) e impediria anexar depois de registrar.
- **Exigir o canhoto já registrado para anexar:** obriga uma ordem que não é a do trabalho real. Anexar a qualquer momento, inclusive antes da entrega, permitiria comprovante de entrega que ainda não aconteceu.
- **Confiar no `Content-Type` ou na extensão:** são informados pelo cliente e não garantem o conteúdo.
- **HEIC e WebP:** cobririam a foto direto do iPhone, mas o navegador não exibe HEIC e o tipo é menos comum no fluxo da transportadora. Ficam para quando aparecer a necessidade.
- **Remoção definitiva:** perde a trilha de auditoria, que o resto do sistema preserva.
- **Remoção só pelo ADMIN:** todo engano do operador viraria pedido ao ADMIN. Por qualquer usuário: um operador poderia remover o comprovante enviado por outro.

## Plano de entrega

1. Este ADR.
2. Envio, consulta e download (migration V10, `anexos` no `GET` do canhoto) e README.
3. Remoção lógica e README.

## Notas da implementação

- **`413` acima do limite do multipart:** o Tomcat recusa o envio e, para conseguir entregar a resposta, precisa descartar o resto do corpo. O padrão dele é descartar até 2 MB e, acima disso, encerrar a conexão sem resposta; `server.tomcat.max-swallow-size` passou para 32 MB, para que um envio de até 32 MB receba o `413`.
- **`Expect: 100-continue`:** quando o cliente manda esse cabeçalho (o `curl` faz isso em arquivos grandes) e a resposta não é 2xx, o Tomcat não descarta o resto do corpo e fecha a conexão (`Http11Processor.checkExpectationAndResponseStatus`). O cliente pode ver a conexão encerrada em vez do `413`. Envios dentro do limite não são afetados. Ajustar `continueResponseTiming` ou tirar o limite da requisição inteira não mudou esse comportamento; ficou documentado no README.
