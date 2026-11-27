package br.com.avaliacao.domain.relatorio;

import java.math.BigDecimal;

public record QuestaoRanking(long posicao, long questaoId, String enunciado, long oportunidades,
                             long respondidas, long emBranco, long erros, BigDecimal percentualErros) { }
