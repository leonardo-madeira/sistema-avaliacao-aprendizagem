package br.com.avaliacao.domain.questao;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class QuestaoTest {
    @Test
    void exigeEnunciadoEAutor() {
        assertThrows(IllegalArgumentException.class,
                () -> new Questao(null, 1, TipoQuestao.TEXTO_LIVRE, "  ", List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> new Questao(null, 0, TipoQuestao.TEXTO_LIVRE, "Explique SQL", List.of()));
    }

    @Test
    void objetivaExigeDuasAlternativasEUmGabarito() {
        assertThrows(IllegalArgumentException.class, () -> objetiva(List.of(new Alternativa(null, "A", true))));
        assertThrows(IllegalArgumentException.class, () -> objetiva(List.of(
                new Alternativa(null, "A", false), new Alternativa(null, "B", false))));
        assertThrows(IllegalArgumentException.class, () -> objetiva(List.of(
                new Alternativa(null, "A", true), new Alternativa(null, "B", true))));
    }

    @Test
    void textoLivreNaoAceitaAlternativas() {
        assertThrows(IllegalArgumentException.class, () -> new Questao(null, 1,
                TipoQuestao.TEXTO_LIVRE, "Explique SQL", List.of(new Alternativa(null, "A", true))));
    }

    @Test
    void preservaInvariantesDepoisDaCriacao() {
        List<Alternativa> alternativas = new ArrayList<>(List.of(
                new Alternativa(null, "  A  ", true), new Alternativa(null, "B", false)));
        Questao questao = objetiva(alternativas);
        alternativas.clear();
        assertEquals(2, questao.alternativas().size());
        assertEquals("A", questao.alternativas().getFirst().texto());
        assertThrows(UnsupportedOperationException.class, () -> questao.alternativas().clear());
    }

    private Questao objetiva(List<Alternativa> alternativas) {
        return new Questao(null, 1, TipoQuestao.MULTIPLA_ESCOLHA, "Qual comando cria tabela?", alternativas);
    }
}
