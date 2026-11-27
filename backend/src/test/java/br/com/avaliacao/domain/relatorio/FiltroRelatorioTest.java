package br.com.avaliacao.domain.relatorio;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class FiltroRelatorioTest {
    @Test
    void rejeitaPeriodoInvertido() {
        assertThrows(IllegalArgumentException.class, () -> new FiltroRelatorio(LocalDate.of(2030, 2, 1),
                LocalDate.of(2030, 1, 1), "", List.of(), BigDecimal.ZERO, BigDecimal.TEN, 0, 1, 10));
    }
    @Test
    void rejeitaFaixasInvalidas() {
        for (String[] faixa : List.of(new String[]{"-1", "100"}, new String[]{"90", "20"}, new String[]{"0", "101"})) {
            assertThrows(IllegalArgumentException.class, () -> criar(List.of(), faixa[0], faixa[1], 0, 1, 10));
        }
    }
    @Test
    void exigeAmostraELimiteValidos() {
        assertThrows(IllegalArgumentException.class, () -> criar(List.of(), "0", "100", -1, 1, 10));
        assertThrows(IllegalArgumentException.class, () -> criar(List.of(), "0", "100", 0, 0, 10));
        assertThrows(IllegalArgumentException.class, () -> criar(List.of(), "0", "100", 0, 1, 51));
    }
    @Test
    void rejeitaIdsDuplicadosOuInvalidos() {
        assertThrows(IllegalArgumentException.class, () -> criar(List.of(1L, 1L), "0", "100", 0, 1, 10));
        assertThrows(IllegalArgumentException.class, () -> criar(List.of(0L), "0", "100", 0, 1, 10));
    }
    private FiltroRelatorio criar(List<Long> ids, String min, String max, int corrigidas, int respostas, int limite) {
        return new FiltroRelatorio(null, null, null, ids, new BigDecimal(min), new BigDecimal(max), corrigidas, respostas, limite);
    }
}
