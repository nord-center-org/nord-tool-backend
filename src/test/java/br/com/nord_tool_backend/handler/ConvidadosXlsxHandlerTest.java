package br.com.nord_tool_backend.handler;

import br.com.nord_tool_backend.exception.NordException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ConvidadosXlsxHandlerTest {

    private final ConvidadosXlsxHandler handler = new ConvidadosXlsxHandler();

    /** Gera um .xlsx real; cada String[] é uma linha (null = célula vazia). */
    private byte[] planilha(String[]... linhas) throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet aba = wb.createSheet("Convidados");
            for (int i = 0; i < linhas.length; i++) {
                Row row = aba.createRow(i);
                for (int c = 0; c < linhas[i].length; c++) {
                    if (linhas[i][c] != null) row.createCell(c).setCellValue(linhas[i][c]);
                }
            }
            wb.write(out);
            return out.toByteArray();
        }
    }

    private static final String[] CABECALHO = {"Nome", "Grupo", "Telefone", "Relação", "Status", "Acompanhantes", "Mesa"};

    @Test
    void leLinhasValidasComStatusPorRotuloECodigo() throws Exception {
        byte[] bytes = planilha(CABECALHO,
                new String[]{"Ana Souza", "Família da noiva", "11 99999-0000", "Prima", "Confirmado", "2", "5"},
                new String[]{"Bruno", "Trabalho", null, null, "NAO_IRA", null, null},
                new String[]{"Carla", null, null, null, null, null, null});

        List<ConvidadosXlsxHandler.LinhaLida> linhas = handler.ler(bytes);

        assertEquals(3, linhas.size());
        assertTrue(linhas.stream().allMatch(ConvidadosXlsxHandler.LinhaLida::valida));
        assertEquals("CONFIRMADO", linhas.get(0).getForm().getNmStatus());
        assertEquals(2, linhas.get(0).getForm().getNrAcompanhantes());
        assertEquals("5", linhas.get(0).getForm().getNmMesa());
        assertEquals("NAO_IRA", linhas.get(1).getForm().getNmStatus());
        assertEquals("NAO_CONVIDADO", linhas.get(2).getForm().getNmStatus());
        assertEquals(0, linhas.get(2).getForm().getNrAcompanhantes());
        assertNull(linhas.get(2).getForm().getNmGrupo());
    }

    @Test
    void cabecalhoSemAcentoOuCaixaEOrdemDiferenteFunciona() throws Exception {
        byte[] bytes = planilha(
                new String[]{"STATUS", "convidado", "RELACAO", "acompanhante"},
                new String[]{"Convidado", "Dora", "Amiga", "1"});

        ConvidadosXlsxHandler.LinhaLida linha = handler.ler(bytes).get(0);

        assertTrue(linha.valida());
        assertEquals("Dora", linha.getForm().getNmConvidado());
        assertEquals("CONVIDADO", linha.getForm().getNmStatus());
        assertEquals("Amiga", linha.getForm().getNmRelacao());
        assertEquals(1, linha.getForm().getNrAcompanhantes());
    }

    @Test
    void linhasInvalidasViramRejeicoesComONumeroDaLinhaSemPararAsDemais() throws Exception {
        byte[] bytes = planilha(CABECALHO,
                new String[]{"Ok", null, null, null, "Convidado", null, null},        // linha 2
                new String[]{null, "Grupo", null, null, null, null, null},            // linha 3: sem nome
                new String[]{"Status ruim", null, null, null, "Talvez", null, null},  // linha 4
                new String[]{"Acomp ruim", null, null, null, null, "dois", null},     // linha 5
                new String[]{"Acomp neg", null, null, null, null, "-1", null},        // linha 6
                new String[]{"Acomp frac", null, null, null, null, "1.5", null},      // linha 7
                new String[]{"Segundo ok", null, null, null, null, "3.0", null});     // linha 8

        List<ConvidadosXlsxHandler.LinhaLida> linhas = handler.ler(bytes);

        assertEquals(7, linhas.size());
        assertTrue(linhas.get(0).valida());
        assertEquals(3, linhas.get(1).getLinha());
        assertEquals("Nome é obrigatório", linhas.get(1).getErro());
        assertTrue(linhas.get(2).getErro().startsWith("Status inválido: Talvez"));
        assertTrue(linhas.get(3).getErro().startsWith("Acompanhantes inválido"));
        assertTrue(linhas.get(4).getErro().contains("entre 0 e 50"));
        assertTrue(linhas.get(5).getErro().contains("inteiro"));
        assertTrue(linhas.get(6).valida());
        assertEquals(3, linhas.get(6).getForm().getNrAcompanhantes());
        assertEquals(8, linhas.get(6).getLinha());
    }

    @Test
    void ignoraLinhasTotalmenteVazias() throws Exception {
        byte[] bytes = planilha(CABECALHO, new String[]{}, new String[]{"Zé", null, null, null, null, null, null});

        List<ConvidadosXlsxHandler.LinhaLida> linhas = handler.ler(bytes);

        assertEquals(1, linhas.size());
        assertEquals("Zé", linhas.get(0).getForm().getNmConvidado());
    }

    @Test
    void campoLongoDemaisEhRejeitado() throws Exception {
        String nomeLongo = new String(new char[151]).replace('\0', 'a');
        byte[] bytes = planilha(CABECALHO, new String[]{nomeLongo, null, null, null, null, null, null});

        assertTrue(handler.ler(bytes).get(0).getErro().contains("150"));
    }

    @Test
    void planilhaSemColunaNomeOuQueNaoEhXlsxEhRecusada() throws Exception {
        assertThrows(NordException.class, () -> handler.ler(planilha(new String[]{"Grupo", "Mesa"}, new String[]{"x", "y"})));
        assertThrows(NordException.class, () -> handler.ler("isto não é uma planilha".getBytes()));
    }
}
