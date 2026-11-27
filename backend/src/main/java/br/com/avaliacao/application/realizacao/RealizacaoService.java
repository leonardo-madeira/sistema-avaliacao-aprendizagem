package br.com.avaliacao.application.realizacao;

import br.com.avaliacao.domain.NaoEncontradoException;
import br.com.avaliacao.domain.realizacao.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.*;

@Service
public class RealizacaoService {
    private final RealizacaoRepository provas;
    private final Clock clock;
    public RealizacaoService(RealizacaoRepository provas, Clock clock) { this.provas = provas; this.clock = clock; }

    @Transactional(readOnly = true)
    public List<Resumo> listar(long alunoId) {
        Instant agora = clock.instant();
        return provas.listarDoAluno(alunoId).stream().map(p -> resumo(p, agora)).toList();
    }

    @Transactional(readOnly = true)
    public Detalhe buscar(long id, long alunoId) {
        Participacao p = carregar(id, alunoId, false);
        if (p.entregueEm() == null) p.exigirEmAndamento(clock.instant());
        return new Detalhe(resumo(p, clock.instant()), provas.questoes(p.avaliacaoId()), provas.respostas(id));
    }

    @Transactional
    public void iniciar(long id, long alunoId) {
        Participacao p = carregar(id, alunoId, true);
        Instant agora = clock.instant();
        p.exigirAberta(agora);
        provas.iniciar(id, agora);
    }

    @Transactional
    public void salvar(long id, long alunoId, List<RespostaAluno> respostas, boolean entregar) {
        Participacao p = carregar(id, alunoId, true);
        p.exigirEmAndamento(clock.instant());
        if (respostas.stream().map(RespostaAluno::avaliacaoQuestaoId).distinct().count() != respostas.size()) {
            throw new IllegalArgumentException("Não repita a mesma questão nas respostas.");
        }
        Map<Long, QuestaoParaResposta> questoes = new HashMap<>();
        provas.questoes(p.avaliacaoId()).forEach(q -> questoes.put(q.id(), q));
        respostas.forEach(r -> r.validar(questoes.get(r.avaliacaoQuestaoId())));
        provas.substituirRespostas(id, respostas, clock.instant());
        Instant agora = clock.instant();
        p.exigirEmAndamento(agora);
        if (entregar) provas.entregar(id, agora);
    }

    private Participacao carregar(long id, long alunoId, boolean bloquear) {
        return provas.buscarDoAluno(id, alunoId, bloquear)
                .orElseThrow(() -> new NaoEncontradoException("Avaliação não encontrada para sua conta."));
    }

    private Resumo resumo(Participacao p, Instant agora) {
        boolean entregue = p.entregueEm() != null;
        return new Resumo(p.id(), p.avaliacaoId(), p.titulo(), p.descricao(), p.inicioEm(), p.fimEm(), p.entregueEm(),
                p.situacao(agora), p.valorTotal(), entregue ? p.notaTotal() : null,
                entregue ? p.correcoesPendentes() : 0, p.quantidadeQuestoes());
    }

    public record Resumo(long id, long avaliacaoId, String titulo, String descricao, Instant inicioEm,
                         Instant fimEm, Instant entregueEm, String situacao, BigDecimal valorTotal,
                         BigDecimal notaTotal, long correcoesPendentes, long quantidadeQuestoes) { }
    public record Detalhe(Resumo avaliacao, List<QuestaoParaResposta> questoes, List<RespostaAluno> respostas) { }
}
