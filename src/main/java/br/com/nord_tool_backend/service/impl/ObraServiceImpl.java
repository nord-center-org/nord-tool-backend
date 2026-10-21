package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.domain.Fase;
import br.com.nord_tool_backend.domain.Obra;
import br.com.nord_tool_backend.dto.FaseDto;
import br.com.nord_tool_backend.dto.ObraDto;
import br.com.nord_tool_backend.exception.ConflitoException;
import br.com.nord_tool_backend.exception.EntradaInvalidaException;
import br.com.nord_tool_backend.exception.NaoEncontradoException;
import br.com.nord_tool_backend.form.FaseForm;
import br.com.nord_tool_backend.form.FaseUsuariosForm;
import br.com.nord_tool_backend.form.ObraForm;
import br.com.nord_tool_backend.repository.ObraRepository;
import br.com.nord_tool_backend.security.Acao;
import br.com.nord_tool_backend.security.Modulo;
import br.com.nord_tool_backend.service.AutorizacaoService;
import br.com.nord_tool_backend.service.ObraService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/** Cadastro de obras e fases e de quem participa de cada fase (só administradores). */
@Service
public class ObraServiceImpl implements ObraService {

    static final String MSG_CONFLITO = "O cadastro mudou. Sincronize e tente novamente.";

    private final ObraRepository repository;
    private final AutorizacaoService autorizacao;

    public ObraServiceImpl(ObraRepository repository, AutorizacaoService autorizacao) {
        this.repository = repository;
        this.autorizacao = autorizacao;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ObraDto> listar() {
        autorizacao.exigir(Modulo.ADMINISTRACAO, Acao.ADMIN);
        Map<Long, List<Long>> participantes = repository.usuariosPorFase();
        Map<Long, List<FaseDto>> fasesPorObra = repository.listarFases().stream()
                .map(f -> FaseDto.de(f, participantes.getOrDefault(f.getId(), Collections.emptyList())))
                .collect(Collectors.groupingBy(FaseDto::getIdObra));
        return repository.listarObras().stream()
                .map(o -> ObraDto.de(o, fasesPorObra.getOrDefault(o.getId(), Collections.emptyList())))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ObraDto criarObra(ObraForm form) {
        autorizacao.exigir(Modulo.ADMINISTRACAO, Acao.ADMIN);
        Obra o = obra(form);
        o.setInAtivo(form.getInAtivo() == null ? Boolean.TRUE : form.getInAtivo());
        Long id = repository.inserirObra(o);
        return ObraDto.de(obraExistente(id), Collections.emptyList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ObraDto alterarObra(Long id, ObraForm form) {
        autorizacao.exigir(Modulo.ADMINISTRACAO, Acao.ADMIN);
        int versao = versao(form.getNrVersao());
        Obra atual = obraExistente(id);
        Obra o = obra(form);
        o.setId(id);
        o.setInAtivo(form.getInAtivo() == null ? atual.getInAtivo() : form.getInAtivo());
        if (repository.alterarObra(o, versao) == 0) throw new ConflitoException(MSG_CONFLITO);
        return listar().stream().filter(x -> x.getId().equals(id)).findFirst().orElseThrow(() -> new NaoEncontradoException("Obra não encontrada"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FaseDto criarFase(Long idObra, FaseForm form) {
        autorizacao.exigir(Modulo.ADMINISTRACAO, Acao.ADMIN);
        obraExistente(idObra);
        Fase f = fase(form);
        f.setIdObra(idObra);
        f.setInAtivo(form.getInAtivo() == null ? Boolean.TRUE : form.getInAtivo());
        Long id = repository.inserirFase(f);
        return FaseDto.de(faseExistente(id), Collections.emptyList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FaseDto alterarFase(Long id, FaseForm form) {
        autorizacao.exigir(Modulo.ADMINISTRACAO, Acao.ADMIN);
        int versao = versao(form.getNrVersao());
        Fase atual = faseExistente(id);
        Fase f = fase(form);
        f.setId(id);
        f.setIdObra(atual.getIdObra());
        f.setInAtivo(form.getInAtivo() == null ? atual.getInAtivo() : form.getInAtivo());
        if (repository.alterarFase(f, versao) == 0) throw new ConflitoException(MSG_CONFLITO);
        return FaseDto.de(faseExistente(id), repository.usuariosPorFase().getOrDefault(id, Collections.emptyList()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FaseDto definirUsuarios(Long idFase, FaseUsuariosForm form) {
        autorizacao.exigir(Modulo.ADMINISTRACAO, Acao.ADMIN);
        Fase f = faseExistente(idFase);
        List<Long> ids = new ArrayList<>(new LinkedHashSet<>(form.getIdsUsuarios()));
        if (ids.stream().anyMatch(i -> i == null)) throw new EntradaInvalidaException("Usuário inválido");
        if (repository.contarUsuarios(ids) != ids.size()) throw new EntradaInvalidaException("Há usuários inexistentes na lista");
        repository.substituirUsuariosDaFase(idFase, ids);
        return FaseDto.de(f, ids);
    }

    // ---------- apoio ----------

    private Obra obraExistente(Long id) {
        return repository.buscarObra(id).orElseThrow(() -> new NaoEncontradoException("Obra não encontrada"));
    }

    private Fase faseExistente(Long id) {
        return repository.buscarFase(id).orElseThrow(() -> new NaoEncontradoException("Fase não encontrada"));
    }

    private static Obra obra(ObraForm form) {
        Obra o = new Obra();
        o.setNmObra(form.getNmObra().trim());
        o.setCdAcesso(form.getCdAcesso().trim().toUpperCase(Locale.ROOT));
        return o;
    }

    private static Fase fase(FaseForm form) {
        Fase f = new Fase();
        f.setNmFase(form.getNmFase().trim());
        String sigla = form.getCdFase() == null ? "" : form.getCdFase().trim();
        f.setCdFase(sigla.isEmpty() ? null : sigla.toUpperCase(Locale.ROOT));
        f.setNrOrdem(form.getNrOrdem() == null ? 0 : form.getNrOrdem());
        return f;
    }

    private static int versao(Integer nrVersao) {
        if (nrVersao == null) throw new EntradaInvalidaException("Informe a versão (nrVersao)");
        return nrVersao;
    }
}
