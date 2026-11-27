package br.com.avaliacao.domain.avaliacao;

import java.math.BigDecimal;
import java.time.Instant;

public record AvaliacaoResumo(long id, String titulo, String descricao, Instant inicioEm, Instant fimEm,
                              Avaliacao.Status status, long quantidadeQuestoes, long quantidadeAlunos,
                              BigDecimal valorTotal) {
}
