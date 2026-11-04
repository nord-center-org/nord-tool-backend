package br.com.nord_tool_backend.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Chamado do SAC: o pedido de mudança no sistema, com os prints e a resposta dos administradores. */
@Data
public class SacChamadoDto {
    private Long id;
    private Long idUsuario;
    private String nmUsuario;
    private String nmTitulo;
    private String txDescricao;
    /** Tela de onde saiu o primeiro print (ex.: /gestao/financeiro). */
    private String txTela;
    /** ABERTO, EM_ANDAMENTO, RESOLVIDO ou RECUSADO. */
    private String cdStatus;
    private String txResposta;
    private String nmUsuarioResposta;
    /** Resposta ou status novo que quem abriu ainda não viu. */
    private Boolean inNovidade;
    /** Avatar de quem respondeu (para o aviso no Início). */
    private Long idUsuarioResposta;
    private String nmCargoResposta;
    private String dsConfigResposta;
    private LocalDateTime dhCriacao;
    private LocalDateTime dhAtualizacao;
    /** Ids das imagens, na ordem (baixadas em /sac/imagens/{id}). */
    private List<Long> imagens = new ArrayList<>();
}
