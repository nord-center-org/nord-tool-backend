package br.com.nord_tool_backend.utils;

public class ApiResponseMessageUtils {
    public static final String RESPONSE_200 = StringUtils.getMensagem("Conteúdo retornado com sucesso");
    public static final String RESPONSE_201 = StringUtils.getMensagem("Conteúdo criado com sucesso");
    public static final String RESPONSE_204 = StringUtils.getMensagem("Conteúdo feito com sucesso, mas não retornou");
    public static final String RESPONSE_304 = StringUtils.getMensagem("Conteúdo não modificado");
    public static final String RESPONSE_400 = StringUtils.getMensagem("Erro na requsição");
    public static final String RESPONSE_401 = StringUtils.getMensagem("Erro de autorização");
    public static final String RESPONSE_404 = StringUtils.getMensagem("Conteúdo não encontrado");
    public static final String RESPONSE_409 = StringUtils.getMensagem("Conflito de versão");
    public static final String RESPONSE_500 = StringUtils.getMensagem("Erro nos servidores internos da aplicação");
}
