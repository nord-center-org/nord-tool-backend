package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.dto.GameConquistaDto;
import br.com.nord_tool_backend.dto.GameCopaDto;
import br.com.nord_tool_backend.dto.GameRegistroDto;
import br.com.nord_tool_backend.dto.GameResumoDto;
import br.com.nord_tool_backend.dto.GameTotaisDto;
import br.com.nord_tool_backend.exception.EntradaInvalidaException;
import br.com.nord_tool_backend.form.GameCopaForm;
import br.com.nord_tool_backend.form.GamePartidaForm;
import br.com.nord_tool_backend.repository.GameRepository;
import br.com.nord_tool_backend.security.Acao;
import br.com.nord_tool_backend.security.Modulo;
import br.com.nord_tool_backend.service.AutorizacaoService;
import br.com.nord_tool_backend.service.GameService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Games (aba Individual): cada partida registrada vira recorde, ranking, histórico e conquistas. Os jogos rodam no
 * navegador; aqui só se guarda o resultado de quem está logado, sempre na própria conta.
 */
@Service
@RequiredArgsConstructor
public class GameServiceImpl implements GameService {

    static final int LIMITE_RANKING = 10;
    static final int LIMITE_HISTORICO = 10;
    static final int LIMITE_COPAS = 20;
    static final int MAX_FAVORITOS = 21;
    static final Pattern JOGO = Pattern.compile("^[a-z0-9-]{1,40}$");
    private static final TypeReference<List<String>> LISTA = new TypeReference<>() { };

    /** Uma conquista: nome, descrição e a regra sobre os totais (de todos os jogos) depois da partida. */
    private static final class Conquista {
        private final String nome;
        private final String descricao;
        private final Predicate<GameTotaisDto> regra;

        private Conquista(String nome, String descricao, Predicate<GameTotaisDto> regra) {
            this.nome = nome;
            this.descricao = descricao;
            this.regra = regra;
        }
    }

    static final String CAMPEAO_COPA = "CAMPEAO_COPA";
    private static final Map<String, Conquista> CONQUISTAS = new LinkedHashMap<>();

    static {
        CONQUISTAS.put("PRIMEIRA_PARTIDA", new Conquista("Primeiro controle", "Jogou a primeira partida", t -> t.getNrPartidas() >= 1));
        CONQUISTAS.put("PRIMEIRA_VITORIA", new Conquista("Primeira vitória", "Venceu uma partida", t -> t.getNrVitorias() >= 1));
        CONQUISTAS.put("DEZ_VITORIAS", new Conquista("Dez na conta", "Venceu 10 partidas", t -> t.getNrVitorias() >= 10));
        CONQUISTAS.put("CINQUENTA_VITORIAS", new Conquista("Veterano", "Venceu 50 partidas", t -> t.getNrVitorias() >= 50));
        CONQUISTAS.put("CEM_PARTIDAS", new Conquista("Maratonista", "Jogou 100 partidas", t -> t.getNrPartidas() >= 100));
        CONQUISTAS.put("CINCO_JOGOS", new Conquista("Explorador", "Jogou 5 jogos diferentes", t -> t.getNrJogos() >= 5));
        CONQUISTAS.put("VENCEU_DIFICIL", new Conquista("Sem moleza", "Venceu os bots no Difícil", t -> t.getNrVitoriasDificil() >= 1));
        CONQUISTAS.put("OITO_JOGADORES", new Conquista("Casa cheia", "Venceu uma partida com 8 jogadores", t -> t.getNrVitoriasOito() >= 1));
        CONQUISTAS.put(CAMPEAO_COPA, new Conquista("Campeão da Copa", "Terminou uma Copa em 1º lugar", t -> false));
    }

    private final GameRepository repository;
    private final AutorizacaoService autorizacao;
    private final ObjectMapper mapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GameRegistroDto registrarPartida(GamePartidaForm partida) {
        Long eu = autorizacao.exigir(Modulo.GAMES, Acao.ESCRITA).getId();
        if (partida.getNrBots() >= partida.getNrJogadores()) throw new EntradaInvalidaException("Bots demais para a partida");
        if (partida.getNrColocacao() > partida.getNrJogadores()) throw new EntradaInvalidaException("Colocação maior que o número de jogadores");
        long recordeAntes = repository.totais(eu, partida.getCdJogo()).getNrRecorde();
        repository.registrarPartida(eu, partida);
        GameTotaisDto todos = repository.totais(eu, null);
        List<GameConquistaDto> novas = new ArrayList<>();
        CONQUISTAS.forEach((codigo, c) -> {
            if (c.regra.test(todos) && repository.ganharConquista(eu, codigo)) novas.add(dto(codigo, c, LocalDateTime.now()));
        });
        boolean novoRecorde = partida.getNrPontos() > recordeAntes;
        return new GameRegistroDto(Math.max(recordeAntes, partida.getNrPontos()), novoRecorde, novas);
    }

    @Override
    @Transactional(readOnly = true)
    public GameResumoDto resumo(String jogo) {
        Long eu = autorizacao.exigir(Modulo.GAMES, Acao.LEITURA).getId();
        if (jogo == null || !JOGO.matcher(jogo).matches()) throw new EntradaInvalidaException("Jogo inválido");
        return new GameResumoDto(repository.totais(eu, jogo), repository.ranking(jogo, LIMITE_RANKING), lerConquistas(eu),
                repository.historico(eu, jogo, LIMITE_HISTORICO));
    }

    @Override
    @Transactional(readOnly = true)
    public List<GameConquistaDto> conquistas() {
        return lerConquistas(autorizacao.exigir(Modulo.GAMES, Acao.LEITURA).getId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> favoritos() {
        Long eu = autorizacao.exigir(Modulo.GAMES, Acao.LEITURA).getId();
        return repository.favoritos(eu).map(this::ler).orElseGet(Collections::emptyList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<String> salvarFavoritos(List<String> jogos) {
        Long eu = autorizacao.exigir(Modulo.GAMES, Acao.LEITURA).getId();
        Set<String> unicos = new LinkedHashSet<>();
        for (String j : jogos == null ? Collections.<String>emptyList() : jogos) {
            if (j == null || !JOGO.matcher(j).matches()) throw new EntradaInvalidaException("Jogo inválido nos favoritos");
            unicos.add(j);
        }
        if (unicos.size() > MAX_FAVORITOS) throw new EntradaInvalidaException("No máximo " + MAX_FAVORITOS + " favoritos");
        List<String> lista = new ArrayList<>(unicos);
        repository.salvarFavoritos(eu, json(lista));
        return lista;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GameCopaDto registrarCopa(GameCopaForm copa) {
        Long eu = autorizacao.exigir(Modulo.GAMES, Acao.ESCRITA).getId();
        int rodadas = copa.getNrRodadas();
        if (rodadas != 3 && rodadas != 5 && rodadas != 7) throw new EntradaInvalidaException("A Copa tem 3, 5 ou 7 rodadas");
        try {
            mapper.readTree(copa.getTxResultado());
        } catch (JsonProcessingException e) {
            throw new EntradaInvalidaException("Resultado da Copa inválido", e);
        }
        Long id = repository.registrarCopa(eu, rodadas, copa.getNrColocacao(), copa.getTxResultado());
        if (copa.getNrColocacao() == 1) repository.ganharConquista(eu, CAMPEAO_COPA);
        GameCopaDto dto = new GameCopaDto();
        dto.setId(id);
        dto.setNrRodadas(rodadas);
        dto.setNrColocacao(copa.getNrColocacao());
        dto.setTxResultado(copa.getTxResultado());
        dto.setDhCriacao(LocalDateTime.now());
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public List<GameCopaDto> copas() {
        return repository.copas(autorizacao.exigir(Modulo.GAMES, Acao.LEITURA).getId(), LIMITE_COPAS);
    }

    /** O catálogo inteiro; as que ainda não vieram ficam com a data vazia. */
    private List<GameConquistaDto> lerConquistas(Long eu) {
        Map<String, LocalDateTime> ganhas = repository.conquistas(eu);
        List<GameConquistaDto> lista = new ArrayList<>();
        CONQUISTAS.forEach((codigo, c) -> lista.add(dto(codigo, c, ganhas.get(codigo))));
        return lista;
    }

    private static GameConquistaDto dto(String codigo, Conquista c, LocalDateTime quando) {
        GameConquistaDto d = new GameConquistaDto();
        d.setCdConquista(codigo);
        d.setNmConquista(c.nome);
        d.setTxDescricao(c.descricao);
        d.setDhCriacao(quando);
        return d;
    }

    private String json(List<String> lista) {
        try {
            return mapper.writeValueAsString(lista);
        } catch (JsonProcessingException e) {
            throw new EntradaInvalidaException("Favoritos inválidos", e);
        }
    }

    private List<String> ler(String json) {
        try {
            List<String> l = mapper.readValue(json, LISTA);
            return l == null ? Collections.emptyList() : l;
        } catch (JsonProcessingException e) {
            return Collections.emptyList();
        }
    }
}
