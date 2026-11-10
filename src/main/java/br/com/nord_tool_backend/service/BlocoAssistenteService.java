package br.com.nord_tool_backend.service;

/** Bloco de notas v4: o que a Lugia sabe do Bloco de notas da pessoa no chat dos cadernos. */
public interface BlocoAssistenteService {

    /**
     * Texto para o prompt de sistema: a lista dos cadernos, planilhas e documentos da pessoa logada, os trechos que mais
     * combinam com a pergunta (o item aberto vem primeiro) e as ações que a tela sabe executar.
     *
     * @param tipoAberto CADERNO, PLANILHA ou DOCUMENTO aberto na tela (ou nulo)
     */
    String contexto(String pergunta, String tipoAberto, Long idAberto);
}
