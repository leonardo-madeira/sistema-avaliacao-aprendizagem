package br.com.avaliacao.domain.realizacao;

import br.com.avaliacao.domain.ConflitoException;
import java.math.BigDecimal;
import java.time.Instant;

public record Participacao(long id, long avaliacaoId, long alunoId, long professorId, String alunoNome,
                           String titulo, String descricao, Instant inicioEm, Instant fimEm,
                           Instant iniciadaEm, Instant entregueEm, BigDecimal valorTotal,
                           BigDecimal notaTotal, long correcoesPendentes, long quantidadeQuestoes) {
    public String situacao(Instant agora) {
        if (entregueEm != null) return "ENTREGUE";
        if (agora.isBefore(inicioEm)) return "AGENDADA";
        if (!agora.isBefore(fimEm)) return "ENCERRADA";
        return iniciadaEm == null ? "DISPONIVEL" : "EM_ANDAMENTO";
    }

    public void exigirAberta(Instant agora) {
        if (entregueEm != null) throw new ConflitoException("A avaliação já foi entregue.");
        if (agora.isBefore(inicioEm)) throw new ConflitoException("A avaliação ainda não começou.");
        if (!agora.isBefore(fimEm)) throw new ConflitoException("O prazo da avaliação terminou.");
    }

    public void exigirEmAndamento(Instant agora) {
        exigirAberta(agora);
        if (iniciadaEm == null) throw new ConflitoException("Inicie a avaliação antes de responder.");
    }

    public void exigirEntregue() {
        if (entregueEm == null) throw new ConflitoException("A avaliação ainda não foi entregue.");
    }
}
