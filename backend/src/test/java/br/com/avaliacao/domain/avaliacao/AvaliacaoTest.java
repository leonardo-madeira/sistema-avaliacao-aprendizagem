package br.com.avaliacao.domain.avaliacao;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class AvaliacaoTest {
    private final Instant inicio = Instant.parse("2030-01-01T12:00:00Z");

    @Test
    void rascunhoPodeEstarIncompletoMasPublicacaoExigeQuestoesEAlunos() {
        Avaliacao rascunho = criar(List.of(), List.of());
        assertThrows(IllegalArgumentException.class, rascunho::publicar);
        assertThrows(IllegalArgumentException.class, () -> criar(List.of(item(1, "2")), List.of()).publicar());
    }

    @Test
    void pontuacaoPertenceAProvaESomaValoresDecimais() {
        Avaliacao prova = criar(List.of(item(1, "2.50"), item(2, "7.50")), List.of(3L));
        assertEquals(new BigDecimal("10.00"), prova.valorTotal());
        assertEquals(Avaliacao.Status.PUBLICADA, prova.publicar().status());
        assertEquals(Avaliacao.Status.RASCUNHO, prova.status());
    }

    @Test
    void rejeitaQuestoesOuAlunosDuplicados() {
        assertThrows(IllegalArgumentException.class, () -> criar(List.of(item(1, "2"), item(1, "3")), List.of(3L)));
        assertThrows(IllegalArgumentException.class, () -> criar(List.of(item(1, "2")), List.of(3L, 3L)));
    }

    @Test
    void rejeitaValoresSemPrecisaoOuForaDaFaixaDoBanco() {
        for (String valor : List.of("0", "-1", "0.001", "100000")) {
            assertThrows(IllegalArgumentException.class, () -> item(1, valor));
        }
    }

    @Test
    void rejeitaPeriodoInvertidoETituloVazio() {
        assertThrows(IllegalArgumentException.class, () -> new Avaliacao(null, 1, "Prova", "", inicio,
                inicio.minusSeconds(1), Avaliacao.Status.RASCUNHO, List.of(), List.of()));
        assertThrows(IllegalArgumentException.class, () -> new Avaliacao(null, 1, " ", "", inicio,
                inicio.plusSeconds(3600), Avaliacao.Status.RASCUNHO, List.of(), List.of()));
    }

    private QuestaoAvaliacao item(long id, String valor) { return new QuestaoAvaliacao(id, new BigDecimal(valor)); }
    private Avaliacao criar(List<QuestaoAvaliacao> questoes, List<Long> alunos) {
        return new Avaliacao(null, 1, "Prova de SQL", "", inicio, inicio.plusSeconds(3600),
                Avaliacao.Status.RASCUNHO, questoes, alunos);
    }
}
