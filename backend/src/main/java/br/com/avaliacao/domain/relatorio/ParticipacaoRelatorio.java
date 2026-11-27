package br.com.avaliacao.domain.relatorio;

import java.math.BigDecimal;
import java.time.Instant;

public record ParticipacaoRelatorio(long avaliacaoId, String titulo, Instant inicioEm, long convidados,
                                   long entregues, long pendentes, long emAndamento, BigDecimal percentualEntregas) { }
