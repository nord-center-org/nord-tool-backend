package br.com.nord_tool_backend.service;

import br.com.nord_tool_backend.dto.CronogramaSemanalDto;
import br.com.nord_tool_backend.dto.CronogramaSemanalExcecaoDto;
import br.com.nord_tool_backend.form.CronogramaSemanalForm;

import java.time.LocalDate;
import java.util.List;

public interface CronogramaSemanalService {
    CronogramaSemanalDto salvarCronogramaSemanal(CronogramaSemanalForm cronogramaSemanalForm);
    CronogramaSemanalDto alterarCronogramaSemanal(CronogramaSemanalForm cronogramaSemanalForm);
    void deletarCronogramaSemanal(Long id);
    CronogramaSemanalDto buscarPorIdCronogramaSemanal(Long id);
    List<CronogramaSemanalDto> listarCronogramaSemanal();
    List<CronogramaSemanalExcecaoDto> listarExcecoes();
    void incluirExcecao(Long id, LocalDate data);
    void removerExcecao(Long id, LocalDate data);
    CronogramaSemanalDto compartilhar(Long id, List<Long> idsUsuarios);
    List<br.com.nord_tool_backend.dto.PessoaCronogramaDto> listarPessoas();

    /** Move a demanda na esteira (só o status): vale para quem enxerga o item, não só o dono. */
    CronogramaSemanalDto moverStatus(Long id, String status);

    /** A Lugia lê o pedido em texto livre e devolve as demandas montadas (sem salvar: a pessoa confere e cria). */
    br.com.nord_tool_backend.dto.CronogramaInterpretacaoDto interpretar(br.com.nord_tool_backend.form.CronogramaInterpretarForm form);
}
