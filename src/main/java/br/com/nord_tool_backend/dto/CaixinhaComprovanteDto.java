package br.com.nord_tool_backend.dto;

import br.com.nord_tool_backend.domain.CaixinhaComprovante;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZoneId;

@Data @AllArgsConstructor @NoArgsConstructor
public class CaixinhaComprovanteDto {
    private Long idComprovante;
    private Long idLancamento;
    private String nmArquivo;
    private String nmContentType;
    private Long nrTamanhoBytes;
    /** Epoch millis da criação. */
    private Long nrVersao;

    public static CaixinhaComprovanteDto de(CaixinhaComprovante c) {
        long versao = c.getDhCriacao() == null ? 0L
                : c.getDhCriacao().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        return new CaixinhaComprovanteDto(c.getId(), c.getIdLancamento(), c.getNmArquivo(), c.getNmContentType(),
                c.getNrTamanhoBytes(), versao);
    }
}
