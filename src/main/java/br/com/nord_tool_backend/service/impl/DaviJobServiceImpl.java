package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.dto.DaviJobPlacarDto;
import br.com.nord_tool_backend.exception.EntradaInvalidaException;
import br.com.nord_tool_backend.form.DaviJobPontuacaoForm;
import br.com.nord_tool_backend.repository.DaviJobRepository;
import br.com.nord_tool_backend.service.AutorizacaoService;
import br.com.nord_tool_backend.service.DaviJobService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Davi Job (easter egg): basta estar logado para jogar e ver o ranking. */
@Service
public class DaviJobServiceImpl implements DaviJobService {

    static final int TOP = 10;
    static final int MAX_PONTOS = 10000;

    private final DaviJobRepository repository;
    private final AutorizacaoService autorizacao;

    public DaviJobServiceImpl(DaviJobRepository repository, AutorizacaoService autorizacao) {
        this.repository = repository;
        this.autorizacao = autorizacao;
    }

    @Override
    @Transactional(readOnly = true)
    public DaviJobPlacarDto placar() {
        return montar(autorizacao.exigirAutenticado().getId());
    }

    @Override
    @Transactional
    public DaviJobPlacarDto registrar(DaviJobPontuacaoForm form) {
        Long eu = autorizacao.exigirAutenticado().getId();
        Integer pontos = form.getNrPontos();
        if (pontos == null || pontos < 0 || pontos > MAX_PONTOS) throw new EntradaInvalidaException("Pontos inválidos");
        repository.inserir(eu, pontos);
        return montar(eu);
    }

    private DaviJobPlacarDto montar(Long eu) {
        DaviJobPlacarDto p = new DaviJobPlacarDto();
        p.setRanking(repository.ranking(eu, TOP));
        p.setMeuRecorde(repository.recorde(eu).orElse(null));
        return p;
    }
}
