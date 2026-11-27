package br.com.avaliacao.api.relatorio;

import br.com.avaliacao.domain.relatorio.FiltroRelatorio;
import org.springframework.format.annotation.DateTimeFormat;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record RelatorioQuery(@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
                             @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
                             String busca, List<Long> avaliacaoIds, BigDecimal notaMin, BigDecimal notaMax,
                             Integer minimoCorrigidas, Integer minimoRespostas, Integer limite) {
    public FiltroRelatorio paraDominio() {
        return new FiltroRelatorio(inicio, fim, busca, avaliacaoIds, notaMin == null ? BigDecimal.ZERO : notaMin,
                notaMax == null ? new BigDecimal("100") : notaMax, minimoCorrigidas == null ? 0 : minimoCorrigidas,
                minimoRespostas == null ? 1 : minimoRespostas, limite == null ? 10 : limite);
    }
}
