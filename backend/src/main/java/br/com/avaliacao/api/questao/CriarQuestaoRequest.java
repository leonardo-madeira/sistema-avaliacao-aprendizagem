package br.com.avaliacao.api.questao;

import br.com.avaliacao.domain.questao.Alternativa;
import br.com.avaliacao.domain.questao.TipoQuestao;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CriarQuestaoRequest(
        @NotNull TipoQuestao tipo,
        @NotBlank String enunciado,
        List<@NotNull @Valid AlternativaRequest> alternativas) {

    public List<Alternativa> alternativasDominio() {
        return alternativas == null ? List.of()
                : alternativas.stream().map(a -> new Alternativa(null, a.texto(), a.correta())).toList();
    }

    public record AlternativaRequest(@NotBlank String texto, boolean correta) {
    }
}
