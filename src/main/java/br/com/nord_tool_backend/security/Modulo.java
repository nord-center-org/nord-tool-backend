package br.com.nord_tool_backend.security;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * Módulos sob controle de acesso. O nome da constante é o {@code cd_modulo} gravado em
 * {@code perfil_permissao}/{@code usuario_permissao}; {@code *} (curinga) vale para todos.
 * <p>
 * Subacessos (ex.: {@link #CRONOGRAMA_EXCLUIR}) ficam dentro de um módulo: sem linha própria herdam o módulo;
 * com linha {@code NENHUM} ficam bloqueados; e nunca valem se o módulo não chega no nível mínimo do subacesso.
 */
public enum Modulo {
    VISTORIA,
    TERMO_REPROVA,
    CRONOGRAMA,
    /** Cronograma: excluir demandas. */
    CRONOGRAMA_EXCLUIR(CRONOGRAMA, Acao.ESCRITA),
    /** Cronograma: compartilhar demandas com outras pessoas. */
    CRONOGRAMA_COMPARTILHAR(CRONOGRAMA, Acao.ESCRITA),
    /** Cronograma: criar e alterar atividades fixas (rotina da semana) e desmarcar dias delas. */
    CRONOGRAMA_ROTINA(CRONOGRAMA, Acao.ESCRITA),
    /** Cronograma: criar links do relatório para IA (Gemini etc.). */
    CRONOGRAMA_RELATORIO_IA(CRONOGRAMA, Acao.LEITURA),
    CONTROLE_CHAVES,
    CAIXINHA,
    FINANCEIRO,
    CASAMENTO,
    CADASTROS,
    MEDICOES,
    RELATORIO_FOTOGRAFICO,
    MAPEAMENTOS,
    /** Escadinha 3D: torres da obra, apartamentos, lados A/B e ciclos de cada pavimento. */
    ESCADINHA,
    /** Lugia (chat multi-IA): LEITURA conversa, ESCRITA deixa a IA agir no NordTool, ADMIN libera mudanças de código. */
    LUGIA,
    /** SAC: qualquer pessoa logada abre chamados; ADMIN vê todos, responde e muda o status. */
    SAC,
    ADMINISTRACAO;

    public static final String CURINGA = "*";

    private final Modulo pai;
    private final Acao minimoDoPai;

    Modulo() {
        this(null, Acao.NENHUM);
    }

    Modulo(Modulo pai, Acao minimoDoPai) {
        this.pai = pai;
        this.minimoDoPai = minimoDoPai;
    }

    /** Módulo de que este subacesso faz parte (vazio para módulos de topo). */
    public Optional<Modulo> pai() {
        return Optional.ofNullable(pai);
    }

    /** Nível que o módulo pai precisa ter para o subacesso valer. */
    public Acao minimoDoPai() {
        return minimoDoPai;
    }

    public boolean subacesso() {
        return pai != null;
    }

    public static Optional<Modulo> de(String codigo) {
        if (codigo == null) return Optional.empty();
        String chave = codigo.trim();
        return Stream.of(values()).filter(m -> m.name().equalsIgnoreCase(chave)).findFirst();
    }
}
