package br.com.avaliacao.domain.correcao;

import java.math.BigDecimal;

public record NotaDiscursiva(long respostaId, BigDecimal nota) {
    public NotaDiscursiva {
        if (respostaId <= 0 || nota == null || nota.signum() < 0
                || nota.compareTo(new BigDecimal("99999.99")) > 0
                || nota.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("Informe uma nota não negativa com até duas casas decimais.");
        }
    }
}
