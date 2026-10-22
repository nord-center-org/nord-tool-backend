package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.domain.Fase;
import br.com.nord_tool_backend.dto.FaseDto;
import br.com.nord_tool_backend.dto.ObraDto;
import br.com.nord_tool_backend.exception.EntradaInvalidaException;
import br.com.nord_tool_backend.repository.ContextoFaseRepository;
import br.com.nord_tool_backend.security.Acao;
import br.com.nord_tool_backend.security.EscopoFase;
import br.com.nord_tool_backend.security.Modulo;
import br.com.nord_tool_backend.security.UsuarioAutenticado;
import br.com.nord_tool_backend.service.AutorizacaoService;
import br.com.nord_tool_backend.service.ContextoFaseService;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ContextoFaseServiceImpl implements ContextoFaseService {

    private static final String ATRIBUTO = ContextoFaseServiceImpl.class.getName() + ".escopo";

    private final ContextoFaseRepository repository;
    private final AutorizacaoService autorizacao;

    public ContextoFaseServiceImpl(ContextoFaseRepository repository, AutorizacaoService autorizacao) {
        this.repository = repository;
        this.autorizacao = autorizacao;
    }

    @Override
    public EscopoFase escopo() {
        RequestAttributes req = RequestContextHolder.getRequestAttributes();
        if (req != null && req.getAttribute(ATRIBUTO, RequestAttributes.SCOPE_REQUEST) instanceof EscopoFase) {
            return (EscopoFase) req.getAttribute(ATRIBUTO, RequestAttributes.SCOPE_REQUEST);
        }
        EscopoFase escopo = calcular();
        if (req != null) req.setAttribute(ATRIBUTO, escopo, RequestAttributes.SCOPE_REQUEST);
        return escopo;
    }

    private EscopoFase calcular() {
        UsuarioAutenticado u = autorizacao.exigirAutenticado();
        if (!repository.existeFase()) return EscopoFase.livre();
        Long obra = cabecalhoLong(CABECALHO_OBRA).orElse(null);
        boolean admin = ehAdmin(u);
        List<Fase> fases = admin ? repository.fasesAtivas(obra) : repository.fasesDoUsuario(u.getId(), obra);
        // Admin sem obra escolhida vê também os registros antigos sem fase, para poder organizá-los.
        return new EscopoFase(false, fases.stream().map(Fase::getId).collect(Collectors.toList()), admin && obra == null);
    }

    @Override
    public Long faseParaCriar(String codigoComSigla) {
        UsuarioAutenticado u = autorizacao.exigirAutenticado();
        if (!repository.existeFase()) return null;
        Long obra = cabecalhoLong(CABECALHO_OBRA).orElse(null);
        List<Fase> possiveis = ehAdmin(u) ? repository.fasesAtivas(obra) : repository.fasesDoUsuario(u.getId(), obra);
        Optional<Long> pedida = cabecalhoLong(CABECALHO_FASE);
        if (pedida.isPresent()) {
            return possiveis.stream().map(Fase::getId).filter(pedida.get()::equals).findFirst()
                    .orElseThrow(() -> new EntradaInvalidaException("Você não participa da fase escolhida"));
        }
        if (codigoComSigla != null && codigoComSigla.contains("-")) {
            String sigla = codigoComSigla.split("-", 2)[0].trim().toUpperCase(Locale.ROOT);
            List<Fase> mesmaSigla = possiveis.stream()
                    .filter(f -> f.getCdFase() != null && f.getCdFase().equalsIgnoreCase(sigla)).collect(Collectors.toList());
            if (mesmaSigla.size() == 1) return mesmaSigla.get(0).getId();
        }
        if (possiveis.size() == 1) return possiveis.get(0).getId();
        if (possiveis.isEmpty()) throw new EntradaInvalidaException("Você ainda não participa de nenhuma fase desta obra");
        throw new EntradaInvalidaException("Escolha a fase antes de salvar");
    }

    @Override
    public List<ObraDto> minhasObras() {
        UsuarioAutenticado u = autorizacao.exigirAutenticado();
        boolean admin = ehAdmin(u);
        List<Fase> fases = admin ? repository.fasesAtivas(null) : repository.fasesDoUsuario(u.getId(), null);
        Map<Long, List<FaseDto>> porObra = fases.stream()
                .map(f -> FaseDto.de(f, Collections.emptyList()))
                .collect(Collectors.groupingBy(FaseDto::getIdObra));
        return repository.obrasAtivas().stream()
                .filter(o -> porObra.containsKey(o.getId()) || admin)
                .map(o -> {
                    ObraDto d = ObraDto.de(o, porObra.getOrDefault(o.getId(), Collections.emptyList()));
                    if (!admin) d.setCdAcesso(null); // o código só interessa ao admin
                    return d;
                })
                .collect(Collectors.toList());
    }

    private static boolean ehAdmin(UsuarioAutenticado u) {
        return u.acao(Modulo.ADMINISTRACAO).atende(Acao.ADMIN);
    }

    private static Optional<Long> cabecalhoLong(String nome) {
        RequestAttributes req = RequestContextHolder.getRequestAttributes();
        if (!(req instanceof ServletRequestAttributes)) return Optional.empty();
        String valor = ((ServletRequestAttributes) req).getRequest().getHeader(nome);
        if (valor == null || valor.isBlank()) return Optional.empty();
        try {
            return Optional.of(Long.parseLong(valor.trim()));
        } catch (NumberFormatException e) {
            throw new EntradaInvalidaException("Cabeçalho " + nome + " inválido");
        }
    }
}
