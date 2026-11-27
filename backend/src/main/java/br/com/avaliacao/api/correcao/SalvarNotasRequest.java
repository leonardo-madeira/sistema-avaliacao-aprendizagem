package br.com.avaliacao.api.correcao;

import br.com.avaliacao.domain.correcao.NotaDiscursiva;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.List;

public record SalvarNotasRequest(@NotNull List<@NotNull @Valid Item> notas) {
    public List<NotaDiscursiva> paraDominio() { return notas.stream().map(n -> new NotaDiscursiva(n.respostaId(), n.nota())).toList(); }
    public record Item(@Positive long respostaId, @NotNull BigDecimal nota) { }
}
