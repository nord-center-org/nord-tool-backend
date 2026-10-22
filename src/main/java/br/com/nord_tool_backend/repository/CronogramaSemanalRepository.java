package br.com.nord_tool_backend.repository;

import br.com.nord_tool_backend.domain.CronogramaSemanal;
import br.com.nord_tool_backend.dto.CronogramaSemanalDto;
import br.com.nord_tool_backend.dto.CronogramaSemanalExcecaoDto;

import java.time.LocalDate;
import java.util.List;

public interface CronogramaSemanalRepository {
    CronogramaSemanalDto salvarCronogramaSemanal(CronogramaSemanal cronogramaSemanal);
    CronogramaSemanalDto alterarCronogramaSemanal(CronogramaSemanal cronogramaSemanal);
    void deletarCronogramaSemanal(Long id);
    CronogramaSemanal buscarPorIdCronogramaSemanal(Long id);
    List<CronogramaSemanal> listarCronogramaSemanal();
    List<CronogramaSemanalExcecaoDto> listarExcecoes();
    void incluirExcecao(Long id, LocalDate data);
    void removerExcecao(Long id, LocalDate data);
    /** Substitui com quem o item está compartilhado. */
    void compartilhar(Long id, List<Long> idsUsuarios);
    List<br.com.nord_tool_backend.dto.PessoaCronogramaDto> listarPessoas();
    int contarUsuariosAtivos(java.util.Collection<Long> ids);
}
