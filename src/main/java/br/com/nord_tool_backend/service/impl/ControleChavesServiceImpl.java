package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.domain.RequisicaoChave;
import br.com.nord_tool_backend.dto.ApartamentoControleChavesDto;
import br.com.nord_tool_backend.dto.DashboardControleChavesDto;
import br.com.nord_tool_backend.dto.ObraControleChavesDto;
import br.com.nord_tool_backend.dto.PessoaControleChavesDto;
import br.com.nord_tool_backend.dto.RetiradaControleChavesDto;
import br.com.nord_tool_backend.form.NovaRetiradaControleChavesForm;
import br.com.nord_tool_backend.form.RecebimentoControleChavesForm;
import br.com.nord_tool_backend.repository.ControleChavesRepository;
import br.com.nord_tool_backend.service.ControleChavesService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ControleChavesServiceImpl implements ControleChavesService {

    private final ControleChavesRepository controleChavesRepository;
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Override
    public List<ObraControleChavesDto> listarObras() {
        return controleChavesRepository.listarObras();
    }

    @Override
    public List<ApartamentoControleChavesDto> listarApartamentos(String busca, int limite, int pagina) {
        return controleChavesRepository.listarApartamentos(busca, limite, pagina);
    }

    @Override
    public DashboardControleChavesDto buscarDashboard(int limiteRecentes, String idObra) {
        Long emCampo = controleChavesRepository.contarChavesEmCampo();
        Long noQuadro = controleChavesRepository.contarChavesNoQuadro();
        Long entregues = controleChavesRepository.contarChavesEntregues();

        List<RequisicaoChave> recentesDomain = controleChavesRepository.listarHistorico(null, null, idObra, limiteRecentes > 0 ? limiteRecentes : 5, 0);
        List<RetiradaControleChavesDto> recentesDto = recentesDomain.stream()
                .map(this::converterParaDto)
                .collect(Collectors.toList());

        return DashboardControleChavesDto.builder()
                .chavesEmCampo(emCampo)
                .chavesNoQuadro(noQuadro)
                .chavesEntregues(entregues)
                .retiradasRecentes(recentesDto)
                .build();
    }

    @Override
    public List<RetiradaControleChavesDto> listarHistorico(String busca, String status, String idObra, int limite, int pagina) {
        List<RequisicaoChave> historico = controleChavesRepository.listarHistorico(busca, status, idObra, limite, pagina);
        return historico.stream()
                .map(this::converterParaDto)
                .collect(Collectors.toList());
    }

    @Override
    public RetiradaControleChavesDto criarRetirada(NovaRetiradaControleChavesForm form) {
        RequisicaoChave criada = controleChavesRepository.criarRetirada(form);
        return converterParaDto(criada);
    }

    @Override
    public RetiradaControleChavesDto receberRetirada(Long id, RecebimentoControleChavesForm form) {
        RequisicaoChave recebida = controleChavesRepository.receberRetirada(id, form);
        return converterParaDto(recebida);
    }

    private RetiradaControleChavesDto converterParaDto(RequisicaoChave domain) {
        if (domain == null) return null;

        ApartamentoControleChavesDto ap = ApartamentoControleChavesDto.builder()
                .id(domain.getIdApartamentoVistoria())
                .label(domain.getNmApartamentoVistoria())
                .build();

        PessoaControleChavesDto retirante = PessoaControleChavesDto.builder()
                .id(domain.getIdUserRetirada())
                .nome(domain.getNmUserRetirada())
                .permissao(domain.getNmPermissaoRetirante())
                .build();

        PessoaControleChavesDto liberador = PessoaControleChavesDto.builder()
                .id(domain.getIdUserLiberacao())
                .nome(domain.getNmUserLiberacao())
                .permissao(domain.getNmPermissaoLiberador())
                .build();

        PessoaControleChavesDto recebedor = null;
        if (domain.getIdUserRecebimento() != null) {
            recebedor = PessoaControleChavesDto.builder()
                    .id(domain.getIdUserRecebimento())
                    .nome(domain.getNmUserRecebimento())
                    .permissao(domain.getNmPermissaoRecebedor())
                    .build();
        }

        return RetiradaControleChavesDto.builder()
                .id(domain.getId())
                .codigo(domain.getCdRetirada())
                .apartamento(ap)
                .retirante(retirante)
                .liberador(liberador)
                .recebedor(recebedor)
                .dataRetirada(domain.getDtRetirada() != null ? domain.getDtRetirada().format(ISO_FORMATTER) : null)
                .dataRecebimento(domain.getDtRecebimento() != null ? domain.getDtRecebimento().format(ISO_FORMATTER) : null)
                .status(domain.getStRequisicao())
                .build();
    }
}
