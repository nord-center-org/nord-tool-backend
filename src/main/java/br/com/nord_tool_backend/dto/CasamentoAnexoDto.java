package br.com.nord_tool_backend.dto;

import br.com.nord_tool_backend.domain.CasamentoAnexo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZoneId;

@Data @AllArgsConstructor @NoArgsConstructor
public class CasamentoAnexoDto {
    private Long idAnexo;
    private Long idFornecedor;
    private String nmArquivo;
    private String nmContentType;
    private Long nrTamanhoBytes;
    private String txDescricao;
    /** Epoch millis da criação; usar em ?v= para cache. */
    private Long nrVersao;

    public static CasamentoAnexoDto de(CasamentoAnexo a) {
        long versao = a.getDhCriacao() == null ? 0L
                : a.getDhCriacao().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        return new CasamentoAnexoDto(a.getId(), a.getIdFornecedor(), a.getNmArquivo(), a.getNmContentType(),
                a.getNrTamanhoBytes(), a.getTxDescricao(), versao);
    }
}
