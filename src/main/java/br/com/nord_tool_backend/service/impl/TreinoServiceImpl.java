package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.dto.TreinoDto;
import br.com.nord_tool_backend.dto.TreinoExercicioDto;
import br.com.nord_tool_backend.dto.TreinoSessaoDto;
import br.com.nord_tool_backend.dto.TreinoSessaoExercicioDto;
import br.com.nord_tool_backend.exception.ConflitoException;
import br.com.nord_tool_backend.exception.EntradaInvalidaException;
import br.com.nord_tool_backend.exception.NaoEncontradoException;
import br.com.nord_tool_backend.form.TreinoExercicioForm;
import br.com.nord_tool_backend.form.TreinoForm;
import br.com.nord_tool_backend.form.TreinoSessaoForm;
import br.com.nord_tool_backend.repository.TreinoRepository;
import br.com.nord_tool_backend.service.AutorizacaoService;
import br.com.nord_tool_backend.service.TreinoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Treinos (Gestão Individual): dado pessoal, então basta estar logado; cada pessoa só vê e altera o que é seu.
 * Exercícios mantêm o id ao editar a ficha, para o histórico das sessões continuar ligado a eles.
 */
@Service
public class TreinoServiceImpl implements TreinoService {

    static final String MSG_CONFLITO = "O treino mudou. Sincronize e tente novamente.";
    static final int LIMITE_PADRAO = 30;
    static final int LIMITE_MAXIMO = 200;

    private final TreinoRepository repository;
    private final AutorizacaoService autorizacao;

    public TreinoServiceImpl(TreinoRepository repository, AutorizacaoService autorizacao) {
        this.repository = repository;
        this.autorizacao = autorizacao;
    }

    private Long dono() {
        return autorizacao.exigirAutenticado().getId();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TreinoDto> listar() {
        Long dono = dono();
        Map<Long, List<TreinoExercicioDto>> exercicios = repository.listarExercicios(dono).stream()
                .collect(Collectors.groupingBy(TreinoExercicioDto::getIdTreino));
        List<TreinoDto> treinos = repository.listar(dono);
        treinos.forEach(t -> t.setExercicios(exercicios.getOrDefault(t.getId(), new ArrayList<>())));
        return treinos;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TreinoDto criar(TreinoForm form) {
        Long dono = dono();
        TreinoDto t = cabecalho(form);
        if (t.getNrOrdem() == null) t.setNrOrdem(repository.listar(dono).size() + 1);
        Long id = repository.inserir(dono, t);
        sincronizarExercicios(id, new ArrayList<>(), form.getExercicios());
        return completo(dono, id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TreinoDto alterar(Long id, TreinoForm form) {
        Long dono = dono();
        if (form.getNrVersao() == null) throw new EntradaInvalidaException("Informe a versão (nrVersao)");
        TreinoDto atual = treino(dono, id);
        TreinoDto t = cabecalho(form);
        t.setId(id);
        if (t.getNrOrdem() == null) t.setNrOrdem(atual.getNrOrdem());
        if (repository.alterar(dono, t, form.getNrVersao()) == 0) throw new ConflitoException(MSG_CONFLITO);
        List<TreinoExercicioDto> existentes = repository.listarExercicios(dono).stream()
                .filter(e -> id.equals(e.getIdTreino())).collect(Collectors.toList());
        sincronizarExercicios(id, existentes, form.getExercicios());
        return completo(dono, id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void excluir(Long id, Integer nrVersao) {
        Long dono = dono();
        if (nrVersao == null) throw new EntradaInvalidaException("Informe a versão (nrVersao)");
        treino(dono, id);
        if (repository.deletar(dono, id, nrVersao) == 0) throw new ConflitoException(MSG_CONFLITO);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TreinoSessaoDto> listarSessoes(Integer limite) {
        Long dono = dono();
        int n = limite == null ? LIMITE_PADRAO : Math.max(1, Math.min(LIMITE_MAXIMO, limite));
        List<TreinoSessaoDto> sessoes = repository.listarSessoes(dono, n);
        Map<Long, List<TreinoSessaoExercicioDto>> itens = repository.listarExerciciosDasSessoes(dono,
                sessoes.stream().map(TreinoSessaoDto::getId).collect(Collectors.toList())).stream()
                .collect(Collectors.groupingBy(TreinoSessaoExercicioDto::getIdSessao));
        sessoes.forEach(s -> s.setExercicios(itens.getOrDefault(s.getId(), new ArrayList<>())));
        return sessoes;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TreinoSessaoDto registrarSessao(TreinoSessaoForm form) {
        Long dono = dono();
        TreinoDto treino = treino(dono, form.getIdTreino());
        Set<Long> doTreino = repository.listarExercicios(dono).stream()
                .filter(e -> treino.getId().equals(e.getIdTreino())).map(TreinoExercicioDto::getId).collect(Collectors.toSet());
        TreinoSessaoDto s = new TreinoSessaoDto();
        s.setIdTreino(treino.getId());
        s.setNmTreino(treino.getNmTreino());
        s.setDtSessao(form.getDtSessao());
        s.setNrDuracaoMin(form.getNrDuracaoMin());
        s.setTxObservacao(texto(form.getTxObservacao()));
        Long id = repository.inserirSessao(dono, s);
        int ordem = 1;
        for (TreinoSessaoForm.Item item : form.getExercicios()) {
            // Exercício de outro treino (ou inexistente) entra no histórico só pelo nome.
            Long idExercicio = item.getIdExercicio() != null && doTreino.contains(item.getIdExercicio()) ? item.getIdExercicio() : null;
            TreinoSessaoExercicioDto e = new TreinoSessaoExercicioDto();
            e.setIdSessao(id);
            e.setNrOrdem(ordem++);
            e.setIdExercicio(idExercicio);
            e.setNmExercicio(item.getNmExercicio().trim());
            e.setTxRepeticoes(texto(item.getTxRepeticoes()));
            e.setVlCarga(item.getVlCarga());
            e.setInFeito(!Boolean.FALSE.equals(item.getInFeito()));
            repository.inserirExercicioDaSessao(e);
            if (Boolean.TRUE.equals(form.getAtualizarCargas()) && idExercicio != null && item.getVlCarga() != null && e.getInFeito()) {
                repository.atualizarCarga(dono, idExercicio, item.getVlCarga());
            }
        }
        return repository.listarSessoes(dono, LIMITE_MAXIMO).stream().filter(x -> id.equals(x.getId())).findFirst()
                .map(x -> {
                    x.setExercicios(repository.listarExerciciosDasSessoes(dono, java.util.Collections.singletonList(id)));
                    return x;
                })
                .orElseThrow(() -> new NaoEncontradoException("Sessão não encontrada"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void excluirSessao(Long id) {
        if (repository.deletarSessao(dono(), id) == 0) throw new NaoEncontradoException("Sessão não encontrada");
    }

    // ---------- apoio ----------

    private void sincronizarExercicios(Long idTreino, List<TreinoExercicioDto> existentes, List<TreinoExercicioForm> lista) {
        Set<Long> idsExistentes = existentes.stream().map(TreinoExercicioDto::getId).collect(Collectors.toSet());
        Set<Long> mantidos = new HashSet<>();
        for (TreinoExercicioForm f : lista) {
            if (f.getId() == null) continue;
            if (!idsExistentes.contains(f.getId())) throw new EntradaInvalidaException("Exercício não pertence a este treino");
            if (!mantidos.add(f.getId())) throw new EntradaInvalidaException("Exercício repetido na lista");
        }
        for (Long idAntigo : idsExistentes) {
            if (!mantidos.contains(idAntigo)) repository.deletarExercicio(idTreino, idAntigo);
        }
        int ordem = 1;
        for (TreinoExercicioForm f : lista) {
            TreinoExercicioDto e = new TreinoExercicioDto();
            e.setIdTreino(idTreino);
            e.setNrOrdem(ordem++);
            e.setNmExercicio(f.getNmExercicio().trim());
            e.setNrSeries(f.getNrSeries() == null ? 3 : f.getNrSeries());
            e.setTxRepeticoes(texto(f.getTxRepeticoes()));
            e.setVlCarga(f.getVlCarga());
            e.setNrDescansoSeg(f.getNrDescansoSeg());
            e.setTxObservacao(texto(f.getTxObservacao()));
            if (f.getId() == null) {
                repository.inserirExercicio(e);
            } else {
                e.setId(f.getId());
                repository.alterarExercicio(e);
            }
        }
    }

    private TreinoDto completo(Long dono, Long id) {
        TreinoDto t = treino(dono, id);
        t.setExercicios(repository.listarExercicios(dono).stream().filter(e -> id.equals(e.getIdTreino())).collect(Collectors.toList()));
        return t;
    }

    private TreinoDto treino(Long dono, Long id) {
        return repository.buscar(dono, id).orElseThrow(() -> new NaoEncontradoException("Treino não encontrado"));
    }

    private static TreinoDto cabecalho(TreinoForm f) {
        TreinoDto t = new TreinoDto();
        t.setNmTreino(f.getNmTreino().trim());
        t.setTxObservacao(texto(f.getTxObservacao()));
        t.setTxDias(texto(f.getTxDias()));
        t.setNrOrdem(f.getNrOrdem());
        t.setInAtivo(f.getInAtivo() == null || f.getInAtivo());
        return t;
    }

    private static String texto(String s) {
        return s == null || s.trim().isEmpty() ? null : s.trim();
    }
}
