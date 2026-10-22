package br.com.nord_tool_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data @AllArgsConstructor @NoArgsConstructor
public class AcessoUsuarioDto {
    private Long id;
    private String nmNome;
    private String nmEmail;
    private String cdPerfil;
    private boolean inAtivo;
    private LocalDateTime dhUltimoLogin;
    private String nrCelular;
    private Integer idEmpresa;
    private String nmEmpresa;
    private Integer idCargo;
    private String nmCargo;
    /** PENDENTE (aguardando aprovação), APROVADO ou RECUSADO. */
    private String cdSituacaoCadastro;
    private Long idObraSolicitada;
    private String nmObraSolicitada;
    /** Nível efetivo por módulo (perfil + exceções do usuário); módulos sem acesso ficam fora. */
    private Map<String, String> acessos;
    /** Superior direto (hierarquia). */
    private Long idSuperior;
    /** O que perfil + cargo dão, sem as exceções do usuário (base para "personalizado"). */
    private Map<String, String> acessosPadrao;

    public AcessoUsuarioDto(Long id, String nmNome, String nmEmail, String cdPerfil, boolean inAtivo, LocalDateTime dhUltimoLogin,
                            String nrCelular, Integer idEmpresa, String nmEmpresa, Integer idCargo, String nmCargo,
                            String cdSituacaoCadastro, Long idObraSolicitada, String nmObraSolicitada, Map<String, String> acessos) {
        this(id, nmNome, nmEmail, cdPerfil, inAtivo, dhUltimoLogin, nrCelular, idEmpresa, nmEmpresa, idCargo, nmCargo,
                cdSituacaoCadastro, idObraSolicitada, nmObraSolicitada, acessos, null, null);
    }
}
