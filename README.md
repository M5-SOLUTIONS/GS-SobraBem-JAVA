# SobraBem — API Java

Padarias, feiras e mercados publicam a comida que sobrou no fim do dia; ONGs e famílias veem as doações
da cidade, reservam uma quantidade e retiram com um código. Esta é a API usada pelo app mobile.

O sistema garante duas regras: **ninguém reserva mais do que existe** e **ninguém reserva depois do prazo**.

## Integrantes

| Nome | RM |
|---|---|
|  |  |
|  |  |

## Tecnologias

- Java 21 e Spring Boot 4.1
- Spring Data JPA + Oracle, com o schema versionado pelo Flyway
- Spring Security + JWT (HS256), com 3 papéis: `DOADOR`, `RECEPTOR` e `ADMIN`
- RabbitMQ (envio de e-mails assíncrono) + SMTP do Gmail
- Spring AI (orientação de conservação com RAG)
- springdoc-openapi (Swagger)

## Arquitetura

```
app mobile ──HTTP + JWT──▶ API Java ──▶ Oracle (Flyway)
                              │
                              ├──▶ RabbitMQ (sb.events → sb.emails) ──▶ consumidor ──▶ Gmail
                              └──▶ IA (orientação de conservação)
```

- **Saldo atômico:** a reserva desconta o saldo num único `UPDATE` condicional. Se dois receptores
  disputam a última unidade, o banco deixa só um passar.
- **Uma reserva ativa por receptor em cada doação:** garantida por um índice único baseado em função,
  inclusive quando chegam duas requisições ao mesmo tempo.
- **E-mail fora do caminho da requisição:** a reserva é gravada, um evento vai para a fila depois do
  commit, e um consumidor envia o e-mail. Se o servidor de e-mail ou o RabbitMQ cair, a reserva funciona
  do mesmo jeito.
- **Expiração:** a cada 5 minutos, doações vencidas e suas reservas ativas viram `EXPIRADA`.

## Pré-requisitos

- Java 21 e Maven 3.9+
- Docker (para o RabbitMQ)
- Acesso a um schema Oracle
- Para envio real de e-mail: uma conta Gmail com verificação em duas etapas e uma **senha de app**

## Como rodar

**1. Variáveis de ambiente**

```bash
cp .env.example .env
```

| Variável | Para quê |
|---|---|
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | conexão com o Oracle |
| `JWT_SECRET` | segredo do token (mínimo de 32 caracteres) |
| `ADMIN_EMAIL`, `ADMIN_SENHA` | ADMIN criado na primeira subida |
| `RABBITMQ_HOST`, `RABBITMQ_PORT`, `RABBITMQ_USER`, `RABBITMQ_PASSWORD`, `RABBITMQ_VHOST`, `RABBITMQ_SSL` | broker de mensagens |
| `MAIL_USERNAME`, `MAIL_PASSWORD` | conta Gmail e senha de app |
| `MAIL_ENVIO_HABILITADO` | `false`: os e-mails só aparecem no log; `true`: são enviados de verdade |
| `IA_MODO`, `GEMINI_API_KEY` | `fake` (padrão) ou `gemini`, com a chave do Google AI Studio |

**2. RabbitMQ**

```bash
docker compose up -d
```

**3. API**

```bash
mvn spring-boot:run
```

Na primeira subida, o Flyway cria as tabelas e as 5 categorias, e a API cria o ADMIN a partir de
`ADMIN_EMAIL` e `ADMIN_SENHA`.

| Recurso | Endereço |
|---|---|
| Swagger | http://localhost:8080/swagger-ui.html |
| Painel do RabbitMQ | http://localhost:15672 |

## Usuários

- **ADMIN:** não se cadastra pela API; é criado na subida com `ADMIN_EMAIL` e `ADMIN_SENHA`. Ele mantém
  as categorias e administra os usuários.
- **Doador e receptor:** cadastram-se em `POST /api/auth/registro`.

```json
{
  "nome": "Padaria Pão Quente",
  "email": "contato@paoquente.com.br",
  "senha": "Senha123",
  "telefone": "11987654321",
  "papel": "DOADOR",
  "endereco": "Avenida Paulista, 1000 - Bela Vista",
  "cidade": "São Paulo",
  "uf": "SP"
}
```

Para um receptor, use `"papel": "RECEPTOR"` e `"endereco": null`.

## Fluxo doar → reservar → retirar

1. O doador faz login (`POST /api/auth/login`) e publica a sobra (`POST /api/doacoes`), com um prazo de
   retirada entre 30 minutos e o limite da categoria.
2. Opcionalmente, antes de publicar, ele pede uma orientação de conservação (`POST /api/ia/orientacoes`).
3. O receptor vê as doações da cidade (`GET /api/doacoes?cidade=São Paulo`) e reserva uma quantidade
   (`POST /api/reservas`). Ele recebe um **código de retirada** de 6 caracteres, e os dois recebem e-mail.
4. No balcão, o receptor mostra o código, e o doador confirma a retirada
   (`PATCH /api/reservas/{id}/retirada`). A reserva passa a `RETIRADA`.
5. Quando o prazo vence, a doação e as reservas não retiradas passam a `EXPIRADA`.

No Swagger, faça o login, copie o `token` da resposta e cole em **Authorize**.

## Endpoints

| Método | Rota | Quem |
|---|---|---|
| POST | `/api/auth/registro` | público |
| POST | `/api/auth/login` | público |
| GET, PUT | `/api/usuarios/me` | logado |
| GET | `/api/usuarios`, `/api/usuarios/{id}` | ADMIN |
| DELETE | `/api/usuarios/{id}` | ADMIN |
| GET | `/api/categorias`, `/api/categorias/{id}` | logado |
| POST, PUT, DELETE | `/api/categorias` | ADMIN |
| POST | `/api/doacoes` | DOADOR |
| GET | `/api/doacoes` (vitrine), `/api/doacoes/{id}` | logado |
| GET | `/api/doacoes/minhas`, `/api/doacoes/{id}/reservas` | DOADOR (dono) |
| PUT | `/api/doacoes/{id}` | DOADOR (dono) |
| PATCH | `/api/doacoes/{id}/cancelamento` | DOADOR (dono) |
| DELETE | `/api/doacoes/{id}` | DOADOR (dono) ou ADMIN |
| POST | `/api/reservas` | RECEPTOR |
| GET | `/api/reservas/minhas` | RECEPTOR |
| PATCH | `/api/reservas/{id}/cancelamento` | RECEPTOR (dono) |
| PATCH | `/api/reservas/{id}/retirada` | DOADOR da doação |
| DELETE | `/api/reservas/{id}` | ADMIN |
| POST | `/api/ia/orientacoes` | DOADOR |

As listagens são paginadas (`page`, `size` até 50, `sort`) e respondem no formato
`{ "content": [...], "page": { "size", "number", "totalElements", "totalPages" } }`.
As datas saem sempre em UTC (`2026-11-10T23:00:00Z`).

## Erros

Todo erro 4xx/5xx segue o mesmo formato (RFC 9457 / `ProblemDetail`):

```json
{
  "type": "https://sobrabem.app/erros/validacao",
  "title": "Dados inválidos",
  "status": 400,
  "detail": "1 campo inválido",
  "instance": "/api/reservas",
  "codigo": "VALIDACAO",
  "timestamp": "2026-11-10T21:42:10Z",
  "erros": [ { "campo": "quantidade", "mensagem": "deve ser maior ou igual a 1" } ]
}
```

Códigos: `VALIDACAO`, `PRAZO_INVALIDO`, `CODIGO_RETIRADA_INVALIDO`, `NAO_AUTENTICADO`,
`CREDENCIAIS_INVALIDAS`, `ACESSO_NEGADO`, `NAO_ENCONTRADO`, `EMAIL_EM_USO`, `CATEGORIA_DUPLICADA`,
`REGISTRO_EM_USO`, `SALDO_INSUFICIENTE`, `DOACAO_INDISPONIVEL`, `RESERVA_DUPLICADA`, `ESTADO_INVALIDO`,
`ERRO_INTERNO` e `IA_INDISPONIVEL`.

## E-mails

| Evento | Quem recebe |
|---|---|
| Reserva criada | o doador (quem reservou, quanto e até quando) e o receptor (comprovante com o código e o endereço) |
| Reserva cancelada pelo receptor | o doador |
| Doação cancelada pelo doador | cada receptor com reserva ativa nela |

Com `MAIL_ENVIO_HABILITADO=false`, que é o padrão, a mensagem passa pela fila normalmente, mas o
consumidor só registra o e-mail no log.

## IA

A orientação de conservação usa o Gemini pelo Spring AI e precisa da variável `GEMINI_API_KEY`
(com `IA_MODO=gemini`). Com `IA_MODO=fake`, que é o padrão, a API devolve uma resposta fixa, para que o
app funcione sem a chave.

## Testes

- **Postman:** a coleção `postman/SobraBem.postman_collection.json` cobre os fluxos de ponta a ponta,
  as regras de negócio e o formato de erro. Preencha `adminEmail` e `adminSenha` nas variáveis da coleção
  e use **Run collection**. Também roda pelo terminal:
  ```bash
  npx newman run postman/SobraBem.postman_collection.json --env-var adminEmail=... --env-var adminSenha=...
  ```
- **Unitários:**
  ```bash
  mvn test
  ```

## Deploy

API publicada em: _(preencher com a URL)_