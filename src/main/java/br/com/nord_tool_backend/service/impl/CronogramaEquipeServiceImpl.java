package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.domain.CronogramaEquipe;
import br.com.nord_tool_backend.dto.CronogramaEquipeDto;
import br.com.nord_tool_backend.exception.ConflitoException;
import br.com.nord_tool_backend.exception.EntradaInvalidaException;
import br.com.nord_tool_backend.exception.NaoEncontradoException;
import br.com.nord_tool_backend.form.CronogramaEquipeForm;
import br.com.nord_tool_backend.repository.CronogramaEquipeRepository;
import br.com.nord_tool_backend.repository.CronogramaSemanalRepository;
import br.com.nord_tool_backend.security.Acao;
import br.com.nord_tool_backend.security.Modulo;
import br.com.nord_tool_backend.security.UsuarioAutenticado;
import br.com.nord_tool_backend.service.AutorizacaoService;
import br.com.nord_tool_backend.service.ContextoFaseService;
import br.com.nord_tool_backend.service.CronogramaEquipeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CronogramaEquipeServiceImpl implements CronogramaEquipeService {

    static final int MAX_MEMBROS = 200;

    private final CronogramaEquipeRepository repository;
    private final CronogramaSemanalRepository cronograma;
    private final AutorizacaoService autorizacao;
    private final ContextoFaseService contexto;

    @Override
    public List<CronogramaEquipeDto> minhasEquipes() {
        UsuarioAutenticado u = autorizacao.exigir(Modulo.CRONOGRAMA, Acao.LEITURA);
        boolean admin = u.acao(Modulo.CRONOGRAMA).atende(Acao.ADMIN);
        return repository.listar(null, u.getId(), admin).stream().map(CronogramaEquipeServiceImpl::dto).collect(Collectors.toList());
    }

    @Override
    public List<CronogramaEquipeDto> equipesDaObra() {
        UsuarioAutenticado u = autorizacao.exigir(Modulo.CRONOGRAMA, Acao.ADMIN);
        Long obra = contexto.obraEscolhida();
        return repository.listar(obra, u.getId(), true).stream().map(CronogramaEquipeServiceImpl::dto).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CronogramaEquipeDto criar(CronogramaEquipeForm form) {
        autorizacao.exigir(Modulo.CRONOGRAMA, Acao.ADMIN);
        Long obra = contexto.obraEscolhida();
        String nome = nome(form);
        if (repository.nomeEmUso(obra, nome, null)) throw new ConflitoException("Já existe uma equipe com esse nome nesta obra");
        Set<Long> membros = membros(form);
        Long id = repository.criar(obra, nome, form.getIdLider());
        repository.substituirMembros(id, membros);
        return buscar(obra, id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CronogramaEquipeDto alterar(Long id, CronogramaEquipeForm form) {
        autorizacao.exigir(Modulo.CRONOGRAMA, Acao.ADMIN);
        Long obra = contexto.obraEscolhida();
        String nome = nome(form);
        if (repository.nomeEmUso(obra, nome, id)) throw new ConflitoException("Já existe uma equipe com esse nome nesta obra");
        Set<Long> membros = membros(form);
        if (repository.alterar(id, obra, nome, form.getIdLider()) == 0) throw new NaoEncontradoException("Equipe não encontrada nesta obra");
        repository.substituirMembros(id, membros);
        return buscar(obra, id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void excluir(Long id) {
        autorizacao.exigir(Modulo.CRONOGRAMA, Acao.ADMIN);
        if (repository.excluir(id, contexto.obraEscolhida()) == 0) throw new NaoEncontradoException("Equipe não encontrada nesta obra");
    }

    @Override
    public List<Long> pelaHierarquia(Long idLider) {
        autorizacao.exigir(Modulo.CRONOGRAMA, Acao.ADMIN);
        if (idLider == null) throw new EntradaInvalidaException("Escolha o líder da equipe");
        return repository.hierarquia(idLider);
    }

    private static String nome(CronogramaEquipeForm form) {
        String nome = form.getNmEquipe() == null ? "" : form.getNmEquipe().trim();
        if (nome.isEmpty()) throw new EntradaInvalidaException("Dê um nome à equipe");
        return nome;
    }

    /** Membros sem repetição, com o líder incluído; todos precisam estar ativos. */
    private Set<Long> membros(CronogramaEquipeForm form) {
        Set<Long> ids = new LinkedHashSet<>();
        if (form.getIdLider() != null) ids.add(form.getIdLider());
        for (Long id : form.getIdsMembros() == null ? Collections.<Long>emptyList() : form.getIdsMembros()) if (id != null) ids.add(id);
        if (ids.isEmpty()) throw new EntradaInvalidaException("Escolha ao menos uma pessoa para a equipe");
        if (ids.size() > MAX_MEMBROS) throw new EntradaInvalidaException("A equipe pode ter no máximo " + MAX_MEMBROS + " pessoas");
        if (cronograma.contarUsuariosAtivos(ids) != ids.size()) throw new EntradaInvalidaException("Há pessoas inativas ou inexistentes na equipe");
        return ids;
    }

    private CronogramaEquipeDto buscar(Long obra, Long id) {
        UsuarioAutenticado u = autorizacao.exigirAutenticado();
        return repository.listar(obra, u.getId(), true).stream().filter(e -> id.equals(e.getId())).findFirst()
                .map(CronogramaEquipeServiceImpl::dto).orElseThrow(() -> new NaoEncontradoException("Equipe não encontrada"));
    }

    static CronogramaEquipeDto dto(CronogramaEquipe e) {
        List<Long> ids = e.getTxMembros() == null || e.getTxMembros().isBlank() ? Collections.emptyList()
                : Arrays.stream(e.getTxMembros().split(",")).map(String::trim).map(Long::valueOf).collect(Collectors.toList());
        return new CronogramaEquipeDto(e.getId(), e.getIdObra(), e.getNmObra(), e.getNmEquipe(), e.getIdLider(), e.getNmLider(), ids);
    }
}
