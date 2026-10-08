package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.domain.CasamentoConvidado;
import br.com.nord_tool_backend.dto.ImportacaoConvidadosDto;
import br.com.nord_tool_backend.excepetion.ValidacaoException;
import br.com.nord_tool_backend.form.CasamentoConvidadoForm;
import br.com.nord_tool_backend.handler.ConvidadosXlsxHandler;
import br.com.nord_tool_backend.repository.CasamentoRepository;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.ByteArrayOutputStream;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CasamentoConvidadoServiceImplTest {

    private CasamentoRepository repository;
    private CasamentoConvidadoServiceImpl convidados;

    @BeforeEach
    void setUp() {
        repository = mock(CasamentoRepository.class);
        convidados = new CasamentoConvidadoServiceImpl(repository, new ConvidadosXlsxHandler());
    }

    private CasamentoConvidado convidado(long id) {
        CasamentoConvidado c = new CasamentoConvidado();
        c.setId(id);
        c.setNmConvidado("Ana");
        c.setNmStatus("CONVIDADO");
        c.setNrAcompanhantes(1);
        return c;
    }

    private CasamentoConvidadoForm form(String nome, String status, Integer acompanhantes, String mesa) {
        CasamentoConvidadoForm f = new CasamentoConvidadoForm();
        f.setNmConvidado(nome);
        f.setNmStatus(status);
        f.setNrAcompanhantes(acompanhantes);
        f.setNmMesa(mesa);
        return f;
    }

    @Test
    void criaConvidadoComPadroesMesaEAcompanhantes() {
        when(repository.inserirConvidado(any())).thenReturn(5L);
        when(repository.buscarConvidado(5L)).thenReturn(Optional.of(convidado(5)));

        convidados.criar(form("  Ana Souza ", null, null, " 12 "));

        ArgumentCaptor<CasamentoConvidado> captor = ArgumentCaptor.forClass(CasamentoConvidado.class);
        verify(repository).inserirConvidado(captor.capture());
        assertEquals("Ana Souza", captor.getValue().getNmConvidado());
        assertEquals("NAO_CONVIDADO", captor.getValue().getNmStatus());
        assertEquals(0, captor.getValue().getNrAcompanhantes());
        assertEquals("12", captor.getValue().getNmMesa());
    }

    @Test
    void aceitaStatusPeloRotulo() {
        when(repository.inserirConvidado(any())).thenReturn(5L);
        when(repository.buscarConvidado(5L)).thenReturn(Optional.of(convidado(5)));

        convidados.criar(form("A", "Não irá", 0, null));

        ArgumentCaptor<CasamentoConvidado> captor = ArgumentCaptor.forClass(CasamentoConvidado.class);
        verify(repository).inserirConvidado(captor.capture());
        assertEquals("NAO_IRA", captor.getValue().getNmStatus());
    }

    @Test
    void recusaStatusInvalidoEConvidadoInexistente() {
        assertThrows(ValidacaoException.class, () -> convidados.criar(form("A", "TALVEZ", 0, null)));
        when(repository.buscarConvidado(9L)).thenReturn(Optional.empty());
        assertEquals(404, assertThrows(ValidacaoException.class, () -> convidados.deletar(9L)).getHttpEnum().getStatus().value());
        assertEquals(404, assertThrows(ValidacaoException.class, () -> convidados.alterar(9L, form("A", null, 0, null))).getHttpEnum().getStatus().value());
    }

    private byte[] xlsx(String[]... linhas) throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            var aba = wb.createSheet();
            for (int i = 0; i < linhas.length; i++) {
                var row = aba.createRow(i);
                for (int c = 0; c < linhas[i].length; c++) if (linhas[i][c] != null) row.createCell(c).setCellValue(linhas[i][c]);
            }
            wb.write(out);
            return out.toByteArray();
        }
    }

    @Test
    void importaValidasERelataRejeitadasSemPararAsDemais() throws Exception {
        byte[] bytes = xlsx(new String[]{"Nome", "Status", "Acompanhantes"},
                new String[]{"Ana", "Confirmado", "2"},
                new String[]{null, "Convidado", null},
                new String[]{"Beto", "Talvez", null},
                new String[]{"Carla", "Convidado", "0"});
        when(repository.inserirConvidado(any())).thenReturn(1L);

        ImportacaoConvidadosDto relatorio = convidados.importar("convidados.xlsx", bytes);

        assertEquals(2, relatorio.getImportados());
        assertEquals(2, relatorio.getRejeitados().size());
        assertEquals(3, relatorio.getRejeitados().get(0).getLinha());
        assertEquals("Nome é obrigatório", relatorio.getRejeitados().get(0).getMotivo());
        assertEquals(4, relatorio.getRejeitados().get(1).getLinha());
        verify(repository, times(2)).inserirConvidado(any());
    }

    @Test
    void importacaoRecusaArquivoVazioExtensaoErradaOuGrande() {
        assertThrows(ValidacaoException.class, () -> convidados.importar("a.xlsx", new byte[0]));
        assertThrows(ValidacaoException.class, () -> convidados.importar("a.csv", new byte[]{1, 2, 3}));
        assertThrows(ValidacaoException.class, () -> convidados.importar(null, new byte[]{1, 2, 3}));
        assertThrows(ValidacaoException.class, () -> convidados.importar("a.xlsx", new byte[CasamentoConvidadoServiceImpl.MAX_BYTES_PLANILHA + 1]));
        verify(repository, never()).inserirConvidado(any());
    }
}
