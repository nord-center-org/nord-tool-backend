package br.com.nord_tool_backend.service;

import br.com.nord_tool_backend.dto.MarketingConexaoDto;
import br.com.nord_tool_backend.dto.MarketingContaDto;
import br.com.nord_tool_backend.dto.MarketingMidiaDto;
import br.com.nord_tool_backend.dto.MarketingMidiaPublicaDto;
import br.com.nord_tool_backend.dto.MarketingPainelDto;
import br.com.nord_tool_backend.dto.MarketingPublicacaoDto;
import br.com.nord_tool_backend.dto.MarketingRedeDto;
import br.com.nord_tool_backend.form.MarketingConectarForm;
import br.com.nord_tool_backend.form.MarketingMidiaForm;
import br.com.nord_tool_backend.form.MarketingPublicarForm;

import java.util.List;
import java.util.Optional;

/** Marketing: contas de redes sociais da própria pessoa, publicar vídeos nelas e o painel de views. */
public interface MarketingService {

    List<MarketingRedeDto> redes();

    /** Endereço do login da rede; {@code origem} é o endereço do frontend (precisa estar entre as origens permitidas). */
    MarketingConexaoDto conexao(String rede, String origem);

    MarketingContaDto conectar(MarketingConectarForm form, String origem);

    void desconectar(Long idConta);

    MarketingMidiaDto iniciarMidia(MarketingMidiaForm form);

    void receberParte(String idMidia, int indice, byte[] dados);

    /** Publica nas contas escolhidas em segundo plano; devolve as publicações (ENVIANDO). */
    List<MarketingPublicacaoDto> publicar(MarketingPublicarForm form, String baseApi);

    List<MarketingPublicacaoDto> publicacoes();

    List<MarketingPainelDto> painel(boolean atualizar);

    /** Vídeo pelo código do link público (sem login); vazio se não existe ou já venceu. */
    Optional<MarketingMidiaPublicaDto> midiaPublica(String codigo);
}
