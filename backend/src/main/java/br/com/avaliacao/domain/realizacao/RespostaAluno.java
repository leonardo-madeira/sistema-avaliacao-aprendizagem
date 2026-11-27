package br.com.avaliacao.domain.realizacao;

import br.com.avaliacao.domain.questao.TipoQuestao;

public record RespostaAluno(long avaliacaoQuestaoId, Long alternativaId, String texto) {
    public RespostaAluno {
        texto = texto == null || texto.isBlank() ? null : texto.strip();
        if (avaliacaoQuestaoId <= 0 || (alternativaId != null && alternativaId <= 0)
                || (alternativaId == null) == (texto == null)) {
            throw new IllegalArgumentException("Informe uma alternativa ou um texto para cada resposta.");
        }
    }

    public void validar(QuestaoParaResposta questao) {
        if (questao == null || questao.id() != avaliacaoQuestaoId) {
            throw new IllegalArgumentException("A questão não pertence à sua avaliação.");
        }
        if (questao.tipo() == TipoQuestao.MULTIPLA_ESCOLHA) {
            if (alternativaId == null || questao.alternativas().stream().noneMatch(item -> item.id() == alternativaId)) {
                throw new IllegalArgumentException("Escolha uma alternativa da própria questão.");
            }
        } else if (texto == null || alternativaId != null) {
            throw new IllegalArgumentException("A questão de texto livre exige uma resposta textual.");
        }
    }
}
