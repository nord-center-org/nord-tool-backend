package br.com.nord_tool_backend.dto;

import br.com.nord_tool_backend.domain.ApartamentoVistoria;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ApartamentoVistoriaDtoTest {

    @Test
    void converteOsDadosDoUltimoTermo() {
        ApartamentoVistoria apt = ApartamentoVistoria.builder()
                .id(1L)
                .nmApartamentoVistoria("EN-02-1307")
                .inTermoAnexado(true)
                .qtTermos(2)
                .nrUltimoTermo(2)
                .nmSituacaoTermo("EM_ANDAMENTO")
                .qtFotosTermo(3)
                .nrPaginasTermo(4)
                .nrPaginasComFoto(2)
                .build();

        ApartamentoVistoriaDto dto = ApartamentoVistoriaDto.converterToDto(apt);

        assertTrue(dto.isInTermoAnexado());
        assertEquals(2, dto.getQtTermos());
        assertEquals(2, dto.getNrUltimoTermo());
        assertEquals("EM_ANDAMENTO", dto.getNmSituacaoTermo());
        assertEquals(3, dto.getQtFotosTermo());
        assertEquals(4, dto.getNrPaginasTermo());
        assertEquals(2, dto.getNrPaginasComFoto());
    }

    @Test
    void semTermoMantemOsCamposVazios() {
        ApartamentoVistoriaDto dto = ApartamentoVistoriaDto.converterToDto(
                ApartamentoVistoria.builder().id(2L).nmApartamentoVistoria("N1-01-0101").build());

        assertFalse(dto.isInTermoAnexado());
        assertNull(dto.getNrUltimoTermo());
        assertNull(dto.getNmSituacaoTermo());
        assertNull(dto.getNrPaginasTermo());
    }
}
