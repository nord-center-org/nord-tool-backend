package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.domain.ApartamentoVistoria;
import br.com.nord_tool_backend.domain.InfoGeralApartamentoVistoria;
import br.com.nord_tool_backend.domain.OrdenacaoApartamentoVistoria;
import br.com.nord_tool_backend.domain.enums.OrdenacaoApartamentoVistoriaEnum;
import br.com.nord_tool_backend.exception.EntradaInvalidaException;
import br.com.nord_tool_backend.dto.ApartamentoVistoriaDto;
import br.com.nord_tool_backend.dto.ApartamentoVistoriaFiltroDto;
import br.com.nord_tool_backend.dto.InfoGeralApartamentoVistoriaDto;
import br.com.nord_tool_backend.form.ApartamentoVistoriaForm;
import br.com.nord_tool_backend.handler.XlsxExtractorHandlerApartamento;
import br.com.nord_tool_backend.repository.ApartamentoVistoriaHistoricoRepository;
import br.com.nord_tool_backend.repository.ApartamentoVistoriaRepository;
import br.com.nord_tool_backend.service.ApartamentoVistoriaService;
import br.com.nord_tool_backend.service.CacheService;
import br.com.nord_tool_backend.service.TermoReprovaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ApartamentoVistoriaServiceImplTest {

    private ApartamentoVistoriaService apartamentoVistoriaService;

    @Mock
    private ApartamentoVistoriaRepository apartamentoVistoriaRepository;

    @Mock
    private ApartamentoVistoriaHistoricoRepository apartamentoVistoriaHistoricoRepository;

    @Mock
    private CacheService cacheService;

    @Mock
    private XlsxExtractorHandlerApartamento xlsxExtractorHandlerApartamento;

    @Mock
    private TermoReprovaService termoReprovaService;

    ApartamentoVistoria apartamentoVistoria = new ApartamentoVistoria();
    ApartamentoVistoriaDto apartamentoVistoriaDto = new ApartamentoVistoriaDto();
    List<ApartamentoVistoriaDto> lsApartamentoVistoriaDto = new ArrayList<>();
    List<ApartamentoVistoria> lsApartamentoVistoria = new ArrayList<>();
    ApartamentoVistoriaForm apartamentoVistoriaForm = new ApartamentoVistoriaForm();
    ApartamentoVistoriaFiltroDto apartamentoVistoriaFiltroDto = new ApartamentoVistoriaFiltroDto();

    List<InfoGeralApartamentoVistoria> lsInfoGeralApartamentoVistoria = new ArrayList<>();
    List<InfoGeralApartamentoVistoriaDto> lsInfoGeralApartamentoVistoriaDto = new ArrayList<>();


    @BeforeEach
    public void setup(){

        apartamentoVistoriaService = new ApartamentoVistoriaServiceImpl(
                apartamentoVistoriaRepository,
                cacheService,
                apartamentoVistoriaHistoricoRepository,
                xlsxExtractorHandlerApartamento,
                termoReprovaService
        );

        apartamentoVistoria = ApartamentoVistoria.builder()
                .id(1L)
                .nmApartamentoVistoria("nmApartamentoVistoria")
                .idDiaSemana(1)
                .nmDiaSemana("nmDiaSemana")
                .dtApartamentoVigente(LocalDate.now())
                .nmHorarioVistoria("nmHorarioVistoria")
                .idStatusVistoria(1)
                .nmStatusVistoria("Pendente")
                .inMarcarRevistoria(true)
                .txObservacaoRevistoria("txObservacaoRevistoria")
                .dtRevistoriaVigente(LocalDate.now())
                .build();

        apartamentoVistoriaDto = ApartamentoVistoriaDto.builder()
                .idApartamentoVistoria(apartamentoVistoria.getId())
                .nmApartamentoVistoria("nmApartamentoVistoria")
                .idDiaSemana(1)
                .nmDiaSemana("nmDiaSemana")
                .dtApartamentoVigente(LocalDate.now())
                .nmHorarioVistoria("nmHorarioVistoria")
                .idStatusVistoria(1)
                .nmStatusVistoria("nmStatusVistoria")
                .inMarcarRevistoria(true)
                .txObservacaoRevistoria("txObservacaoRevistoria")
                .dtRevistoriaVigente(LocalDate.now())
                .build();

        lsApartamentoVistoria.add(ApartamentoVistoria.builder()
                .nmApartamentoVistoria("nmApartamentoVistoria")
                .idDiaSemana(1)
                .nmDiaSemana("nmDiaSemana")
                .dtApartamentoVigente(LocalDate.now())
                .nmHorarioVistoria("nmHorarioVistoria")
                .idStatusVistoria(1)
                .nmStatusVistoria("nmStatusVistoria")
                .inMarcarRevistoria(true)
                .txObservacaoRevistoria("txObservacaoRevistoria")
                .dtRevistoriaVigente(LocalDate.now())
                .build());

        lsApartamentoVistoriaDto.add(ApartamentoVistoriaDto.builder()
                .nmApartamentoVistoria("nmApartamentoVistoria")
                .idDiaSemana(1)
                .nmDiaSemana("nmDiaSemana")
                .dtApartamentoVigente(LocalDate.now())
                .nmHorarioVistoria("nmHorarioVistoria")
                .idStatusVistoria(1)
                .nmStatusVistoria("nmStatusVistoria")
                .inMarcarRevistoria(true)
                .txObservacaoRevistoria("txObservacaoRevistoria")
                .dtRevistoriaVigente(LocalDate.now())
                .build());

        apartamentoVistoriaForm = ApartamentoVistoriaForm.builder()
                .idApartamentoVistoria(1L)
                .nmApartamentoVistoria("nmApartamentoVistoria")
                .idDiaSemana(1)
                .dtApartamentoVigente(LocalDate.now())
                .nmHorarioVistoria("nmHorarioVistoria")
                .idStatusVistoria(1)
                .nmStatusVistoria("nmStatusVistoria")
                .inMarcarRevistoria(true)
                .txObservacaoRevistoria("txObservacaoRevistoria")
                .dtRevistoriaVigente(LocalDate.now())
                .build();

        apartamentoVistoriaFiltroDto = ApartamentoVistoriaFiltroDto.builder()
                .nmApartamentoVistoria("nmApartamentoVistoria")
                .nmDiaSemana("nmDiaSemana")
                .dtApartamentoVigente("DataFiltrada")
                .nmHorarioVistoria("nmHorarioVistoria")
                .nmStatusVistoria("nmStatusVistoria")
                .txObservacaoRevistoria("txObservacaoRevistoria")
                .dtRevistoriaVigente("DataFiltrada")
                .build();

        lsInfoGeralApartamentoVistoria.add(InfoGeralApartamentoVistoria.builder()
                .nmStatusVistoria("Liberado")
                .qtApartamentoStatusVistoria(30)
                .pcApartamentoStatusVistoria(20.0)
                .nrTotalRegistros(2000)
                .build());

        lsInfoGeralApartamentoVistoriaDto.add(InfoGeralApartamentoVistoriaDto.builder()
                .nmStatusVistoria("Liberado")
                .qtApartamentoStatusVistoria(30)
                .pcApartamentoStatusVistoria(20.0)
                .nrTotalRegistros(2000)
                .build());
    }

    @Test
    void deveSalvarApartamentoVistoria() {
        when(apartamentoVistoriaRepository.salvarApartamentoVistoria(any(ApartamentoVistoria.class))).thenReturn(apartamentoVistoriaDto);
        ApartamentoVistoriaDto result = apartamentoVistoriaService.salvarApartamentoVistoria(apartamentoVistoriaForm);
        assertEquals(apartamentoVistoriaDto, result);
        verify(apartamentoVistoriaRepository).salvarApartamentoVistoria(any(ApartamentoVistoria.class));
        verify(cacheService, times(1)).limparTodos();
    }

    @Test
    void deveAlterarApartamentoVistoria() {
        when(apartamentoVistoriaRepository.buscarApartamentoVistoria(anyLong())).thenReturn(apartamentoVistoria);
        when(apartamentoVistoriaHistoricoRepository.buscarNrVersaoHistorico(anyLong())).thenReturn(List.of(1));
        when(apartamentoVistoriaRepository.alterarApartamentoVistoria(any(ApartamentoVistoria.class))).thenReturn(apartamentoVistoriaDto);
        ApartamentoVistoriaDto result = apartamentoVistoriaService.alterarApartamentoVistoria(apartamentoVistoriaForm);
        assertEquals(apartamentoVistoriaDto, result);
        verify(apartamentoVistoriaRepository).buscarApartamentoVistoria(anyLong());
        verify(apartamentoVistoriaHistoricoRepository).buscarNrVersaoHistorico(anyLong());
        verify(apartamentoVistoriaHistoricoRepository).salvarTodosHistoricos(anyList());
        verify(apartamentoVistoriaRepository).alterarApartamentoVistoria(any(ApartamentoVistoria.class));
        verify(cacheService, times(1)).limparTodos();
    }

    @Test
    void deveDeletarApartamentoVistoria(){
        doNothing().when(apartamentoVistoriaRepository).deletarApartamentoVistoria(anyLong());
        apartamentoVistoriaService.deletarApartamentoVistoria(anyLong());
        verify(termoReprovaService, times(1)).apagarPorApartamento(anyLong());
        verify(apartamentoVistoriaRepository, times(1)).deletarApartamentoVistoria(anyLong());
        verify(cacheService, times(1)).limparTodos();
    }

    @Test
    void deveRetornarUmApartamentoVistoria(){
        when(apartamentoVistoriaRepository.buscarApartamentoVistoria(anyLong())).thenReturn(apartamentoVistoria);
        apartamentoVistoriaService.buscarApartamentoVistoria(anyLong());
        verify(apartamentoVistoriaRepository, times(1)).buscarApartamentoVistoria(anyLong());
    }

    @Test
    void deveRetornarUmaListaApartamentoVistoria(){
        when(apartamentoVistoriaRepository.listarApartamentoVistoria()).thenReturn(lsApartamentoVistoria);
        apartamentoVistoriaService.listarApartamentoVistoria();
        verify(apartamentoVistoriaRepository, times(1)).listarApartamentoVistoria();
    }

    @Test
    void deveImportarApartamentoVistoria() throws Exception {
        // Arrange
        MockMultipartFile planilhaFile = new MockMultipartFile("arquivo", "arquivo.xlsx", MediaType.MULTIPART_FORM_DATA_VALUE, "conteudo".getBytes());
        doNothing().when(xlsxExtractorHandlerApartamento).init(any());
        // Act
        apartamentoVistoriaService.importarPlanilha(planilhaFile);
        // Assert
        verify(xlsxExtractorHandlerApartamento, times(1)).init(planilhaFile);
        verify(cacheService, times(1)).limparTodos();
    }

    @Test
    void deveBuscarApartamentoVistoriaFiltroVazio() {
        String filtraTodos = "";
        int nrPagina = 0;
        int nrQuantidadePorPagina = 20;
        String nmOrdem = "";

        when(apartamentoVistoriaRepository.listarApartamentoVistoriaFiltrado(Mockito.eq(apartamentoVistoriaFiltroDto), Mockito.eq(filtraTodos), Mockito.eq(nrPagina), Mockito.eq(nrQuantidadePorPagina), Mockito.any()))
                .thenReturn(lsApartamentoVistoriaDto);

        apartamentoVistoriaService.listarApartamentoVistoriaFiltrado(apartamentoVistoriaFiltroDto, filtraTodos, nrPagina, nrQuantidadePorPagina, nmOrdem);
        verify(apartamentoVistoriaRepository, times(1))
                .listarApartamentoVistoriaFiltrado(Mockito.eq(apartamentoVistoriaFiltroDto), Mockito.eq(filtraTodos), Mockito.eq(nrPagina), Mockito.eq(nrQuantidadePorPagina), Mockito.any()
        );
    }

    @Test
    void deveBuscarApartamentoVistoriaFiltroNulo() {
        String filtraTodos = null;
        int nrPagina = 0;
        int nrQuantidadePorPagina = 20;
        String nmOrdem = null;

        when(apartamentoVistoriaRepository.listarApartamentoVistoriaFiltrado(Mockito.eq(apartamentoVistoriaFiltroDto), Mockito.eq(filtraTodos), Mockito.eq(nrPagina), Mockito.eq(nrQuantidadePorPagina), Mockito.any()))
                .thenReturn(lsApartamentoVistoriaDto);

        apartamentoVistoriaService.listarApartamentoVistoriaFiltrado(apartamentoVistoriaFiltroDto, filtraTodos, nrPagina, nrQuantidadePorPagina, nmOrdem);
        verify(apartamentoVistoriaRepository, times(1))
                .listarApartamentoVistoriaFiltrado(Mockito.eq(apartamentoVistoriaFiltroDto), Mockito.eq(filtraTodos), Mockito.eq(nrPagina), Mockito.eq(nrQuantidadePorPagina), Mockito.any());
    }

    @Test
    void deveBuscarApartamentoVistoriaFiltrandoTodos() {
        String filtraTodos = "FiltrandoTodos";
        int nrPagina = 0;
        int nrQuantidadePorPagina = 20;
        String nmOrdem = "ASC";

        when(apartamentoVistoriaRepository.listarApartamentoVistoriaFiltrado(Mockito.eq(apartamentoVistoriaFiltroDto), Mockito.eq(filtraTodos), Mockito.eq(nrPagina), Mockito.eq(nrQuantidadePorPagina), Mockito.any()))
                .thenReturn(lsApartamentoVistoriaDto);

        apartamentoVistoriaService.listarApartamentoVistoriaFiltrado(apartamentoVistoriaFiltroDto, filtraTodos, nrPagina, nrQuantidadePorPagina, nmOrdem);
        verify(apartamentoVistoriaRepository, times(1))
                .listarApartamentoVistoriaFiltrado(Mockito.eq(apartamentoVistoriaFiltroDto), Mockito.eq(filtraTodos), Mockito.eq(nrPagina), Mockito.eq(nrQuantidadePorPagina), Mockito.any());
    }

    @Test
    void deveBuscarTodasInfoGeralApartamentoVistoria() {
        String dtInicio = "DataInicioFiltrada";
        String dtFim = "DataFimFiltrada";

        when(apartamentoVistoriaRepository.listarInfoGeralApartamentoVistoria(dtInicio,dtFim)).thenReturn((lsInfoGeralApartamentoVistoria));
        List<InfoGeralApartamentoVistoriaDto> lsInfoGeralApartamentoVistoriaDtoTest = apartamentoVistoriaService.listarInfoGeralApartamentoVistoria(dtInicio,dtFim);
        assertEquals(lsInfoGeralApartamentoVistoriaDto, lsInfoGeralApartamentoVistoriaDtoTest);
        verify(apartamentoVistoriaRepository, times(1)).listarInfoGeralApartamentoVistoria(Mockito.eq(dtInicio), Mockito.eq(dtFim));
    }

    @Test
    void ordenacaoComTextoArbitrarioSoUsaColunaEDirecaoPermitidas() {
        apartamentoVistoriaService.listarApartamentoVistoriaFiltrado(apartamentoVistoriaFiltroDto, null, 0, 20,
                "nmDiaSemana,ASC; DROP TABLE apartamento_vistoria");
        verify(apartamentoVistoriaRepository).listarApartamentoVistoriaFiltrado(apartamentoVistoriaFiltroDto, null, 0, 20,
                new OrdenacaoApartamentoVistoria(OrdenacaoApartamentoVistoriaEnum.nm_dia_semana, false));

        apartamentoVistoriaService.listarApartamentoVistoriaFiltrado(apartamentoVistoriaFiltroDto, null, 0, 20,
                "id_apartamento_vistoria; DELETE FROM usuario,DESC");
        verify(apartamentoVistoriaRepository).listarApartamentoVistoriaFiltrado(apartamentoVistoriaFiltroDto, null, 0, 20,
                new OrdenacaoApartamentoVistoria(OrdenacaoApartamentoVistoriaEnum.id_apartamento_vistoria, true));
    }

    @Test
    void ordenacaoAusenteUsaOPadrao() {
        apartamentoVistoriaService.listarApartamentoVistoriaFiltrado(apartamentoVistoriaFiltroDto, null, 0, 20, null);
        verify(apartamentoVistoriaRepository).listarApartamentoVistoriaFiltrado(apartamentoVistoriaFiltroDto, null, 0, 20,
                OrdenacaoApartamentoVistoria.PADRAO);
        assertEquals(" ORDER BY id_apartamento_vistoria DESC ", OrdenacaoApartamentoVistoria.PADRAO.sql());
    }

    @Test
    void paginacaoForaDosLimitesENegada() {
        assertThrows(EntradaInvalidaException.class, () ->
                apartamentoVistoriaService.listarApartamentoVistoriaFiltrado(apartamentoVistoriaFiltroDto, null, -1, 20, null));
        assertThrows(EntradaInvalidaException.class, () ->
                apartamentoVistoriaService.listarApartamentoVistoriaFiltrado(apartamentoVistoriaFiltroDto, null, 0, 0, null));
        assertThrows(EntradaInvalidaException.class, () ->
                apartamentoVistoriaService.listarApartamentoVistoriaFiltrado(apartamentoVistoriaFiltroDto, null, 0, 101, null));
        verifyNoInteractions(apartamentoVistoriaRepository);
    }
}
