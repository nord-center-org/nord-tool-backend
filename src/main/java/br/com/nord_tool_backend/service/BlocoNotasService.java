package br.com.nord_tool_backend.service;

import br.com.nord_tool_backend.dto.BlocoCadernoDto;
import br.com.nord_tool_backend.dto.BlocoPaginaDto;
import br.com.nord_tool_backend.form.BlocoCadernoForm;
import br.com.nord_tool_backend.form.BlocoConteudoForm;
import br.com.nord_tool_backend.form.BlocoPaginaForm;
import br.com.nord_tool_backend.storage.ArquivoDownload;

import java.util.List;

/** Bloco de notas (Mapeamentos): cadernos pessoais de quem está logado. */
public interface BlocoNotasService {

    List<BlocoCadernoDto> cadernos();

    /** O caderno com as páginas (sem o desenho). */
    BlocoCadernoDto abrir(Long id);

    BlocoCadernoDto criar(BlocoCadernoForm form);

    BlocoCadernoDto alterar(Long id, BlocoCadernoForm form);

    void excluir(Long id);

    /** A página com o desenho. */
    BlocoPaginaDto pagina(Long id);

    BlocoPaginaDto novaPagina(Long idCaderno, BlocoPaginaForm form);

    /** Página com fundo de PDF: a imagem da página do PDF (o navegador converte), na posição e no tamanho dados. */
    BlocoPaginaDto paginaDePdf(Long idCaderno, Integer posicao, int largura, int altura, String nome, byte[] bytes);

    /** Salva o desenho; devolve a página com a versão nova. */
    BlocoPaginaDto salvar(Long idPagina, BlocoConteudoForm form);

    BlocoCadernoDto reordenar(Long idCaderno, List<Long> ids);

    BlocoCadernoDto excluirPagina(Long idPagina);

    /** Guarda uma imagem para colar numa página; devolve o id que o desenho usa. */
    Long imagem(Long idCaderno, String nome, byte[] bytes);

    ArquivoDownload baixarImagem(Long idImagem);

    ArquivoDownload baixarFundo(Long idPagina);
}
