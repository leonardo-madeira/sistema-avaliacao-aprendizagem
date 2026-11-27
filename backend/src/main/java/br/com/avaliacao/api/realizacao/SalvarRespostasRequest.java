package br.com.avaliacao.api.realizacao;

import br.com.avaliacao.domain.realizacao.RespostaAluno;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

public record SalvarRespostasRequest(@NotNull List<@NotNull @Valid Item> respostas) {
    public List<RespostaAluno> paraDominio() {
        return respostas.stream().map(r -> new RespostaAluno(r.avaliacaoQuestaoId(), r.alternativaId(), r.texto())).toList();
    }
    public record Item(@Positive long avaliacaoQuestaoId, @Positive Long alternativaId, String texto) { }
}
