package br.com.nord_tool_backend.service;

import br.com.nord_tool_backend.dto.IntegracaoTokenDto;
import br.com.nord_tool_backend.dto.RelatorioCronogramaDto;
import br.com.nord_tool_backend.form.IntegracaoTokenForm;

import java.util.List;

/**
 * Links de integração: o usuário cria uma chave de leitura do próprio cronograma para um assistente de IA
 * (Gemini, ChatGPT…), que lê o relatório pela URL pública sem precisar de login.
 */
public interface IntegracaoService {
    List<IntegracaoTokenDto> listarTokens();

    IntegracaoTokenDto criarToken(IntegracaoTokenForm form);

    void revogarToken(Long idToken);

    /** Relatório do cronograma do dono da chave; chave inválida, revogada ou sem permissão → 404 genérico. */
    RelatorioCronogramaDto relatorioCronograma(String chave, int dias);

    /** O mesmo relatório em texto simples, fácil para o assistente ler e resumir. */
    String relatorioCronogramaTexto(String chave, int dias);
}
