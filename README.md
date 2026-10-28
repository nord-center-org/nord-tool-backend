## **Tecnologia**
Java 11 · Spring Boot 2.7 · Spring JDBC (NamedParameterJdbcTemplate) · PostgreSQL · Spring Security (JWT) · Docker

## **Módulos da API** (prefixo `/api/v1/nord-tool`)
Apartamentos (vistorias, termos de reprova e fotos) · Controle de chaves · Cronograma semanal · Casamento · Caixinha · Financeiro · Autenticação.
A documentação interativa (Swagger) fica em `/swagger-ui.html` no profile `local`.

## **Rodando localmente**
Requisitos: JDK 11, Maven 3.9+ e um PostgreSQL. O `docker-compose.yml` sobe Postgres (em `127.0.0.1:8080`) e o backend; copie `.env.example` para `.env` (fora do git) e preencha a senha do banco e `NORD_JWT_KEYS` antes de `docker compose up`.

```bash
bash scripts/verificar-politica-testes.sh   # gate da política de testes e da convenção Service/ServiceImpl
mvn clean verify                            # compila e roda os testes
mvn spring-boot:run                         # profile "local" (porta 8081), chave JWT temporária
```

O profile padrão é `local` (`SPRING_PROFILES_ACTIVE`). A senha que aparece em `application-local.yaml` é só do banco de
desenvolvimento local; **nunca** use credenciais reais nesses arquivos.

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
| `NORD_JWT_KEYS` | chaves HS256 do JWT: `kid1:<base64 de 32+ bytes>;kid2:...`. **Obrigatória** fora do profile `local` (o startup falha sem ela) | — |
| `NORD_JWT_ACTIVE_KID` | `kid` que assina os tokens novos (obrigatória com mais de uma chave) | — |
| `NORD_JWT_SECRET` | compatibilidade: chave única em texto (≥ 32 bytes), usada só se `NORD_JWT_KEYS` estiver vazia | — |
| `NORD_ACCESS_TOKEN_MINUTES` | duração do token de acesso (o frontend renova enquanto há atividade) | `15` |
| `NORD_SESSION_MAX_HOURS` | limite absoluto da sessão desde o login | `2` |
| `NORD_CORS_ORIGINS` | origens do frontend, separadas por vírgula | `localhost:5173` (local/docker) · frontend develop (railway) |
| `NORD_ADMIN_EMAIL` `NORD_ADMIN_NAME` `NORD_ADMIN_PASSWORD` | cria um ADMIN com esse e-mail se ele ainda não existir (não altera usuário existente nem a senha) | — |
| `NORD_STORAGE_PROVIDER` | provedor de arquivos (`POSTGRES`, provisório) | `POSTGRES` |
| `nord-tool.caixinha.max-comprovante-bytes` | limite do PDF de comprovante da Caixinha | 5 MB |
| `NORD_COTACAO_TOKEN` | token da brapi.dev (plano gratuito) para a cotação dos fundos; sem ele só alguns tickers respondem. Nunca é exposto pela API | — |
| `NORD_COTACAO_URL` `NORD_COTACAO_TTL_SEGUNDOS` `NORD_COTACAO_TIMEOUT_SEGUNDOS` | provedor de cotação, cache (evita estourar o limite gratuito) e timeout | `https://brapi.dev/api` · `60` · `5` |
| `ANTHROPIC_API_KEY` `OPENAI_API_KEY` `GEMINI_API_KEY` | Lugia: chaves de Claude, ChatGPT e Gemini (cada provedor aparece com chave + modelo). A da Anthropic também liga o agente | — |
| `GROQ_API_KEY` `OPENROUTER_API_KEY` | Lugia, provedores gratuitos (só texto): Groq (rápido, sem cartão, console.groq.com) e OpenRouter (modelos com `:free`) | — |
| `LUGIA_MODELOS_GROQ` `LUGIA_MODELOS_OPENROUTER` | modelos desses provedores, separados por vírgula | `openai/gpt-oss-120b,llama-3.1-8b-instant` · — |
| `LUGIA_MODELOS_CLAUDE` `LUGIA_MODELOS_OPENAI` `LUGIA_MODELOS_GEMINI` | modelos no seletor, separados por vírgula (OpenAI sem padrão) | `claude-opus-5-5,claude-sonnet-5-5` · — · `gemini-flash-latest` |
| `LUGIA_GITHUB_TOKEN` | Lugia, modo Executar: token *fine-grained* do GitHub com **Contents** e **Pull requests** (leitura e escrita) nos repositórios da organização. Vai para o vault da Anthropic; nunca para o container do agente | — |
| `LUGIA_MODELO_AGENTE` `LUGIA_REPOSITORIOS` `LUGIA_ANEXO_MAX_MB` | modelo do agente, repositórios montados (`owner/repo,...`) e tamanho máximo de anexo | `claude-opus-5-5` · os 5 do NordTool · `10` |

### Pipeline e deploy no Railway
O `pipeline-dev.yml` roda a cada merge em `develop` (ou manualmente): Build & Testes (política de testes + `*ServiceImplTest`), Qualidade de Código (PMD e SpotBugs, bloqueante), Secret Scanning (Gitleaks), Análise de Vulnerabilidades (OSV) e Container Image (Trivy). O Resumo de Alertas junta tudo; OSV e Trivy só alertam. O job CD publica no Railway só se as etapas bloqueantes passarem. Para rodar a qualidade localmente: `mvn compile pmd:check spotbugs:check`. `master` ainda não publica pelo CI. No environment `develop` do GitHub (Settings → Environments, só a branch `develop` liberada):
1. Crie o secret `RAILWAY_TOKEN` com um **Project Token** do ambiente `develop` no Railway (Project Settings → Tokens).
2. Crie a variável `RAILWAY_SERVICE` com o nome do serviço do backend no Railway.
3. No serviço do Railway, **desligue o auto-deploy pelo GitHub** (Settings → Source); senão o Railway publica sem esperar o CI.

### Primeiro acesso (Railway)
1. Aplique os scripts do `nord-tool-scripts-sql` (colunas de sessão e `usuario_permissao`) antes de publicar o backend.
2. Defina `NORD_JWT_KEYS`/`NORD_JWT_ACTIVE_KID` e `NORD_ADMIN_*`, publique e confira no log "Usuário ADMIN de NORD_ADMIN_EMAIL criado".
3. Entre pelo frontend e confirme o login. Quando houver tela de cadastro de usuários, **remova `NORD_ADMIN_*`**.

### Rotação da chave JWT
1. Gere a nova chave (`openssl rand -base64 48`) e acrescente: `NORD_JWT_KEYS=k1:<antiga>;k2:<nova>`, mantendo `NORD_JWT_ACTIVE_KID=k1`; publique.
2. Troque `NORD_JWT_ACTIVE_KID=k2` e publique: tokens novos saem com `k2`, os antigos seguem válidos até expirar.
3. Depois do limite da sessão (`NORD_SESSION_MAX_HOURS`), remova `k1`. Em incidente, remova a chave antiga já no passo 1 (todas as sessões caem).

## **Lugia (chat multi-IA e agente)**
`/api/v1/nord-tool/lugia`: chat com Claude (SDK oficial da Anthropic), ChatGPT, Gemini, Groq e OpenRouter (HTTP; erros 429/5xx repetem até duas vezes antes de responder), com histórico em `lugia_conversa`/`lugia_mensagem` e anexos (imagem e PDF). `POST /lugia/chat` responde em Server-Sent Events (`conversa`, `texto`, `ferramenta`, `fim`, `erro`), escrevendo direto na resposta da própria requisição. Com `modo: "agente"` (exige `ADMIN` no módulo `LUGIA`) o pedido vai para um agente Claude (Managed Agents) com os repositórios montados: ele segue o `nord-center-infra-agent`, abre PR para `develop` e faz o merge quando o CI passa. Agente, ambiente e vault são criados no primeiro uso e os ids ficam em `lugia_configuracao`; mudar o prompt do agente cria uma nova versão.

**Acesso**: módulo `LUGIA` (`LEITURA` conversa; `ADMIN` também executa).

## **Financeiro**
`/api/v1/nord-tool/financeiro`: lançamentos (extrato com filtros por mês, período, pessoa, categoria, tipo, situação e texto; criação idempotente por `cdRequisicao`, parcelamento mensal com `qtParcelas`, controle de versão por `nrVersao`), pessoas (de quem é o lançamento) e categorias. A **conta do mês** (`GET /financeiro/mes/{yyyy-MM}`) substitui a planilha de fechamento: saldo anterior (do mês anterior fechado), entradas e saídas reais ou projetadas por categoria (média dos meses anteriores, valor fixo das recorrências ou ritmo de gasto da fatura do cartão; a fatura "do mês M" é a das compras de M: o ciclo vai do dia seguinte ao fechamento de M até o fechamento de M+1, então a fatura de setembro fecha em outubro e vence em 10/10), saldo final e a folga em relação à meta de saldo (verde/vermelho). `POST /mes/{yyyy-MM}/fechar` grava o saldo real, trava os lançamentos do mês e gera os fixos do mês seguinte; `/reabrir` desfaz (só o último mês fechado). Recorrências em `/recorrencias` e parâmetros (meta, meses da média, dia de fechamento da fatura) em `/configuracao`. Respostas saem com `Cache-Control: no-store`.
**Investimentos** (`/financeiro/investimentos`): fundos imobiliários por pessoa, compras/vendas (preço médio; venda acima das cotas é recusada; idempotente por `cdRequisicao`) e proventos (valor a receber = cotas na data-com × valor por cota; manual ou importado do provedor sem sobrescrever o digitado). A cotação vem do `CotacaoProvider` (brapi.dev) com cache de 60 s; se o provedor não responder vale a última cotação guardada e a API devolve `cotacaoAoVivo=false`.

**Acesso**: exige o módulo `FINANCEIRO` (ver Segurança > Permissões).
O histórico da planilha entra pelos scripts de `MIGRACAO/financeiro/` (repositório `nord-tool-scripts-sql`).

## **Segurança**
Toda rota exige `Authorization: Bearer <token>`; só `POST /auth/login` e `GET /nord-tool/health` são públicos. O token dura
`NORD_ACCESS_TOKEN_MINUTES` e é renovado em `POST /auth/refresh` até `NORD_SESSION_MAX_HOURS` desde o login. A cada requisição o
backend confere se o usuário segue ativo e se a versão da sessão do token é a atual (cache de 60 s): a troca de senha encerra as
outras sessões. Login: mesma resposta para qualquer recusa, bloqueio progressivo (1 a 15 min) a partir da 5ª falha e limite de
10 tentativas/min por IP e 5 por e-mail (429).

As guardas `guard/RotasProtegidasGuardTest` (varre todas as rotas e exige 401 sem token) e `guard/ContratoSegurancaGuardTest`
(401/403, sessão revogada, corpo de erro) são a única exceção da política de testes. Respostas de dados pessoais/financeiros
(Casamento, Caixinha, Financeiro, contratos e comprovantes) saem com `Cache-Control: no-store`.

### Permissões
Cada serviço exige `módulo:nível` na primeira linha (`AutorizacaoService`). Módulos: `VISTORIA`, `TERMO_REPROVA`, `CRONOGRAMA`,
`CONTROLE_CHAVES`, `CAIXINHA`, `FINANCEIRO`, `CASAMENTO`, `CADASTROS` (cargos, empresas, colaboradores, ferramentas — a leitura
só exige login) e `ADMINISTRACAO` (cache). Níveis hierárquicos: `NENHUM < LEITURA < ESCRITA < ADMIN`. A permissão efetiva vem de
`usuario_permissao` (exceções do usuário) e `perfil_permissao`, do mais específico para o menos: usuário+módulo, perfil+módulo,
usuário+`*`, perfil+`*`. `NENHUM` nega o módulo mesmo com o `*` do perfil. Exemplos:
```sql
-- liberar o Casamento só para um usuário
INSERT INTO usuario_permissao (id_usuario, cd_modulo, cd_acao)
SELECT id_usuario, 'CASAMENTO', 'ESCRITA' FROM usuario WHERE LOWER(nm_email) = LOWER('<email>')
ON CONFLICT (id_usuario, cd_modulo) DO UPDATE SET cd_acao = EXCLUDED.cd_acao;

-- dar a um perfil leitura no Financeiro
INSERT INTO perfil_permissao (id_perfil, cd_modulo, cd_acao)
SELECT id_perfil, 'FINANCEIRO', 'LEITURA' FROM perfil WHERE cd_perfil = '<PERFIL>'
ON CONFLICT (id_perfil, cd_modulo) DO UPDATE SET cd_acao = EXCLUDED.cd_acao;
```
Mudanças de permissão valem no próximo login ou renovação do token (até `NORD_ACCESS_TOKEN_MINUTES`).

## **Armazenamento de arquivos**
Provisório em Postgres (`arquivo_armazenado`, BYTEA) atrás da interface `ArmazenamentoService` (implementação atual: `ArmazenamentoPostgresServiceImpl`); o provedor definitivo
(Drive/S3) ainda está por decidir e entrará sem alterar controllers. Limites: PDF de termo 15 MB / 80 páginas, imagens 5 MB,
contratos do casamento 15 MB, comprovantes da Caixinha 5 MB (multipart: 16 MB por arquivo). Cotas: 320 fotos por termo, 20 anexos
por fornecedor e 10 comprovantes por lançamento. A JVM usa 75% da memória do container (`MaxRAMPercentage`, ver `Dockerfile`).

## **Outros plugins recomendados**
SonarQube · Spring · Docker · Maven Helper
