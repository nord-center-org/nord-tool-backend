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
}
