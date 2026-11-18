# SobraBem — API Java

Padarias, feiras e mercados publicam a comida que sobrou no fim do dia; ONGs e famílias veem as doações
da sua cidade, reservam uma quantidade e retiram no balcão com um código. Esta é a API REST usada pelo
app mobile.

**API publicada:** https://gs-sobrabem-java-final.onrender.com  
**Swagger:** https://gs-sobrabem-java.onrender.com/swagger-ui.html  
**Vídeo pitch:** https://youtu.be/g3DAfSZLWVM

> O serviço roda no plano gratuito do Render, que desliga a aplicação depois de um tempo sem acesso.
> O primeiro acesso depois disso leva alguns minutos, enquanto a API sobe de novo.
>
> A função de envio de emails não está disponível devido ao bloqueio automático no plano gratuito do Render.

## Integrantes

| Nome | RM |
|---|---|
| Daniel Fonseca de Almeida | RM 563045 |
| Erick de Faria Gama | RM 561951 |
| Guilherme Batista Cintra | RM 562850 |
| Matheus Nascimento Corregio | RM 563765 |
| Pedro Fonseca de Almeida | RM 563466 |

---

# Sumário

- [Objetivo](#objetivo)
- [Impacto para o cidadão e ODS](#impacto-para-o-cidadão-e-ods)
- [Tecnologias](#tecnologias)
- [Arquitetura](#arquitetura)
- [Estrutura do projeto](#estrutura-do-projeto)
- [Segurança e perfis de acesso](#segurança-e-perfis-de-acesso)
- [Banco de dados](#banco-de-dados)
- [Mensageria e e-mails](#mensageria-e-e-mails)
- [Inteligência artificial](#inteligência-artificial)
- [Instalação e execução](#instalação-e-execução)
- [Acessando a aplicação](#acessando-a-aplicação)
- [Usuários de teste](#usuários-de-teste)
- [Endpoints da API REST](#endpoints-da-api-rest)
- [Fluxos completos](#fluxos-completos)
- [Validações e regras de negócio](#validações-e-regras-de-negócio)
- [Tratamento de exceções](#tratamento-de-exceções)
- [Testes](#testes)
- [Deploy](#deploy)
- [Declaração de uso de IA](#declaração-de-uso-de-ia)

---

# Objetivo

A aplicação reduz o desperdício de alimentos ligando quem tem sobra a quem precisa, permitindo:

- Cadastro e autenticação de doadores (padarias, feiras, mercados) e receptores (ONGs, famílias)
- Publicação de doações com quantidade, categoria e prazo de retirada
- Vitrine de doações ativas, filtrada por cidade e categoria
- Reserva de parte de uma doação, com código de retirada
- Confirmação da retirada pelo doador, conferindo o código
- Expiração automática de doações e reservas vencidas
- E-mails automáticos a cada reserva e cancelamento, enviados de forma assíncrona
- Sugestão de orientação de conservação do alimento por IA generativa

O sistema garante duas regras centrais: **ninguém reserva mais do que existe** e **ninguém reserva
depois do prazo**, mesmo com várias pessoas reservando ao mesmo tempo.

---

# Impacto para o cidadão e ODS

**Quem é beneficiado:** famílias e ONGs que recebem comida em boas condições, de graça e perto de
casa; e os comércios, que deixam de descartar alimentos sem precisar organizar a doação por conta
própria.

**Como a solução ajuda:** o comércio publica a sobra em poucos segundos, e o receptor vê o que está
disponível na sua cidade e reserva com a garantia de que o alimento existe e ainda está no prazo. A
orientação de conservação gerada por IA acompanha a doação até quem vai consumi-la.

**Objetivos de Desenvolvimento Sustentável (ONU):**

| ODS | Como o SobraBem contribui |
|---|---|
| **2 — Fome zero e agricultura sustentável** | Leva alimento que seria descartado a famílias e ONGs que precisam dele |
| **11 — Cidades e comunidades sustentáveis** | Conecta comércios e comunidades da mesma cidade e reduz o lixo orgânico urbano |

**Como o impacto é medido:** o próprio banco registra quantas doações foram publicadas, quantas
unidades foram reservadas e quantas foram de fato retiradas (reservas `RETIRADA`), por doador, por
categoria e por cidade.

---

# Tecnologias

| Tecnologia | Uso |
|---|---|
| Java 21 | Linguagem |
| Spring Boot 4.1 | Framework base |
| Spring Data JPA / Hibernate | Persistência |
| Oracle Database | Banco de dados |
| Flyway | Versionamento do schema do banco |
| Spring Security + OAuth2 Resource Server | Autenticação e autorização com JWT (HS256) |
| Bean Validation | Validação dos dados de entrada |
| RabbitMQ (Spring AMQP) | Mensageria para o envio assíncrono de e-mails |
| JavaMail + Gmail (SMTP) | Envio dos e-mails |
| Spring AI + Google Gemini | Orientação de conservação por IA generativa |
| springdoc-openapi | Documentação da API (Swagger) |
| Lombok | Redução de código repetitivo nas entidades |
| Docker / Docker Compose | Imagem da aplicação e RabbitMQ local |
| Maven | Build e dependências |

---

# Arquitetura

```
                                      ┌──▶ Oracle (schema versionado pelo Flyway)
                                      │
app mobile ──HTTP + JWT──▶ API Java ──┼──▶ RabbitMQ (exchange sb.events → fila sb.emails)
                                      │         └──▶ EmailConsumer ──▶ Gmail
                                      │
                                      └──▶ Spring AI ──▶ Google Gemini
```

**Arquitetura distribuída (requisito 5): opção A, mensageria com RabbitMQ.** Os e-mails de reserva
são publicados como eventos no RabbitMQ e consumidos de forma assíncrona. Detalhes em
[Mensageria e e-mails](#mensageria-e-e-mails).

O projeto segue arquitetura em camadas. As regras de negócio ficam nos services; os controllers só
recebem a requisição, chamam o service e devolvem o JSON.

Algumas decisões do desenho:

- **Saldo atômico:** a reserva desconta o saldo num único `UPDATE` condicional (só passa se a doação
  está ativa, no prazo e com saldo suficiente). Se dois receptores disputam a última unidade, o banco
  deixa só um passar.
- **Uma reserva ativa por receptor em cada doação:** garantida por um índice único baseado em função,
  inclusive quando chegam duas requisições do mesmo receptor ao mesmo tempo.
- **Travamento ordenado:** edição, cancelamento e retirada travam primeiro a doação e depois a reserva
  (`SELECT ... FOR UPDATE`), sempre na mesma ordem, para evitar deadlocks.
- **E-mail fora do caminho da requisição:** a reserva é gravada, um evento vai para a fila só depois do
  commit, e um consumidor envia o e-mail. Se o servidor de e-mail ou o RabbitMQ cair, a reserva funciona
  do mesmo jeito.
- **IA isolada atrás de uma interface:** o resto da API não depende do Gemini. Se a IA falhar ou demorar,
  só o endpoint de orientação responde 503.
- **Datas em UTC:** todas as datas são gravadas e devolvidas em UTC (`2026-11-10T23:00:00Z`).

## Camadas

### Controller

`@RestController` sob `/api`, consumidos pelo app mobile. Cada endpoint é documentado no Swagger com
os possíveis códigos de resposta.

### Service

Concentra as regras de negócio: validação de posse dos dados, prazo de retirada por categoria, saldo,
transições de status, código de retirada e publicação dos eventos de e-mail.

### Repository

Interfaces Spring Data JPA, com consultas JPQL para a vitrine, o desconto atômico de saldo, o
travamento das linhas e a expiração em lote.

### Domain

Entidades JPA (`entity`), enums de domínio (`enums`) e o conversor `SimNaoConverter`, que mapeia
`boolean` para as colunas `CHAR(1)` com `S`/`N`.

### DTO

Registros (`record`) que trafegam entre cliente e API, separados em `request` e `response`.

### Security

Emissão e validação do JWT, conversão do papel do token em permissão e respostas padronizadas de
401 e 403.

### Email

Evento de domínio, publicador (envia para o RabbitMQ depois do commit) e consumidor (monta e envia
os e-mails).

### AI

Interface `OrientacaoService` e suas duas implementações: a real, com Gemini, e uma simulada, para
desenvolvimento sem chave.

### Scheduler

Tarefa agendada que expira doações e reservas vencidas.

---

# Estrutura do projeto

```text
src/main/java/br/com/sobrabem
├── ai              Interface da IA, implementação com Gemini e implementação simulada
├── config          Segurança, RabbitMQ, Swagger e criação do ADMIN inicial
├── controller      Controllers REST (/api/**)
├── domain
│   ├── converter   SimNaoConverter (boolean ↔ 'S'/'N')
│   ├── entity      Categoria, Usuario, Doacao, Reserva
│   └── enums       Papel, StatusDoacao, StatusReserva, Unidade
├── dto
│   ├── request     Dados de entrada
│   └── response    Dados de saída
├── email           ReservaEmailEvent, EmailPublisher, EmailConsumer
├── exception       CodigoErro, NegocioException e handler global
├── repository      Interfaces Spring Data JPA
├── scheduler       ExpiracaoScheduler
├── security        TokenService, usuário autenticado, handlers de 401 e 403
└── service         Regras de negócio

src/main/resources
├── db/migration                    Migrations Flyway
├── application.properties          Configuração principal
└── application-gemini.properties   Configuração da IA real (profile gemini)

docs/SobraBem.postman_collection.json   Coleção de testes do Postman
docker-compose.yaml                     RabbitMQ local
Dockerfile                              Imagem da aplicação (build em duas etapas)
```

---

# Segurança e perfis de acesso

## Autenticação

A API é **stateless**: o login devolve um JWT assinado com HS256, válido por **2 horas**, que vai no
header `Authorization: Bearer <token>` de cada requisição. O token carrega o papel do usuário (claim
`papel`), que o Spring Security converte em permissão (`ROLE_DOADOR`, `ROLE_RECEPTOR`, `ROLE_ADMIN`).

Senhas são armazenadas com **BCrypt**.

## Perfis

| Perfil | Como é criado | Acesso |
|---|---|---|
| `DOADOR` | `POST /api/auth/registro` | Publica, edita, cancela e exclui as próprias doações; vê as reservas delas; confirma retiradas; pede orientação à IA |
| `RECEPTOR` | `POST /api/auth/registro` | Vê a vitrine, reserva, cancela as próprias reservas e consulta o código de retirada |
| `ADMIN` | Criado na primeira subida, a partir de `ADMIN_EMAIL` e `ADMIN_SENHA` | Mantém as categorias, administra usuários e pode excluir doações e reservas |

O ADMIN não pode ser cadastrado pela API.

## Isolamento de dados

Além da checagem por perfil nas rotas, os services validam a posse de cada recurso: um doador não
edita nem vê as reservas da doação de outro, e um receptor não cancela a reserva de outro, mesmo
informando o identificador correto.

O **código de retirada** só aparece para o receptor dono da reserva, e só enquanto ela está `ATIVA`.
O doador nunca vê o código: é ele quem confere o código que o receptor apresenta no balcão.

---

# Banco de dados

Oracle Database, com schema versionado por Flyway. A propriedade `spring.jpa.hibernate.ddl-auto` está
fixada em `validate`: o Hibernate nunca altera o schema, apenas confere se o mapeamento corresponde às
tabelas criadas pelas migrations.

O Flyway usa uma tabela de histórico própria (`sb_flyway_schema_history`), para não conflitar com
outros projetos no mesmo schema.

## Tabelas

Todas as tabelas usam o prefixo `sb_t_`.

| Tabela | Conteúdo |
|---|---|
| `sb_t_usuario` | Doadores, receptores e administradores, com credenciais e papel |
| `sb_t_categoria` | Categorias de alimento e o prazo máximo de retirada de cada uma |
| `sb_t_doacao` | Doações, com quantidade total, saldo disponível, prazo e status |
| `sb_t_reserva` | Reservas, com quantidade, status e código de retirada |

## Migrations

Localizadas em `src/main/resources/db/migration`:

| Arquivo | Conteúdo |
|---|---|
| `V1__tabelas.sql` | Tabelas de usuários, categorias, doações e reservas |
| `V2__indice_reserva_ativa.sql` | Índice único que permite só uma reserva ativa por receptor em cada doação |
| `V3__categorias.sql` | Carga das 5 categorias |

As migrations são aplicadas automaticamente na inicialização da aplicação.

## Categorias

| Id | Categoria | Prazo máximo de retirada |
|---|---|---|
| 1 | Pães e bolos | 12 h |
| 2 | Frutas, legumes e verduras | 24 h |
| 3 | Laticínios | 6 h |
| 4 | Refeições prontas | 4 h |
| 5 | Mercearia | 48 h |

## Relacionamentos

| Relacionamento | Cardinalidade |
|---|---|
| Usuário (doador) → Doação | Um doador publica várias doações |
| Categoria → Doação | Uma categoria agrupa várias doações |
| Doação → Reserva | Uma doação recebe várias reservas |
| Usuário (receptor) → Reserva | Um receptor faz várias reservas |

## Constraints

**Primary key:** identificação única, com `GENERATED AS IDENTITY`.

**Foreign key:** doação vinculada ao doador e à categoria; reserva vinculada à doação e ao receptor.

**Unique:** e-mail do usuário e nome da categoria.

**Check:** papel do usuário, status da doação e da reserva, unidade (`UNIDADE`, `KG`, `PACOTE`),
quantidades mínimas, saldo entre 0 e o total, prazo da categoria entre 1 e 48 horas e as colunas
`S`/`N`.

**Índice único baseado em função:** `sb_ux_reserva_ativa` indexa o par doação + receptor só quando a
reserva está `ATIVA`. Reservas canceladas, retiradas ou expiradas não contam, então o receptor pode
reservar de novo depois de cancelar.

Todas as constraints são nomeadas com o padrão `sb_pk_`, `sb_fk_`, `sb_uk_` e `sb_ck_`.

---

# Mensageria e e-mails

**Opção escolhida no requisito 5 (arquitetura distribuída): A, RabbitMQ**, com evento publicado e
consumido de forma assíncrona.

Os e-mails não são enviados durante a requisição. O fluxo é:

1. O service grava a reserva e publica um evento interno (`ReservaEmailEvent`).
2. Depois do commit da transação, o `EmailPublisher` envia o evento para o exchange `sb.events` do
   RabbitMQ, com a routing key `reserva.criada` ou `reserva.cancelada`.
3. A fila durável `sb.emails` recebe as mensagens, em JSON.
4. O `EmailConsumer` lê a fila, monta os textos e envia pelo Gmail.

Se o envio falhar, o consumidor tenta mais duas vezes (com espera de 2 s e depois 4 s). Se as três
tentativas falharem, a mensagem é descartada e o erro fica no log, sem afetar a reserva.

| Evento | Quem recebe |
|---|---|
| Reserva criada | O doador (quem reservou, quanto e até quando) e o receptor (comprovante com o código e o endereço de retirada) |
| Reserva cancelada pelo receptor | O doador |
| Doação cancelada pelo doador | Cada receptor com reserva ativa nela |

Com `MAIL_ENVIO_HABILITADO=false`, que é o padrão, a mensagem passa pela fila normalmente, mas o
consumidor só registra o e-mail no log. Isso permite testar todo o fluxo sem enviar e-mails reais.

---

# Inteligência artificial

Antes de publicar uma doação, o doador pode pedir uma orientação de conservação
(`POST /api/ia/orientacoes`): como guardar o alimento e até quando consumir. A sugestão não é gravada;
o app usa o texto para preencher o campo `orientacoesConservacao` da doação, e o doador pode editá-lo.

A IA é acessada pelo **Spring AI**, com o modelo **Gemini 3.5 Flash** do Google:

- o prompt pede no máximo 3 frases, em português, sobre conservação e prazo de consumo;
- o título e a descrição digitados pelo usuário são tratados só como dados, nunca como instruções;
- se o texto não for um alimento, ou se o modelo não souber orientar com segurança, a API devolve um
  aviso em vez de inventar uma resposta;
- a temperatura é baixa (0,2), para respostas estáveis.

O `IaService` executa a chamada numa thread virtual com tempo limite. Se o Gemini falhar ou passar do
limite, a API responde **503 `IA_INDISPONIVEL`**, e o resto do sistema continua funcionando.

## Modos de execução

A IA fica atrás da interface `OrientacaoService`, com duas implementações. Só uma sobe por vez:

| Modo | Como ativar | Comportamento |
|---|---|---|
| `gemini` | Profile `gemini` (padrão), com a variável `GEMINI_API_KEY` | Chama o Gemini de verdade |
| `fake` | Outro profile ativo, por exemplo `SPRING_PROFILES_ACTIVE=fake` | Resposta fixa, sem chave, para desenvolvimento e testes do app |

No modo `fake`, títulos com `#aviso`, `#erro` ou `#lento` simulam, nessa ordem, o aviso de "sem
orientação", o erro 503 e o estouro do tempo limite.

---

# Instalação e execução

## Pré-requisitos

- **Java 21** ou superior (`java -version` para conferir)
- **Maven 3.9+**, ou o wrapper `./mvnw` incluído no projeto
- **Docker**, para o RabbitMQ local
- **Acesso a um banco Oracle** com permissão para criar tabelas
- Para envio real de e-mails: uma conta Gmail com verificação em duas etapas e uma **senha de app**
- Para a IA real: uma chave do **Google AI Studio**

## 1. Clonar o repositório

```bash
git clone <url-do-repositorio>
cd sobrabem
```

## 2. Configurar as variáveis de ambiente

Copie o modelo e preencha os valores:

```bash
cp .env.example .env
```

Em desenvolvimento, a aplicação lê o arquivo `.env` da raiz do projeto. Em produção, use variáveis de
ambiente de verdade. Nenhum segredo é versionado: o `.env` está no `.gitignore`.

| Variável | Descrição | Exemplo |
|---|---|---|
| `DB_URL` | URL JDBC do Oracle | — |
| `DB_USER` | Usuário do banco | — |
| `DB_PASSWORD` | Senha do banco | — |
| `JWT_SECRET` | Chave de assinatura dos tokens, **mínimo de 32 caracteres** | — |
| `ADMIN_EMAIL` | E-mail do ADMIN criado na primeira subida | `admin@sobrabem.com` |
| `ADMIN_SENHA` | Senha do ADMIN | — |
| `ADDRESS_RABBIT` | Endereço do RabbitMQ, com usuário e senha | `amqp://sobrabem:sobrabem@localhost:5672` |
| `MAIL_USERNAME` | Conta Gmail que envia os e-mails | `sobrabem.gs@gmail.com` |
| `MAIL_PASSWORD` | Senha de app do Gmail (16 caracteres) | — |
| `MAIL_ENVIO_HABILITADO` | `true` envia de verdade; `false` só registra no log | `false` |
| `GEMINI_API_KEY` | Chave do Google AI Studio, para a IA real | — |

> A chave JWT precisa ter pelo menos 32 caracteres. O algoritmo HS256 exige uma chave de 256 bits, e a
> aplicação falha na inicialização com uma chave menor.

> Para um RabbitMQ na nuvem com TLS (como o CloudAMQP), use o endereço `amqps://`, na porta 5671.

**Pela IDE:** no IntelliJ IDEA, as variáveis também podem ir em *Run > Edit Configurations >
Environment variables*.

## 3. Subir o RabbitMQ

```bash
docker compose up -d
```

O painel do RabbitMQ fica em http://localhost:15672 (usuário e senha `sobrabem`).

## 4. Executar

```bash
./mvnw spring-boot:run
```

Ou, gerando o `.jar`:

```bash
./mvnw clean package -DskipTests
java -jar target/sobrabem-0.0.1-SNAPSHOT.jar
```

Ou com Docker:

```bash
docker build -t sobrabem .
docker run --env-file .env -p 8080:8080 sobrabem
```

Na primeira execução, o Flyway cria as tabelas e as 5 categorias, e a aplicação cria o ADMIN a partir
de `ADMIN_EMAIL` e `ADMIN_SENHA`.

Para rodar **sem a chave do Gemini**, ative outro profile, por exemplo com
`SPRING_PROFILES_ACTIVE=fake`. A IA passa a devolver a resposta simulada.

## 5. Verificar

A aplicação sobe em `http://localhost:8080`. Se a inicialização falhar, os erros mais comuns são:

| Mensagem | Causa |
|---|---|
| `JWT_SECRET precisa ter pelo menos 32 caracteres` | `JWT_SECRET` ausente ou curta |
| `Failed to configure a DataSource` | `DB_URL`, `DB_USER` ou `DB_PASSWORD` ausentes |
| `Schema-validation: missing table` | Migrations não aplicadas; confira as credenciais do banco |
| `Could not resolve placeholder 'GEMINI_API_KEY'` | Profile `gemini` ativo sem a chave; defina a chave ou use outro profile |
| `Connection refused` (RabbitMQ) | O container do RabbitMQ não está rodando |

---

# Acessando a aplicação

| Recurso | Local | Publicado |
|---|---|---|
| Swagger | http://localhost:8080/swagger-ui.html | https://gs-sobrabem-java.onrender.com/swagger-ui.html |
| Painel do RabbitMQ | http://localhost:15672 | — |

Para testar endpoints protegidos no Swagger:

1. Cadastre um usuário em `POST /api/auth/registro`
2. Faça login em `POST /api/auth/login` e copie o valor do campo `token`
3. Clique em **Authorize** no topo da página e cole o token
4. Os endpoints protegidos passam a exibir o cadeado fechado

Exemplo de cadastro de doador:

```json
{
  "nome": "Padaria Pão Quente",
  "email": "contato@paoquente.com.br",
  "senha": "Senha123",
  "telefone": "11987654321",
  "papel": "DOADOR",
  "endereco": "Avenida Paulista, 100 - Bela Vista",
  "cidade": "São Paulo",
  "uf": "SP"
}
```

Para um receptor, use `"papel": "RECEPTOR"` e `"endereco": null`: o endereço só é guardado para
doadores, porque é o local de retirada.

Para demonstrar o sistema por completo, crie **uma conta de cada perfil**: o doador publica, o receptor
reserva e o doador confirma a retirada.

---

# Usuários de teste

Na **API publicada**, estas contas já existem e podem ser usadas direto no login:

| Perfil | E-mail | Senha |
|---|---|---|
| `DOADOR` | `doador.teste@sobrabem.app` | `Senha123` |
| `RECEPTOR` | `receptor.teste@sobrabem.app` | `Senha123` |
| `ADMIN` | informado no `.txt` da entrega | informado no `.txt` da entrega |

As credenciais do ADMIN não ficam no README porque o repositório é público e essa conta administra
todos os usuários da API publicada.

**Rodando localmente**, o banco começa só com as categorias e o ADMIN. Crie as duas contas de teste
com `POST /api/auth/registro`:

```json
{
  "nome": "Padaria Teste",
  "email": "doador.teste@sobrabem.app",
  "senha": "Senha123",
  "telefone": "11999990000",
  "papel": "DOADOR",
  "endereco": "Avenida Paulista, 100 - Bela Vista",
  "cidade": "São Paulo",
  "uf": "SP"
}
```

```json
{
  "nome": "ONG Teste",
  "email": "receptor.teste@sobrabem.app",
  "senha": "Senha123",
  "telefone": "11999990001",
  "papel": "RECEPTOR",
  "endereco": null,
  "cidade": "São Paulo",
  "uf": "SP"
}
```

O ADMIN local usa o `ADMIN_EMAIL` e a `ADMIN_SENHA` do seu `.env`.

---

# Endpoints da API REST

Base: `http://localhost:8080/api`

Todas as rotas exigem o header `Authorization: Bearer <token>`, exceto cadastro e login.

## Autenticação

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/api/auth/registro` | Cadastra doador ou receptor |
| `POST` | `/api/auth/login` | Autentica e devolve o token (validade de 2 h) |

## Usuários

| Método | Rota | Perfil |
|---|---|---|
| `GET` | `/api/usuarios/me` | Qualquer usuário autenticado |
| `PUT` | `/api/usuarios/me` | Qualquer usuário autenticado (nome, telefone, endereço, cidade, UF) |
| `GET` | `/api/usuarios?papel=` | ADMIN |
| `GET` | `/api/usuarios/{id}` | ADMIN |
| `DELETE` | `/api/usuarios/{id}` | ADMIN, apenas usuários sem doações nem reservas |

## Categorias

| Método | Rota | Perfil |
|---|---|---|
| `GET` | `/api/categorias` | Qualquer usuário autenticado |
| `GET` | `/api/categorias/{id}` | Qualquer usuário autenticado |
| `POST` | `/api/categorias` | ADMIN |
| `PUT` | `/api/categorias/{id}` | ADMIN |
| `DELETE` | `/api/categorias/{id}` | ADMIN, apenas categorias sem doações |

## Doações

| Método | Rota | Perfil |
|---|---|---|
| `POST` | `/api/doacoes` | DOADOR |
| `GET` | `/api/doacoes?cidade=&categoriaId=` | Qualquer usuário autenticado (vitrine: doações ativas e no prazo) |
| `GET` | `/api/doacoes/{id}` | Qualquer usuário autenticado |
| `GET` | `/api/doacoes/minhas?status=` | DOADOR, apenas as suas |
| `GET` | `/api/doacoes/{id}/reservas?status=` | DOADOR dono da doação |
| `PUT` | `/api/doacoes/{id}` | DOADOR dono, apenas se `ATIVA` ou `ESGOTADA` |
| `PATCH` | `/api/doacoes/{id}/cancelamento` | DOADOR dono; cancela também as reservas ativas |
| `DELETE` | `/api/doacoes/{id}` | DOADOR dono ou ADMIN, apenas doações sem reservas |

> `DELETE` e `PATCH /cancelamento` são operações distintas: o primeiro apaga o registro (só se ninguém
> reservou), o segundo altera o status para `CANCELADA`, preservando o histórico e avisando os
> receptores por e-mail.

## Reservas

| Método | Rota | Perfil |
|---|---|---|
| `POST` | `/api/reservas` | RECEPTOR; devolve o código de retirada |
| `GET` | `/api/reservas/minhas?status=` | RECEPTOR, apenas as suas, com o código enquanto ativas |
| `PATCH` | `/api/reservas/{id}/cancelamento` | RECEPTOR dono, apenas antes do prazo |
| `PATCH` | `/api/reservas/{id}/retirada` | DOADOR da doação, informando o código |
| `DELETE` | `/api/reservas/{id}` | ADMIN |

## IA

| Método | Rota | Perfil |
|---|---|---|
| `POST` | `/api/ia/orientacoes` | DOADOR |

```json
{
  "titulo": "Iogurte natural",
  "descricao": "potes de 170 g",
  "categoriaId": 3
}
```

Resposta:

```json
{
  "orientacao": "Mantenha o iogurte na geladeira, abaixo de 5 °C, ...",
  "fontes": [],
  "aviso": null
}
```

Quando não há orientação segura, `orientacao` vem `null` e `aviso` traz a mensagem.

## Paginação

As listagens são paginadas (`page`, `size` até 50, `sort`) e respondem no formato:

```json
{
  "content": [ ... ],
  "page": { "size": 10, "number": 0, "totalElements": 25, "totalPages": 3 }
}
```

---

# Fluxos completos

## Fluxo 1: doar, reservar e retirar

1. O doador faz login e, opcionalmente, pede uma orientação de conservação à IA.
2. O doador publica a doação (`POST /api/doacoes`) com quantidade, unidade, categoria e prazo de
   retirada. A doação nasce `ATIVA`, com o saldo igual ao total.
3. O receptor vê a vitrine da sua cidade (`GET /api/doacoes?cidade=São Paulo`) e reserva uma quantidade
   (`POST /api/reservas`).
4. Numa única operação, o banco confere status, prazo e saldo e desconta a quantidade. Se o saldo chega
   a zero, a doação passa a `ESGOTADA`.
5. A reserva é criada com um **código de retirada** de 6 caracteres, devolvido na resposta. Depois do
   commit, o evento vai para o RabbitMQ, e o doador e o receptor recebem e-mail.
6. No balcão, o receptor mostra o código, e o doador confirma a retirada
   (`PATCH /api/reservas/{id}/retirada`). A reserva passa a `RETIRADA`.

## Fluxo 2: cancelamentos

- **O receptor cancela a reserva** antes do prazo: a reserva passa a `CANCELADA`, a quantidade volta
  para o saldo (uma doação `ESGOTADA` volta a `ATIVA`), e o doador recebe e-mail.
- **O doador cancela a doação:** a doação passa a `CANCELADA`, todas as reservas ativas dela também, e
  cada receptor recebe e-mail.

## Fluxo 3: expiração automática

A cada 5 minutos, uma tarefa agendada procura doações `ATIVA` ou `ESGOTADA` com o prazo vencido. As
reservas ativas dessas doações e as próprias doações passam a `EXPIRADA`, usando o mesmo instante nas
duas atualizações.

## Fluxo 4: concorrência

- Dois receptores disputando a última unidade: o `UPDATE` condicional deixa só um passar; o outro recebe
  `409 SALDO_INSUFICIENTE`.
- O mesmo receptor enviando duas reservas ao mesmo tempo: o índice único barra a segunda, o desconto de
  saldo dela é desfeito, e ela recebe `409 RESERVA_DUPLICADA`.

## Ciclo de vida

```
Doação:   ATIVA ⇄ ESGOTADA ──▶ EXPIRADA
            └──────┴─────────▶ CANCELADA

Reserva:  ATIVA ──▶ RETIRADA
            ├─────▶ CANCELADA
            └─────▶ EXPIRADA
```

---

# Validações e regras de negócio

## Validação de campos

Aplicada com Bean Validation nos DTOs. Os erros voltam no array `erros` da resposta, com o campo e a
mensagem.

| Entidade | Regras |
|---|---|
| Cadastro | Nome de 3 a 120 caracteres · e-mail válido, até 150 · senha de 8 a 60, com ao menos 1 letra e 1 número · telefone só com dígitos, 10 ou 11 (com DDD) · cidade obrigatória · UF com 2 letras maiúsculas · endereço obrigatório para doador |
| Doação | Título de 3 a 100 · descrição até 500 · categoria obrigatória · quantidade de 1 a 9999 · unidade `UNIDADE`, `KG` ou `PACOTE` · prazo obrigatório, com fuso (`2026-11-16T21:00:00-03:00`) · orientações até 1000 |
| Reserva | Doação obrigatória · quantidade de 1 a 9999 |
| Retirada | Código de retirada obrigatório |
| Categoria | Nome de 3 a 60 · descrição até 255 · prazo máximo de 1 a 48 horas |
| Orientação (IA) | Título de 3 a 100 · descrição até 500 · categoria obrigatória |

## Regras de negócio

- O prazo de retirada deve ficar **entre 30 minutos e o prazo máximo da categoria** a partir de agora
- Só doações `ATIVA` e no prazo podem ser reservadas
- Ninguém reserva mais do que o saldo disponível
- Cada receptor tem no máximo **uma reserva ativa por doação**
- Só doações `ATIVA` ou `ESGOTADA` podem ser editadas ou canceladas
- Ao editar, a quantidade total não pode ficar menor que o já reservado
- Doações com reservas não podem ser excluídas; devem ser canceladas
- O receptor só cancela reservas `ATIVA` e antes do prazo
- A retirada exige reserva `ATIVA` e o código correto (sem diferenciar maiúsculas)
- O código de retirada usa um alfabeto sem caracteres ambíguos (sem `0`, `O`, `1`, `I`) e é gerado com
  `SecureRandom`
- Categorias e usuários com registros vinculados não podem ser excluídos
- E-mails de usuários são únicos no sistema

---

# Tratamento de exceções

O tratamento é centralizado no `GlobalExceptionHandler` (`@RestControllerAdvice`). Todo erro 4xx/5xx
segue o mesmo formato, baseado na RFC 9457 (`ProblemDetail`):

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

O app decide o que fazer pelo campo `codigo`. O array `erros` aparece só em erros de validação. JSON
malformado, enum desconhecido e data inválida também apontam o campo que falhou.

| Código | Status | Situação |
|---|---|---|
| `VALIDACAO` | 400 | Campo inválido, JSON malformado, enum desconhecido, parâmetro de ordenação inválido |
| `PRAZO_INVALIDO` | 400 | Prazo de retirada fora da janela da categoria |
| `CODIGO_RETIRADA_INVALIDO` | 400 | Código de retirada não confere |
| `NAO_AUTENTICADO` | 401 | Token ausente, inválido ou expirado |
| `CREDENCIAIS_INVALIDAS` | 401 | E-mail ou senha incorretos no login |
| `ACESSO_NEGADO` | 403 | Perfil sem permissão ou recurso de outro usuário |
| `NAO_ENCONTRADO` | 404 | Identificador inexistente |
| `EMAIL_EM_USO` | 409 | E-mail já cadastrado |
| `CATEGORIA_DUPLICADA` | 409 | Já existe categoria com esse nome |
| `REGISTRO_EM_USO` | 409 | Exclusão de registro com vínculos |
| `SALDO_INSUFICIENTE` | 409 | Quantidade maior que o saldo |
| `DOACAO_INDISPONIVEL` | 409 | Doação não está ativa ou o prazo venceu |
| `RESERVA_DUPLICADA` | 409 | O receptor já tem reserva ativa nessa doação |
| `ESTADO_INVALIDO` | 409 | Operação não permitida no status atual |
| `ERRO_INTERNO` | 500 | Falha não tratada |
| `IA_INDISPONIVEL` | 503 | O Gemini falhou ou passou do tempo limite |

Mensagens internas nunca são expostas ao cliente; os detalhes das falhas ficam apenas no log da
aplicação.

---

# Testes

## Postman

A coleção `docs/SobraBem.postman_collection.json` cobre os fluxos de ponta a ponta, com verificações
automáticas de status, formato de resposta e regras de negócio. Cada execução cria usuários próprios,
então pode rodar quantas vezes for preciso.

1. Importe a coleção no Postman
2. Preencha `adminEmail` e `adminSenha` nas variáveis da coleção
3. Use **Run collection**

Também roda pelo terminal:

```bash
npx newman run docs/SobraBem.postman_collection.json --env-var adminEmail=... --env-var adminSenha=...
```

Cenários cobertos:

- Cadastro e autenticação, incluindo credenciais inválidas e e-mail repetido
- Bloqueio de acesso entre perfis e entre usuários do mesmo perfil
- CRUD de categorias, doações e reservas
- Regras de prazo, saldo, reserva duplicada e estados inválidos
- Ciclo completo: publicar, reservar, cancelar, reservar de novo e retirar com o código
- Formato padronizado de erro em todos os casos

## Testes unitários

```bash
./mvnw test
```

---

# Deploy

A API está publicada no **Render**, a partir do `Dockerfile` do projeto, com o banco Oracle da FIAP e o
RabbitMQ no **CloudAMQP**.

**URL:** https://gs-sobrabem-java.onrender.com

---

# Declaração de uso de IA

Conforme as regras da Global Solution, declaramos as ferramentas de IA generativa usadas no
desenvolvimento e para quê:

| Ferramenta | Uso |
|---|---|
| Claude (Anthropic) | Apoio no planejamento da arquitetura e do contrato da API, revisão da documentação, explicação de erros e revisão de código |

Todo o código entregue foi revisado e compreendido pelos integrantes do grupo.

A IA **dentro do produto** (orientação de conservação com o Gemini, via Spring AI) é uma funcionalidade
da aplicação e está descrita em [Inteligência artificial](#inteligência-artificial).