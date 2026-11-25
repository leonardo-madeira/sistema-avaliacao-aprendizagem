package br.com.avaliacao.domain.questao;

import java.util.List;

public record Questao(Long id, long autorId, TipoQuestao tipo, String enunciado,
                     List<Alternativa> alternativas) {
    public Questao {
        if (autorId <= 0) {
            throw new IllegalArgumentException("Informe o autor da questão.");
        }
        if (tipo == null) {
            throw new IllegalArgumentException("Informe o tipo da questão.");
        }
        if (enunciado == null || enunciado.isBlank()) {
            throw new IllegalArgumentException("Informe o enunciado da questão.");
        }
        enunciado = enunciado.strip();
        alternativas = alternativas == null ? List.of() : List.copyOf(alternativas);
        if (tipo == TipoQuestao.TEXTO_LIVRE && !alternativas.isEmpty()) {
            throw new IllegalArgumentException("Questões de texto livre não possuem alternativas.");
        }
        if (tipo == TipoQuestao.MULTIPLA_ESCOLHA) {
            if (alternativas.size() < 2) {
                throw new IllegalArgumentException("Informe pelo menos duas alternativas.");
            }
            if (alternativas.stream().filter(Alternativa::correta).count() != 1) {
                throw new IllegalArgumentException("Defina exatamente uma alternativa correta.");
            }
        }
    }
}
