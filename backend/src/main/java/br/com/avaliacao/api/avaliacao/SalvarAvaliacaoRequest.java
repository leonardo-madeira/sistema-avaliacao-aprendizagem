package br.com.avaliacao.api.avaliacao;

import br.com.avaliacao.domain.avaliacao.Avaliacao;
import br.com.avaliacao.domain.avaliacao.QuestaoAvaliacao;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record SalvarAvaliacaoRequest(@NotBlank String titulo, String descricao,
                                     @NotNull Instant inicioEm, @NotNull Instant fimEm,
                                     List<@NotNull @Valid ItemRequest> questoes,
                                     List<@NotNull @Positive Long> alunos) {
    public Avaliacao paraDominio(Long id, long professorId) {
        return new Avaliacao(id, professorId, titulo, descricao, inicioEm, fimEm, Avaliacao.Status.RASCUNHO,
                questoes == null ? List.of() : questoes.stream()
                        .map(item -> new QuestaoAvaliacao(item.questaoId(), item.valor())).toList(), alunos);
    }
    public record ItemRequest(@Positive long questaoId, @NotNull BigDecimal valor) { }
}
