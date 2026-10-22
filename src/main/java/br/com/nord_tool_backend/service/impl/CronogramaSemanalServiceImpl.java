package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.security.Acao;
import br.com.nord_tool_backend.security.Modulo;
import br.com.nord_tool_backend.service.AutorizacaoService;
import br.com.nord_tool_backend.exception.EntradaInvalidaException;
import br.com.nord_tool_backend.domain.CronogramaSemanal;
import br.com.nord_tool_backend.dto.CronogramaSemanalDto;
import br.com.nord_tool_backend.dto.CronogramaSemanalExcecaoDto;
import br.com.nord_tool_backend.form.CronogramaSemanalForm;
import br.com.nord_tool_backend.repository.CronogramaSemanalRepository;
import br.com.nord_tool_backend.service.CronogramaSemanalService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CronogramaSemanalServiceImpl implements CronogramaSemanalService {
    private final Logger log = LogManager.getLogger(CronogramaSemanalServiceImpl.class);

    private final CronogramaSemanalRepository cronogramaSemanalRepository;


    private final AutorizacaoService autorizacao;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CronogramaSemanalDto salvarCronogramaSemanal(CronogramaSemanalForm cronogramaSemanalForm) {
        autorizacao.exigir(Modulo.CRONOGRAMA, Acao.ESCRITA);
        log.info("Iniciando método para salvar um Cronograma Semanal");
        CronogramaSemanal cronogramaSemanal = cronogramaSemanalForm.converterToDomain();
        validarOrigemCronogramaSemanal(cronogramaSemanal);
        CronogramaSemanalDto cronogramaSemanalDto = cronogramaSemanalRepository.salvarCronogramaSemanal(cronogramaSemanal);
        log.info("Finalizando método que salva um Cronograma Semanal");
        return cronogramaSemanalDto;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CronogramaSemanalDto alterarCronogramaSemanal(CronogramaSemanalForm cronogramaSemanalForm) {
        autorizacao.exigir(Modulo.CRONOGRAMA, Acao.ESCRITA);
        log.info("Iniciando método para alterar um Cronograma Semanal");
        CronogramaSemanal cronogramaSemanal = cronogramaSemanalForm.converterToDomain();
        validarOrigemCronogramaSemanal(cronogramaSemanal);
        CronogramaSemanalDto cronogramaSemanalDto = cronogramaSemanalRepository.alterarCronogramaSemanal(cronogramaSemanal);
        log.info("Finalizando método que alterar um Cronograma Semanal");
        return cronogramaSemanalDto;
    }

    private void validarOrigemCronogramaSemanal(CronogramaSemanal cronogramaSemanal) {
        boolean fixo = Boolean.TRUE.equals(cronogramaSemanal.getInCronogramaFixo());
        if (fixo && cronogramaSemanal.getIdDiaSemana() == null) {
            throw new EntradaInvalidaException("Atividades fixas precisam de um dia da semana.");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletarCronogramaSemanal(Long id) {
        autorizacao.exigir(Modulo.CRONOGRAMA, Acao.ESCRITA);
        log.info("Iniciando método para deletar um Cronograma Semanal");
        this.cronogramaSemanalRepository.deletarCronogramaSemanal(id);
        log.info("Finalizando método que deleta um Cronograma Semanal");
    }

    @Override
    public CronogramaSemanalDto buscarPorIdCronogramaSemanal(Long id) {
        autorizacao.exigir(Modulo.CRONOGRAMA, Acao.LEITURA);
        log.info("Iniciando método para buscar um Cronograma Semanal por id");
        CronogramaSemanal cronogramaSemanal = cronogramaSemanalRepository.buscarPorIdCronogramaSemanal(id);
        CronogramaSemanalDto cronogramaSemanalDto = CronogramaSemanalDto.converterToDto(cronogramaSemanal);
        log.info("Finalizando método que busca um Cronograma Semanal por id");
        return cronogramaSemanalDto;
    }

    @Override
    public List<CronogramaSemanalDto> listarCronogramaSemanal() {
        autorizacao.exigir(Modulo.CRONOGRAMA, Acao.LEITURA);
        log.info("Iniciando método para listar Cronograma Semanal");
        List<CronogramaSemanal> lsCronogramaSemanal = cronogramaSemanalRepository.listarCronogramaSemanal();
        List<CronogramaSemanalDto> lsCronogramaSemanalDto = lsCronogramaSemanal.stream().map(CronogramaSemanalDto::converterToDto).collect(Collectors.toList());
        log.info("Finalizando método que lista Cronograma Semanal");
        return lsCronogramaSemanalDto;
    }

    @Override
    public List<CronogramaSemanalExcecaoDto> listarExcecoes() {
        autorizacao.exigir(Modulo.CRONOGRAMA, Acao.LEITURA);
        return cronogramaSemanalRepository.listarExcecoes();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void incluirExcecao(Long id, LocalDate data) {
        autorizacao.exigir(Modulo.CRONOGRAMA, Acao.ESCRITA);
        CronogramaSemanal item = cronogramaSemanalRepository.buscarPorIdCronogramaSemanal(id);
        exigirDono(item);
        if (!Boolean.TRUE.equals(item.getInCronogramaFixo())) {
            throw new EntradaInvalidaException("Só compromissos fixos aceitam exceção por dia.");
        }
        cronogramaSemanalRepository.incluirExcecao(id, data);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removerExcecao(Long id, LocalDate data) {
        autorizacao.exigir(Modulo.CRONOGRAMA, Acao.ESCRITA);
        exigirDono(cronogramaSemanalRepository.buscarPorIdCronogramaSemanal(id));
        cronogramaSemanalRepository.removerExcecao(id, data);
    }

    static final int MAX_COMPARTILHAMENTOS = 200;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CronogramaSemanalDto compartilhar(Long id, List<Long> idsUsuarios) {
        br.com.nord_tool_backend.security.UsuarioAutenticado u = autorizacao.exigir(Modulo.CRONOGRAMA, Acao.ESCRITA);
        CronogramaSemanal item = cronogramaSemanalRepository.buscarPorIdCronogramaSemanal(id);
        exigirDono(item);
        java.util.LinkedHashSet<Long> destinos = new java.util.LinkedHashSet<>();
        for (Long d : idsUsuarios == null ? new ArrayList<Long>() : idsUsuarios) {
            if (d != null && !d.equals(u.getId())) destinos.add(d);
        }
        if (destinos.size() > MAX_COMPARTILHAMENTOS) throw new EntradaInvalidaException("Compartilhe com no máximo " + MAX_COMPARTILHAMENTOS + " pessoas");
        if (!destinos.isEmpty() && cronogramaSemanalRepository.contarUsuariosAtivos(destinos) != destinos.size()) {
            throw new EntradaInvalidaException("Há pessoas inativas ou inexistentes na lista");
        }
        cronogramaSemanalRepository.compartilhar(id, new ArrayList<>(destinos));
        return CronogramaSemanalDto.converterToDto(cronogramaSemanalRepository.buscarPorIdCronogramaSemanal(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<br.com.nord_tool_backend.dto.PessoaCronogramaDto> listarPessoas() {
        autorizacao.exigir(Modulo.CRONOGRAMA, Acao.LEITURA);
        return cronogramaSemanalRepository.listarPessoas();
    }

    /** Compartilhado comigo é só leitura: exceções e compartilhamento são do dono. */
    private static void exigirDono(CronogramaSemanal item) {
        if (Boolean.FALSE.equals(item.getInProprio())) {
            throw new br.com.nord_tool_backend.exception.AcessoNegadoException("Só quem criou o item pode alterá-lo");
        }
    }
}
