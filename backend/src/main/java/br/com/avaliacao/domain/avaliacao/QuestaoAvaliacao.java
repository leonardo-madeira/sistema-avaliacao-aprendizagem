package br.com.avaliacao.domain.avaliacao;

import java.math.BigDecimal;

public record QuestaoAvaliacao(long questaoId, BigDecimal valor) {
    public QuestaoAvaliacao {
        if (questaoId <= 0 || valor == null || valor.signum() <= 0
                || valor.compareTo(new BigDecimal("99999.99")) > 0
                || valor.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("Cada questão precisa de um ID válido e valor positivo com até duas casas decimais.");
        }
    }
}
