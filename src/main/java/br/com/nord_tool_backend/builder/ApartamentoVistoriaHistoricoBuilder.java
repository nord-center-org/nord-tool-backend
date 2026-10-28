package br.com.nord_tool_backend.builder;

import br.com.nord_tool_backend.domain.ApartamentoVistoria;
import br.com.nord_tool_backend.domain.ApartamentoVistoriaHistorico;
import br.com.nord_tool_backend.domain.ApartamentoVistoriaHistoricoCustom;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ApartamentoVistoriaHistoricoBuilder {

    public static List<ApartamentoVistoriaHistorico> gerarHistorico(ApartamentoVistoria apartamentoVistoriaAnterior,
                                                              ApartamentoVistoria apartamentoVistoriaAtual, List<Integer> lsNrVersaoHistorico) {

        List<ApartamentoVistoriaHistoricoCustom> lsApVistoriaAlterado =
                comparar(apartamentoVistoriaAnterior, apartamentoVistoriaAtual);

        List<ApartamentoVistoriaHistorico> lsApVistoriaHistorico = lsApVistoriaAlterado.stream()
                .map(alterado -> criarHistorico(
                        apartamentoVistoriaAnterior.getId(),
                        lsNrVersaoHistorico,
                        alterado.getNmAtributo(),
                        alterado.getTxAnterior(),
                        alterado.getTxAtual()
                ))
                .collect(Collectors.toList());

        return lsApVistoriaHistorico;
    }

    private static ApartamentoVistoriaHistorico criarHistorico(Long idApartamentoVistoria, List<Integer> lsNrVersaoHistorico, String nmAtributo, String txAnterior, String txAtual) {

        ApartamentoVistoriaHistorico apVistoriaHistorico = new ApartamentoVistoriaHistorico();

        apVistoriaHistorico.setIdApartamentoVistoria(idApartamentoVistoria);
        apVistoriaHistorico.setNrVersao(lsNrVersaoHistorico.stream().max(Integer::compareTo).map(versao -> versao + 1)
                .orElse(1));
        apVistoriaHistorico.setNmAtributo(nmAtributo);
        apVistoriaHistorico.setTxAnterior(txAnterior);
        apVistoriaHistorico.setTxAtual(txAtual);
        return apVistoriaHistorico;
    }

    public static List<ApartamentoVistoriaHistoricoCustom> comparar(ApartamentoVistoria anterior, ApartamentoVistoria atual) {

        List<ApartamentoVistoriaHistoricoCustom> lsApVistoriaAlterado = new ArrayList<>();

        add(lsApVistoriaAlterado, "Nome do Apartamento", anterior.getNmApartamentoVistoria(), atual.getNmApartamentoVistoria());
        add(lsApVistoriaAlterado, "Dia da Semana", anterior.getNmDiaSemana(), atual.getNmDiaSemana());
        add(lsApVistoriaAlterado, "Data Vigente", anterior.getDtApartamentoVigente(), atual.getDtApartamentoVigente());
        add(lsApVistoriaAlterado, "Horario da Vistoria", anterior.getNmHorarioVistoria(), atual.getNmHorarioVistoria());
        add(lsApVistoriaAlterado, "Status da Vistoria", anterior.getNmStatusVistoria(), atual.getNmStatusVistoria());
        add(lsApVistoriaAlterado, "Marcação da Revistoria", anterior.isInMarcarRevistoria(), atual.isInMarcarRevistoria());
        add(lsApVistoriaAlterado, "Observação da Revistoria", anterior.getTxObservacaoRevistoria(), atual.getTxObservacaoRevistoria());
        add(lsApVistoriaAlterado, "Data da Revistoria", anterior.getDtRevistoriaVigente(), atual.getDtRevistoriaVigente());
        return lsApVistoriaAlterado;
    }

    private static void add(List<ApartamentoVistoriaHistoricoCustom> lsApHistoricoCustom, String nmAtributo, Object txAnterior, Object txAtual) {
        if (!Objects.equals(txAnterior, txAtual)) {
            lsApHistoricoCustom.add(new ApartamentoVistoriaHistoricoCustom(
                    nmAtributo,
                    txAnterior != null ? txAnterior.toString() : null,
                    txAtual != null ? txAtual.toString() : null
            ));
        }
    }
}
