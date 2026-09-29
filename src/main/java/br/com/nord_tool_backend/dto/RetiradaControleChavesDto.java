package br.com.nord_tool_backend.dto;

import br.com.nord_tool_backend.domain.RequisicaoChaveConsulta;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.format.DateTimeFormatter;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RetiradaControleChavesDto {
    private Long idRequisicao;
    private String cdCodigoRetirada;
    private ApartamentoControleChavesDto apartamentoControleChavesDto;
    private RetiranteControleChavesDto retiranteControleChavesDto;
    private LiberadorControleChavesDto liberadorControleChavesDto;
    private RecebedorControleChavesDto recebedorControleChavesDto;
    private String dtRetirada;
    private String dtRecebimento;
    private String nmStatusRetiradaControle;

    public static RetiradaControleChavesDto converterToDto(RequisicaoChaveConsulta requisicaoChaveConsulta) {
        if (requisicaoChaveConsulta == null) return null;

        ApartamentoControleChavesDto apartamentoControleChavesDto = ApartamentoControleChavesDto.builder()
                .idApartamentoVistoria(requisicaoChaveConsulta.getIdApartamentoVistoria())
                .nmApartamentoVistoria(requisicaoChaveConsulta.getNmApartamentoVistoria())
                .build();

        RetiranteControleChavesDto retiranteControleChavesDto = RetiranteControleChavesDto.builder()
                .idUserRetirada(requisicaoChaveConsulta.getIdUserRetirada())
                .nmPessoaRetirante(requisicaoChaveConsulta.getNmPessoaRetirante())
                .nmPermissaoRetirante(requisicaoChaveConsulta.getNmPermissaoRetirante())
                .build();

        LiberadorControleChavesDto liberadorControleChavesDto = LiberadorControleChavesDto.builder()
                .idUserLiberacao(requisicaoChaveConsulta.getIdUserLiberacao())
                .nmPessoaLiberador(requisicaoChaveConsulta.getNmPessoaLiberador())
                .nmPermissaoLiberador(requisicaoChaveConsulta.getNmPermissaoLiberador())
                .build();

        RecebedorControleChavesDto recebedorControleChavesDto = null;
        if (requisicaoChaveConsulta.getIdUserRecebimento() != null) {
            recebedorControleChavesDto = RecebedorControleChavesDto.builder()
                    .idUserRecebimento(requisicaoChaveConsulta.getIdUserRecebimento())
                    .nmPessoaRecebedor(requisicaoChaveConsulta.getNmPessoaRecebedor())
                    .nmPermissaoRecebedor(requisicaoChaveConsulta.getNmPermissaoRecebedor())
                    .build();
        }

        return RetiradaControleChavesDto.builder()
                .idRequisicao(requisicaoChaveConsulta.getIdRequisicao())
                .cdCodigoRetirada(requisicaoChaveConsulta.getCdRetirada())
                .apartamentoControleChavesDto(apartamentoControleChavesDto)
                .retiranteControleChavesDto(retiranteControleChavesDto)
                .liberadorControleChavesDto(liberadorControleChavesDto)
                .recebedorControleChavesDto(recebedorControleChavesDto)
                .dtRetirada(requisicaoChaveConsulta.getDtRetirada() != null
                        ? requisicaoChaveConsulta.getDtRetirada().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null)
                .dtRecebimento(requisicaoChaveConsulta.getDtRecebimento() != null
                        ? requisicaoChaveConsulta.getDtRecebimento().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null)
                .nmStatusRetiradaControle(requisicaoChaveConsulta.getNmStatusRequisicao())
                .build();
    }
}
