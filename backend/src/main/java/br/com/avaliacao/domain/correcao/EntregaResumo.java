package br.com.avaliacao.domain.correcao;

import java.math.BigDecimal;
import java.time.Instant;

public record EntregaResumo(long id, String alunoNome, Instant entregueEm, BigDecimal valorTotal,
                           BigDecimal notaParcial, BigDecimal notaTotal, long correcoesPendentes) { }
