package br.com.avaliacao.domain.relatorio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record FiltroRelatorio(LocalDate inicio, LocalDate fim, String busca, List<Long> avaliacaoIds,
                              BigDecimal notaMin, BigDecimal notaMax, int minimoCorrigidas,
                              int minimoRespostas, int limite) {
    public FiltroRelatorio {
        if (inicio != null && fim != null && fim.isBefore(inicio)) {
            throw new IllegalArgumentException("A data final deve ser igual ou posterior à inicial.");
        }
        busca = busca == null ? "" : busca.strip();
        if (avaliacaoIds != null && avaliacaoIds.stream().anyMatch(java.util.Objects::isNull)) {
            throw new IllegalArgumentException("Selecione somente IDs de avaliações válidos.");
        }
        avaliacaoIds = avaliacaoIds == null ? List.of() : List.copyOf(avaliacaoIds);
        if (avaliacaoIds.stream().anyMatch(id -> id <= 0)
                || avaliacaoIds.stream().distinct().count() != avaliacaoIds.size()) {
            throw new IllegalArgumentException("Selecione avaliações válidas, sem duplicidade.");
        }
        if (notaMin == null || notaMax == null || notaMin.signum() < 0
                || notaMax.compareTo(new BigDecimal("100")) > 0 || notaMin.compareTo(notaMax) > 0) {
            throw new IllegalArgumentException("A faixa de notas percentuais deve ficar entre 0 e 100.");
        }
        if (minimoCorrigidas < 0 || minimoRespostas < 1 || limite < 1 || limite > 50) {
            throw new IllegalArgumentException("Confira a amostra mínima e use um limite de ranking entre 1 e 50.");
        }
    }
}
