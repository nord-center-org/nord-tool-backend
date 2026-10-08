## **Tecnologia**
Java 11 · Spring Boot 2.7 · Spring JDBC (NamedParameterJdbcTemplate) · PostgreSQL · Spring Security (JWT) · Docker

## **Módulos da API** (prefixo `/api/v1/nord-tool`)
Apartamentos (vistorias, termos de reprova e fotos) · Controle de chaves · Cronograma semanal · Casamento · Caixinha · Financeiro · Autenticação.
A documentação interativa (Swagger) fica em `/swagger-ui.html` no profile `local`.

## **Rodando localmente**
Requisitos: JDK 11, Maven 3.9+ e um PostgreSQL (o `docker-compose.yml` sobe um na porta 8080 com o banco `apartamento`).

```bash
mvn clean verify          # compila e roda os testes
mvn spring-boot:run       # profile "local" (porta 8081)
```

O profile padrão é `local` (`SPRING_PROFILES_ACTIVE`). A senha que aparece em `application-local.yaml`/`docker-compose.yml`
é só do banco de desenvolvimento local; **nunca** use credenciais reais nesses arquivos.

## **Banco de dados (repositório `nord-tool-scripts-sql`)**
O esquema vive no repositório de scripts, em `DDL/NN.*_ddl.sql` e `DML/NN.*_dml.sql`, na ordem do `filelist.txt`
(apartamentos → controle de chaves → login/arquivos/termos de reprova → casamento → limite de 80 páginas → Caixinha → Financeiro).
Os scripts são idempotentes e a pipeline "SQL Dev" aplica tudo no `develop`. **Aplique os scripts antes de publicar o backend
que depende deles.** Migrações de dados do Lugia ficam em `MIGRACAO/` (manuais, fora do `filelist.txt`).

## **Variáveis de ambiente**
| Variável | Para quê | Padrão |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `local`, `docker` ou `railway` | `local` |
| `PGHOST` `PGPORT` `PGDATABASE` `PGUSER` `PGPASSWORD` | conexão do Postgres (profile `railway`) | — |
| `PORT` | porta HTTP (Railway) | `8080` |
| `NORD_SECURITY_ENABLED` | liga a exigência de login (JWT). `false` deixa a API aberta (usar só na transição) | `false` (`true` no profile local) |
| `NORD_JWT_SECRET` | segredo HS256 do JWT (≥ 32 bytes). **Obrigatório** com a segurança ligada | — |
| `NORD_INACTIVITY_MINUTES` | expiração por inatividade do token | `30` |
| `NORD_ADMIN_EMAIL` `NORD_ADMIN_NAME` `NORD_ADMIN_PASSWORD` | cria o primeiro ADMIN **somente** com a tabela `usuario` vazia | — |
| `NORD_STORAGE_PROVIDER` | provedor de arquivos (`POSTGRES`, provisório) | `POSTGRES` |
| `nord-tool.caixinha.max-comprovante-bytes` | limite do PDF de comprovante da Caixinha | 5 MB |

### Primeiro acesso em produção (Railway)
1. Defina `NORD_JWT_SECRET` e `NORD_ADMIN_*`, publique e confira no log "Usuário ADMIN inicial criado".
2. Entre pelo frontend e confirme o login; então defina `NORD_SECURITY_ENABLED=true`.
3. **Remova `NORD_ADMIN_PASSWORD`** (e as demais `NORD_ADMIN_*`) depois do primeiro login.

## **Financeiro**
`/api/v1/nord-tool/financeiro`: lançamentos (extrato com filtros por mês, período, pessoa, categoria, tipo, situação e texto; criação idempotente por `cdRequisicao`, parcelamento mensal com `qtParcelas`, controle de versão por `nrVersao`), pessoas (de quem é o lançamento) e categorias. Respostas saem com `Cache-Control: no-store`.
Os testes de repositório contra o banco real (`FinanceiroRepositoryImplDbTest`) só rodam com `NORD_TEST_DATABASE_URL` (ex.: `jdbc:postgresql://localhost:5432/nordtest?currentSchema=nord_tool`, usuário/senha em `NORD_TEST_DATABASE_USER`/`NORD_TEST_DATABASE_PASSWORD`) apontando para um banco **de teste** com os scripts aplicados; nunca use produção.

## **Segurança**
Com `NORD_SECURITY_ENABLED=true` toda rota exige `Authorization: Bearer <token>`; só `POST /auth/login` e `GET /nord-tool/health`
são públicos. O teste `VarreduraRotasSemTokenTest` lê por reflexão todas as rotas dos controllers (inclusive arquivos, fotos e PDFs)
e falha se alguma responder sem token, então rotas novas entram na checagem automaticamente.
Respostas de dados pessoais/financeiros (Casamento, Caixinha, contratos e comprovantes) saem com `Cache-Control: no-store`.

## **Armazenamento de arquivos**
Provisório em Postgres (`arquivo_armazenado`, BYTEA) atrás da interface `ArmazenamentoArquivoService`; o provedor definitivo
(Drive/S3) ainda está por decidir e entrará sem alterar controllers. Limites: PDF de termo 15 MB / 80 páginas, imagens 5 MB,
contratos do casamento 15 MB, comprovantes da Caixinha 5 MB. A JVM roda com `-Xmx512m` (ver `Dockerfile`).

## **Outros plugins recomendados**
SonarQube · Spring · Docker · Maven Helper
