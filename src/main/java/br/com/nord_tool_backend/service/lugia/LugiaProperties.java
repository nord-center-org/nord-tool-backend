package br.com.nord_tool_backend.service.lugia;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Configuração da Lugia ({@code nord-tool.lugia.*}). As chaves vêm só do ambiente (ANTHROPIC_API_KEY,
 * OPENAI_API_KEY, GEMINI_API_KEY, LUGIA_GITHUB_TOKEN) e nunca vão para log ou resposta.
 */
@Component
public class LugiaProperties {

    private final String anthropicApiKey;
    private final String openaiApiKey;
    private final String geminiApiKey;
    private final List<String> modelosClaude;
    private final List<String> modelosOpenai;
    private final List<String> modelosGemini;
    private final long anexoMaxBytes;
    private final String githubToken;
    private final List<String> repositorios;
    private final String modeloAgente;
    private final String agenteId;
    private final String ambienteId;
    private final String vaultId;

    public LugiaProperties(
            @Value("${nord-tool.lugia.anthropic-api-key:}") String anthropicApiKey,
            @Value("${nord-tool.lugia.openai-api-key:}") String openaiApiKey,
            @Value("${nord-tool.lugia.gemini-api-key:}") String geminiApiKey,
            @Value("${nord-tool.lugia.modelos-claude:claude-opus-5-5,claude-sonnet-5-5}") String modelosClaude,
            @Value("${nord-tool.lugia.modelos-openai:}") String modelosOpenai,
            @Value("${nord-tool.lugia.modelos-gemini:gemini-flash-latest}") String modelosGemini,
            @Value("${nord-tool.lugia.anexo-max-mb:10}") long anexoMaxMb,
            @Value("${nord-tool.lugia.github-token:}") String githubToken,
            @Value("${nord-tool.lugia.repositorios:}") String repositorios,
            @Value("${nord-tool.lugia.modelo-agente:claude-opus-5-5}") String modeloAgente,
            @Value("${nord-tool.lugia.agente-id:}") String agenteId,
            @Value("${nord-tool.lugia.ambiente-id:}") String ambienteId,
            @Value("${nord-tool.lugia.vault-id:}") String vaultId) {
        this.anthropicApiKey = limpo(anthropicApiKey);
        this.openaiApiKey = limpo(openaiApiKey);
        this.geminiApiKey = limpo(geminiApiKey);
        this.modelosClaude = lista(modelosClaude);
        this.modelosOpenai = lista(modelosOpenai);
        this.modelosGemini = lista(modelosGemini);
        this.anexoMaxBytes = anexoMaxMb * 1024 * 1024;
        this.githubToken = limpo(githubToken);
        List<String> repos = lista(repositorios);
        this.repositorios = repos.isEmpty() ? REPOSITORIOS_PADRAO : repos;
        this.modeloAgente = limpo(modeloAgente);
        this.agenteId = limpo(agenteId);
        this.ambienteId = limpo(ambienteId);
        this.vaultId = limpo(vaultId);
    }

    /** Repositórios montados na sessão do agente (owner/repo). */
    public static final List<String> REPOSITORIOS_PADRAO = Collections.unmodifiableList(Arrays.asList(
            "nord-center-org/nord-tool-frontend",
            "nord-center-org/nord-tool-backend",
            "nord-center-org/nord-tool-scripts-sql",
            "nord-center-org/nord-center-infra-agent",
            "nord-center-org/lugia-center"));

    private static String limpo(String valor) {
        return valor == null ? "" : valor.trim();
    }

    private static List<String> lista(String valor) {
        if (valor == null || valor.trim().isEmpty()) return new ArrayList<>();
        return Arrays.stream(valor.split(",")).map(String::trim).filter(v -> !v.isEmpty()).collect(Collectors.toList());
    }

    public String getAnthropicApiKey() { return anthropicApiKey; }
    public String getOpenaiApiKey() { return openaiApiKey; }
    public String getGeminiApiKey() { return geminiApiKey; }
    public List<String> getModelosClaude() { return modelosClaude; }
    public List<String> getModelosOpenai() { return modelosOpenai; }
    public List<String> getModelosGemini() { return modelosGemini; }
    public long getAnexoMaxBytes() { return anexoMaxBytes; }
    public String getGithubToken() { return githubToken; }
    public List<String> getRepositorios() { return repositorios; }
    public String getModeloAgente() { return modeloAgente; }
    public String getAgenteId() { return agenteId; }
    public String getAmbienteId() { return ambienteId; }
    public String getVaultId() { return vaultId; }
}
