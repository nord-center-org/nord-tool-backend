package br.com.nord_tool_backend.service;

import br.com.nord_tool_backend.dto.FinanceiroConfiguracaoDto;
import br.com.nord_tool_backend.dto.FinanceiroFaturaLeituraDto;
import br.com.nord_tool_backend.dto.FinanceiroFechamentoDto;
import br.com.nord_tool_backend.dto.FinanceiroGeracaoDto;
import br.com.nord_tool_backend.dto.FinanceiroProjecaoMesDto;
import br.com.nord_tool_backend.dto.FinanceiroRecorrenciaDto;
import br.com.nord_tool_backend.form.FinanceiroConfiguracaoForm;
import br.com.nord_tool_backend.form.FinanceiroRecorrenciaForm;
import br.com.nord_tool_backend.form.FinanceiroSaldoInicialForm;

import java.util.List;

/** A conta do mês: saldo anterior, projeção das entradas e saídas, fechamento e lançamentos fixos. */
public interface FinanceiroProjecaoService {

    /**
     * A conta do mês (yyyy-MM). Mês fechado mostra o que foi gravado; mês atual ou futuro soma estimativas ao
     * que já foi lançado. Com {@code idPessoa} não há saldo anterior (ele é da conta toda).
     */
    FinanceiroProjecaoMesDto obterMes(String competencia, Long idPessoa);

    /** Define (ou, com valor vazio, remove) o saldo inicial do mês. Mês fechado → 400. */
    FinanceiroProjecaoMesDto definirSaldoInicial(String competencia, FinanceiroSaldoInicialForm form);

    /**
     * Fecha o mês: grava o saldo final real (só o que foi lançado), trava os lançamentos do mês e gera os
     * lançamentos fixos do mês seguinte. Exige o mês anterior fechado, salvo no primeiro mês ou com saldo inicial.
     */
    FinanceiroFechamentoDto fechar(String competencia, Long idUsuario);

    /** Reabre o último mês fechado (não pode haver mês posterior fechado). */
    FinanceiroProjecaoMesDto reabrir(String competencia);

    FinanceiroConfiguracaoDto obterConfiguracao();

    FinanceiroConfiguracaoDto atualizarConfiguracao(FinanceiroConfiguracaoForm form);

    /** Evolução do valor parcial de uma fatura ao longo do ciclo. */
    List<FinanceiroFaturaLeituraDto> listarLeituras(Long idLancamento);

    List<FinanceiroRecorrenciaDto> listarRecorrencias();

    FinanceiroRecorrenciaDto criarRecorrencia(FinanceiroRecorrenciaForm form);

    /** Substitui os dados da recorrência; exige nrVersao (divergente → 409). */
    FinanceiroRecorrenciaDto atualizarRecorrencia(Long id, FinanceiroRecorrenciaForm form);

    /** Cria os lançamentos das recorrências vigentes no mês que ainda não existem. Mês fechado → 400. */
    FinanceiroGeracaoDto gerarRecorrencias(String competencia, Long idUsuario);
}
