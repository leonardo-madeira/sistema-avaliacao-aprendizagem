package br.com.avaliacao.api.avaliacao;

import br.com.avaliacao.domain.avaliacao.Avaliacao;
import br.com.avaliacao.domain.avaliacao.QuestaoAvaliacao;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record AvaliacaoResponse(long id, long professorId, String titulo, String descricao, Instant inicioEm,
                               Instant fimEm, Avaliacao.Status status, List<QuestaoAvaliacao> questoes,
                               List<Long> alunos, BigDecimal valorTotal) {
    public static AvaliacaoResponse de(Avaliacao avaliacao) {
        return new AvaliacaoResponse(avaliacao.id(), avaliacao.professorId(), avaliacao.titulo(), avaliacao.descricao(),
                avaliacao.inicioEm(), avaliacao.fimEm(), avaliacao.status(), avaliacao.questoes(),
                avaliacao.alunos(), avaliacao.valorTotal());
    }
}
