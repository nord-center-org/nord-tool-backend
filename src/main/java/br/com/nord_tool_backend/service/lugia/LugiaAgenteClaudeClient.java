package br.com.nord_tool_backend.service.lugia;

import br.com.nord_tool_backend.dto.LugiaParteDto;
import br.com.nord_tool_backend.repository.LugiaRepository;
import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.core.http.StreamResponse;
import com.anthropic.errors.AnthropicIoException;
import com.anthropic.errors.AnthropicServiceException;
import com.anthropic.errors.RateLimitException;
import com.anthropic.errors.UnauthorizedException;
import com.anthropic.models.beta.agents.AgentCreateParams;
import com.anthropic.models.beta.agents.AgentUpdateParams;
import com.anthropic.models.beta.agents.BetaManagedAgentsAgent;
import com.anthropic.models.beta.agents.BetaManagedAgentsAgentToolset20260401Params;
import com.anthropic.models.beta.agents.BetaManagedAgentsAgentToolsetDefaultConfigParams;
import com.anthropic.models.beta.agents.BetaManagedAgentsAlwaysAllowPolicy;
import com.anthropic.models.beta.agents.BetaManagedAgentsMcpToolsetDefaultConfigParams;
import com.anthropic.models.beta.agents.BetaManagedAgentsMcpToolsetParams;
import com.anthropic.models.beta.agents.BetaManagedAgentsModel;
import com.anthropic.models.beta.agents.BetaManagedAgentsUrlMcpServerParams;
import com.anthropic.models.beta.agents.BetaManagedAgentsWebFetchToolConfigParams;
import com.anthropic.models.beta.agents.BetaManagedAgentsWebSearchToolConfigParams;
import com.anthropic.models.beta.environments.BetaCloudConfigParams;
import com.anthropic.models.beta.environments.BetaLimitedNetworkParams;
import com.anthropic.models.beta.environments.EnvironmentCreateParams;
import com.anthropic.models.beta.sessions.BetaManagedAgentsGitHubRepositoryResourceParams;
import com.anthropic.models.beta.sessions.SessionCreateParams;
import com.anthropic.models.beta.sessions.events.BetaManagedAgentsAgentEvaluatedPermission;
import com.anthropic.models.beta.sessions.events.BetaManagedAgentsBase64ImageSource;
import com.anthropic.models.beta.sessions.events.BetaManagedAgentsStreamSessionEvents;
import com.anthropic.models.beta.sessions.events.BetaManagedAgentsUserMessageEventParams;
import com.anthropic.models.beta.sessions.events.BetaManagedAgentsUserToolConfirmationEventParams;
import com.anthropic.models.beta.sessions.events.EventSendParams;
import com.anthropic.models.beta.vaults.VaultCreateParams;
import com.anthropic.models.beta.vaults.credentials.BetaManagedAgentsStaticBearerCreateParams;
import com.anthropic.models.beta.vaults.credentials.CredentialCreateParams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

/**
 * Agente executor da Lugia (Claude Managed Agents). O agente, o ambiente e o vault com a credencial do GitHub
 * são criados uma vez e os ids ficam em {@code lugia_configuracao} (ou vêm de LUGIA_AGENTE_ID etc.); quando a
 * definição do agente muda, ele é atualizado (nova versão), nunca recriado. Cada conversa abre uma sessão com
 * os repositórios do NordTool montados em /workspace.
 */
@Component @Slf4j
public class LugiaAgenteClaudeClient implements LugiaAgenteClient {

    static final String MCP_GITHUB = "https://api.githubcopilot.com/mcp/";
    static final String CHAVE_AGENTE = "agente.id";
    static final String CHAVE_ASSINATURA = "agente.assinatura";
    static final String CHAVE_AMBIENTE = "agente.ambiente";
    static final String CHAVE_VAULT = "agente.vault";
    static final String CHAVE_VAULT_TOKEN = "agente.vault.token";
    private static final int MAX_RECONEXOES = 3;

    static final String SISTEMA = String.join("\n",
            "Você é a Lugia, agente executor do NordTool. Pedidos chegam do chat do próprio app, feitos pelo dono do sistema.",
            "Responda sempre em português do Brasil.",
            "",
            "Os repositórios do NordTool estão em /workspace/<nome-do-repositório>:",
            "nord-tool-frontend (React/TypeScript), nord-tool-backend (Java 11/Spring Boot), nord-tool-scripts-sql (PostgreSQL),",
            "nord-center-infra-agent (regras para agentes) e lugia-center.",
            "",
            "Antes de alterar qualquer repositório, leia nord-center-infra-agent/AGENTS.md, todas as policies/, o perfil em projects/,",
            "o steering/ e as skills/ do projeto alvo, e o AGENTS.md do próprio repositório. Siga essas regras à risca:",
            "- trabalhe numa branch nova criada a partir de develop atualizado (ex.: lugia/<assunto>); nunca escreva em develop, master ou main;",
            "- rode as validações que o steering do projeto pede (lint, testes, build) antes de publicar;",
            "- publique só a sua branch e abra o PR para develop pela ferramenta do GitHub (MCP);",
            "- acompanhe o CI do PR; quando estiver verde e mergeable, faça o squash merge, como a policy autoriza; se falhar, corrija e publique de novo;",
            "- toda mudança visível no app acrescenta uma novidade em nord-tool-frontend/src/react-app/utils/novidades.ts (policies/novidades.md);",
            "- nunca exponha segredos, tokens ou dados pessoais.",
            "",
            "Imagens anexadas costumam ser prints de telas do app mostrando o que deve mudar; use-as para localizar a tela e o componente.",
            "Se o pedido for só uma pergunta, responda lendo o código, sem abrir PR. Se faltar informação essencial, pergunte antes de mudar.",
            "Ao terminar, responda com um resumo curto do que fez, os links dos PRs e onde ver a mudança no app.");

    private final LugiaProperties props;
    private final LugiaRepository repository;
    private volatile AnthropicClient client;

    public LugiaAgenteClaudeClient(LugiaProperties props, LugiaRepository repository) {
        this.props = props;
        this.repository = repository;
    }

    @Override
    public boolean disponivel() {
        return !props.getAnthropicApiKey().isEmpty() && !props.getGithubToken().isEmpty();
    }

    private AnthropicClient client() {
        if (client == null) {
            synchronized (this) {
                if (client == null) client = AnthropicOkHttpClient.builder().apiKey(props.getAnthropicApiKey()).build();
            }
        }
        return client;
    }

    @Override
    public String criarSessao(String titulo) {
        try {
            Recursos r = recursos();
            SessionCreateParams.Builder sessao = SessionCreateParams.builder()
                    .agent(r.agente)
                    .environmentId(r.ambiente)
                    .addVaultId(r.vault)
                    .title(titulo.length() > 100 ? titulo.substring(0, 100) : titulo);
            for (String repo : props.getRepositorios()) {
                String nome = repo.substring(repo.indexOf('/') + 1);
                sessao.addResource(BetaManagedAgentsGitHubRepositoryResourceParams.builder()
                        .url("https://github.com/" + repo)
                        .mountPath("/workspace/" + nome)
                        .authorizationToken(props.getGithubToken())
                        .build());
            }
            return client().beta().sessions().create(sessao.build()).id();
        } catch (RuntimeException e) {
            throw traduzir(e);
        }
    }

    @Override
    public LugiaResposta executar(String idSessao, List<LugiaParteDto> partes, LugiaSaida saida) {
        StringBuilder texto = new StringBuilder();
        List<String> aguardando = new ArrayList<>();
        try {
            boolean enviado = false;
            for (int tentativa = 0; tentativa <= MAX_RECONEXOES; tentativa++) {
                // Abre o stream antes de enviar: só chegam eventos posteriores à abertura.
                try (StreamResponse<BetaManagedAgentsStreamSessionEvents> stream =
                             client().beta().sessions().events().streamStreaming(idSessao)) {
                    if (!enviado) {
                        client().beta().sessions().events().send(idSessao, EventSendParams.builder().addEvent(mensagem(partes)).build());
                        enviado = true;
                    }
                    Iterator<BetaManagedAgentsStreamSessionEvents> eventos = stream.stream().iterator();
                    while (eventos.hasNext()) {
                        String fim = tratar(idSessao, eventos.next(), texto, aguardando, saida);
                        if (fim != null) return new LugiaResposta(texto.toString(), null, fim, null, null);
                    }
                }
                // Stream caiu sem a sessão terminar: confere o estado antes de reabrir.
                if (ociosa(idSessao)) return new LugiaResposta(texto.toString(), null, "end_turn", null, null);
            }
            throw new LugiaFalhaException("Perdi a conexão com o agente. Abra a conversa de novo em instantes para ver o resultado.");
        } catch (LugiaFalhaException e) {
            throw e;
        } catch (RuntimeException e) {
            throw traduzir(e);
        }
    }

    /** Processa um evento; devolve o motivo de término quando a sessão para, ou null para continuar. */
    private String tratar(String idSessao, BetaManagedAgentsStreamSessionEvents ev, StringBuilder texto,
                          List<String> aguardando, LugiaSaida saida) {
        if (ev.isAgentMessage()) {
            ev.asAgentMessage().content().forEach(bloco -> bloco.text().ifPresent(t -> {
                String trecho = (texto.length() > 0 ? "\n\n" : "") + t.text();
                texto.append(trecho);
                saida.texto(trecho);
            }));
        } else if (ev.isAgentToolUse()) {
            var uso = ev.asAgentToolUse();
            saida.ferramenta(LugiaAgenteDescricao.ferramenta(uso.name(), uso.input()._additionalProperties()));
            if (pedePermissao(uso.evaluatedPermission())) aguardando.add(uso.id());
        } else if (ev.isAgentMcpToolUse()) {
            var uso = ev.asAgentMcpToolUse();
            saida.ferramenta(LugiaAgenteDescricao.ferramenta(uso.mcpServerName() + "." + uso.name(), uso.input()._additionalProperties()));
            if (pedePermissao(uso.evaluatedPermission())) aguardando.add(uso.id());
        } else if (ev.isSessionError()) {
            log.warn("Sessão do agente com erro: {}", ev.asSessionError().error().message());
            saida.ferramenta("⚠️ " + ev.asSessionError().error().message());
        } else if (ev.isSessionStatusTerminated()) {
            return "terminated";
        } else if (ev.isSessionStatusIdle()) {
            var motivo = ev.asSessionStatusIdle().stopReason();
            if (!motivo.isRequiresAction()) return motivo.isEndTurn() ? "end_turn" : String.valueOf(motivo._json().orElse(null));
            // Política always_allow: não deveria pausar. Se pausar, nega (ninguém aprova pelo app ainda).
            for (String id : aguardando) {
                client().beta().sessions().events().send(idSessao, EventSendParams.builder()
                        .addEvent(BetaManagedAgentsUserToolConfirmationEventParams.builder()
                                .toolUseId(id)
                                .result(BetaManagedAgentsUserToolConfirmationEventParams.Result.DENY)
                                .build())
                        .build());
            }
            aguardando.clear();
        }
        return null;
    }

    private static boolean pedePermissao(Optional<BetaManagedAgentsAgentEvaluatedPermission> avaliada) {
        return avaliada.equals(Optional.of(BetaManagedAgentsAgentEvaluatedPermission.ASK));
    }

    private boolean ociosa(String idSessao) {
        String status = String.valueOf(client().beta().sessions().retrieve(idSessao).status()).toLowerCase();
        return status.contains("idle") || status.contains("terminated");
    }

    private static BetaManagedAgentsUserMessageEventParams mensagem(List<LugiaParteDto> partes) {
        BetaManagedAgentsUserMessageEventParams.Builder m = BetaManagedAgentsUserMessageEventParams.builder();
        StringBuilder texto = new StringBuilder();
        for (LugiaParteDto p : partes) {
            if (LugiaParteDto.IMAGEM.equals(p.getTipo())) {
                m.addImageContent(BetaManagedAgentsBase64ImageSource.builder().mediaType(p.getMime()).data(p.getBase64()).build());
            } else if (LugiaParteDto.PDF.equals(p.getTipo())) {
                texto.append("[PDF anexado no chat: ").append(p.getNome() == null ? "documento.pdf" : p.getNome())
                        .append(" (o agente não recebe PDFs; peça um print da parte importante)]\n");
            } else if (p.getTexto() != null) {
                texto.append(p.getTexto());
            }
        }
        m.addTextContent(texto.length() == 0 ? "Veja a imagem anexada." : texto.toString());
        return m.build();
    }

    /** Ids do agente, do ambiente e do vault: configurados, guardados no banco ou criados agora (uma vez). */
    private synchronized Recursos recursos() {
        String agente = props.getAgenteId().isEmpty() ? garantirAgente() : props.getAgenteId();
        String ambiente = props.getAmbienteId().isEmpty()
                ? repository.lerConfiguracao(CHAVE_AMBIENTE).orElseGet(this::criarAmbiente) : props.getAmbienteId();
        String vault = props.getVaultId().isEmpty() ? garantirVault() : props.getVaultId();
        return new Recursos(agente, ambiente, vault);
    }

    private String garantirAgente() {
        String assinatura = hash(props.getModeloAgente() + "\n" + SISTEMA);
        Optional<String> guardado = repository.lerConfiguracao(CHAVE_AGENTE);
        if (guardado.isPresent()) {
            if (!assinatura.equals(repository.lerConfiguracao(CHAVE_ASSINATURA).orElse(""))) {
                BetaManagedAgentsAgent atual = client().beta().agents().retrieve(guardado.get());
                client().beta().agents().update(guardado.get(), AgentUpdateParams.builder()
                        .version(atual.version())
                        .model(BetaManagedAgentsModel.of(props.getModeloAgente()))
                        .system(SISTEMA)
                        .build());
                repository.gravarConfiguracao(CHAVE_ASSINATURA, assinatura);
                log.info("Agente da Lugia atualizado para nova versão");
            }
            return guardado.get();
        }
        BetaManagedAgentsAgent criado = client().beta().agents().create(AgentCreateParams.builder()
                .name("Lugia · executor do NordTool")
                .description("Executa pedidos do chat da Lugia nos repositórios do NordTool (PR para develop).")
                .model(BetaManagedAgentsModel.of(props.getModeloAgente()))
                .system(SISTEMA)
                // Ferramentas rodam no container isolado da sessão; a web fica desligada.
                .addTool(BetaManagedAgentsAgentToolset20260401Params.builder()
                        .defaultConfig(BetaManagedAgentsAgentToolsetDefaultConfigParams.builder()
                                .permissionPolicy(BetaManagedAgentsAlwaysAllowPolicy.builder().build())
                                .build())
                        .addConfig(BetaManagedAgentsWebFetchToolConfigParams.builder().enabled(false).build())
                        .addConfig(BetaManagedAgentsWebSearchToolConfigParams.builder().enabled(false).build())
                        .build())
                .addMcpServer(BetaManagedAgentsUrlMcpServerParams.builder()
                        .name("github").url(MCP_GITHUB).build())
                .addTool(BetaManagedAgentsMcpToolsetParams.builder()
                        .mcpServerName("github")
                        .defaultConfig(BetaManagedAgentsMcpToolsetDefaultConfigParams.builder()
                                .permissionPolicy(BetaManagedAgentsAlwaysAllowPolicy.builder().build())
                                .build())
                        .build())
                .build());
        repository.gravarConfiguracao(CHAVE_AGENTE, criado.id());
        repository.gravarConfiguracao(CHAVE_ASSINATURA, assinatura);
        log.info("Agente da Lugia criado: {}", criado.id());
        return criado.id();
    }

    private String criarAmbiente() {
        String id = client().beta().environments().create(EnvironmentCreateParams.builder()
                .name("lugia-nordtool")
                .config(BetaCloudConfigParams.builder()
                        .networking(BetaLimitedNetworkParams.builder()
                                .allowPackageManagers(true)
                                .allowMcpServers(true)
                                .addAllowedHost("repo.maven.apache.org")
                                .addAllowedHost("repo1.maven.org")
                                .build())
                        .build())
                .build()).id();
        repository.gravarConfiguracao(CHAVE_AMBIENTE, id);
        log.info("Ambiente do agente da Lugia criado: {}", id);
        return id;
    }

    /** Vault com o token do GitHub para o MCP; recriado se o token mudar (o segredo nunca volta da API). */
    private String garantirVault() {
        String marca = hash(props.getGithubToken());
        Optional<String> guardado = repository.lerConfiguracao(CHAVE_VAULT);
        if (guardado.isPresent() && marca.equals(repository.lerConfiguracao(CHAVE_VAULT_TOKEN).orElse(""))) {
            return guardado.get();
        }
        String vault = client().beta().vaults().create(VaultCreateParams.builder().displayName("Lugia · GitHub NordTool").build()).id();
        client().beta().vaults().credentials().create(vault, CredentialCreateParams.builder()
                .displayName("GitHub (MCP)")
                .auth(BetaManagedAgentsStaticBearerCreateParams.builder()
                        .mcpServerUrl(MCP_GITHUB)
                        .token(props.getGithubToken())
                        .build())
                .build());
        repository.gravarConfiguracao(CHAVE_VAULT, vault);
        repository.gravarConfiguracao(CHAVE_VAULT_TOKEN, marca);
        log.info("Vault do agente da Lugia criado: {}", vault);
        return vault;
    }

    static String hash(String valor) {
        try {
            byte[] h = MessageDigest.getInstance("SHA-256").digest(valor.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : h) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static LugiaFalhaException traduzir(RuntimeException e) {
        log.warn("Agente da Lugia falhou: {}", e.getMessage());
        if (e instanceof RateLimitException) return new LugiaFalhaException("Limite de uso do agente atingido; tente em instantes.", e);
        if (e instanceof UnauthorizedException) return new LugiaFalhaException("Chave da Anthropic inválida no servidor.", e);
        if (e instanceof AnthropicServiceException) {
            return new LugiaFalhaException("O agente não pôde atender (" + ((AnthropicServiceException) e).statusCode() + ").", e);
        }
        if (e instanceof AnthropicIoException) return new LugiaFalhaException("Não foi possível falar com o agente agora.", e);
        return new LugiaFalhaException("Falha inesperada no agente.", e);
    }

    private static final class Recursos {
        final String agente;
        final String ambiente;
        final String vault;

        Recursos(String agente, String ambiente, String vault) {
            this.agente = agente;
            this.ambiente = ambiente;
            this.vault = vault;
        }
    }
}
