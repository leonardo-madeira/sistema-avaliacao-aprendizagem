package br.com.avaliacao.domain.relatorio;

import java.math.BigDecimal;
import java.time.Instant;

public record DesempenhoRelatorio(long avaliacaoId, String titulo, Instant inicioEm, BigDecimal valorTotal,
                                  long convidados, long entregues, long notasConsideradas, long aguardandoCorrecao,
                                  BigDecimal mediaNota, BigDecimal mediaPercentual, BigDecimal menorNota,
                                  BigDecimal maiorNota, BigDecimal mediaGeralPercentual) { }
