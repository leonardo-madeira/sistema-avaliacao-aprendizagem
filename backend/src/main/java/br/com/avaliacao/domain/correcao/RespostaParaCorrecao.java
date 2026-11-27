package br.com.avaliacao.domain.correcao;

import br.com.avaliacao.domain.questao.TipoQuestao;
import java.math.BigDecimal;

public record RespostaParaCorrecao(Long respostaId, long avaliacaoQuestaoId, String enunciado,
                                  TipoQuestao tipo, BigDecimal valor, String resposta, BigDecimal notaObtida) { }
