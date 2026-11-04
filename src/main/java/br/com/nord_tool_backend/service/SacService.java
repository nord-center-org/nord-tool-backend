package br.com.nord_tool_backend.service;

import br.com.nord_tool_backend.dto.SacChamadoDto;
import br.com.nord_tool_backend.form.SacRespostaForm;
import br.com.nord_tool_backend.storage.ArquivoDownload;

import java.util.List;

public interface SacService {
    /** Imagem anexada ao chamado: nome do arquivo e bytes. */
    final class Anexo {
        public final String nome;
        public final byte[] bytes;

        public Anexo(String nome, byte[] bytes) {
            this.nome = nome;
            this.bytes = bytes;
        }
    }

    /** Os chamados da pessoa; com {@code todos} (só ADMIN do SAC), os de todo mundo. */
    List<SacChamadoDto> listar(boolean todos);

    SacChamadoDto abrir(String nmTitulo, String txDescricao, String txTela, List<Anexo> anexos);

    SacChamadoDto responder(Long id, SacRespostaForm form);

    void excluir(Long id);

    ArquivoDownload baixarImagem(Long idImagem);
}
