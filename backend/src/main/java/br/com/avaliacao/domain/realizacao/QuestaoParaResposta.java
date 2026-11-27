package br.com.avaliacao.domain.realizacao;

import br.com.avaliacao.domain.questao.TipoQuestao;
import java.math.BigDecimal;
import java.util.List;

public record QuestaoParaResposta(long id, String enunciado, TipoQuestao tipo, BigDecimal valor,
                                 List<Opcao> alternativas) {
    public record Opcao(long id, String texto) { }
}
