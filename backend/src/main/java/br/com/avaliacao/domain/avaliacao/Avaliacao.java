package br.com.avaliacao.domain.avaliacao;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;

public record Avaliacao(Long id, long professorId, String titulo, String descricao,
                        Instant inicioEm, Instant fimEm, Status status,
                        List<QuestaoAvaliacao> questoes, List<Long> alunos) {
    public enum Status { RASCUNHO, PUBLICADA }

    public Avaliacao {
        if (professorId <= 0 || titulo == null || titulo.isBlank() || titulo.strip().length() > 200) {
            throw new IllegalArgumentException("Informe um professor e um título com até 200 caracteres.");
        }
        if (inicioEm == null || fimEm == null || !fimEm.isAfter(inicioEm)) {
            throw new IllegalArgumentException("O fim da avaliação deve ser posterior ao início.");
        }
        if (status == null) throw new IllegalArgumentException("Informe a situação da avaliação.");
        titulo = titulo.strip();
        descricao = descricao == null ? "" : descricao.strip();
        questoes = questoes == null ? List.of() : List.copyOf(questoes);
        alunos = alunos == null ? List.of() : List.copyOf(alunos);
        if (questoes.size() > Short.MAX_VALUE) throw new IllegalArgumentException("Quantidade de questões excedida.");
        if (questoes.stream().map(QuestaoAvaliacao::questaoId).distinct().count() != questoes.size()) {
            throw new IllegalArgumentException("Uma questão só pode aparecer uma vez na mesma avaliação.");
        }
        if (alunos.stream().anyMatch(idAluno -> idAluno <= 0) || new HashSet<>(alunos).size() != alunos.size()) {
            throw new IllegalArgumentException("Informe alunos válidos, sem duplicidade.");
        }
        if (status == Status.PUBLICADA && (questoes.isEmpty() || alunos.isEmpty())) {
            throw new IllegalArgumentException("Para publicar, selecione pelo menos uma questão e um aluno.");
        }
    }

    public BigDecimal valorTotal() {
        return questoes.stream().map(QuestaoAvaliacao::valor).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Avaliacao publicar() {
        return new Avaliacao(id, professorId, titulo, descricao, inicioEm, fimEm, Status.PUBLICADA, questoes, alunos);
    }
}
