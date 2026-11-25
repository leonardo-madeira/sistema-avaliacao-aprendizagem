package br.com.avaliacao.api.questao;

import br.com.avaliacao.domain.questao.Questao;
import br.com.avaliacao.domain.questao.TipoQuestao;

import java.util.List;

public record QuestaoResponse(long id, long autorId, TipoQuestao tipo, String enunciado,
                             List<AlternativaResponse> alternativas) {
    public static QuestaoResponse de(Questao questao) {
        return new QuestaoResponse(questao.id(), questao.autorId(), questao.tipo(), questao.enunciado(),
                questao.alternativas().stream()
                        .map(a -> new AlternativaResponse(a.id(), a.texto(), a.correta())).toList());
    }

    public record AlternativaResponse(long id, String texto, boolean correta) {
    }
}
