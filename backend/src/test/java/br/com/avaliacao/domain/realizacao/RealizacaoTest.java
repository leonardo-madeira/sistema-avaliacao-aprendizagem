package br.com.avaliacao.domain.realizacao;

import br.com.avaliacao.domain.ConflitoException;
import br.com.avaliacao.domain.correcao.NotaDiscursiva;
import br.com.avaliacao.domain.questao.TipoQuestao;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class RealizacaoTest {
    private static final Instant INICIO = Instant.parse("2030-01-01T12:00:00Z");

    @Test
    void inicioInclusivoEFimExclusivo() {
        Participacao p = participacao(INICIO, null);
        assertThrows(ConflitoException.class, () -> p.exigirAberta(INICIO.minusSeconds(1)));
        assertDoesNotThrow(() -> p.exigirEmAndamento(INICIO));
        assertThrows(ConflitoException.class, () -> p.exigirAberta(INICIO.plusSeconds(3600)));
    }

    @Test
    void exigeInicioEBloqueiaDepoisDaEntrega() {
        assertThrows(ConflitoException.class, () -> participacao(null, null).exigirEmAndamento(INICIO));
        Participacao entregue = participacao(INICIO, INICIO.plusSeconds(60));
        assertEquals("ENTREGUE", entregue.situacao(INICIO.plusSeconds(7200)));
        assertThrows(ConflitoException.class, () -> entregue.exigirAberta(INICIO.plusSeconds(120)));
    }

    @Test
    void alternativaDevePertencerAQuestao() {
        var questao = new QuestaoParaResposta(10, "SQL?", TipoQuestao.MULTIPLA_ESCOLHA, BigDecimal.ONE,
                List.of(new QuestaoParaResposta.Opcao(1, "A"), new QuestaoParaResposta.Opcao(2, "B")));
        assertDoesNotThrow(() -> new RespostaAluno(10, 1L, null).validar(questao));
        assertThrows(IllegalArgumentException.class, () -> new RespostaAluno(10, 3L, null).validar(questao));
        assertThrows(IllegalArgumentException.class, () -> new RespostaAluno(10, null, "texto").validar(questao));
        assertThrows(IllegalArgumentException.class, () -> new RespostaAluno(11, 1L, null).validar(questao));
    }

    @Test
    void textoLivreSoAceitaTexto() {
        var questao = new QuestaoParaResposta(11, "Explique", TipoQuestao.TEXTO_LIVRE, BigDecimal.ONE, List.of());
        assertDoesNotThrow(() -> new RespostaAluno(11, null, "Resposta").validar(questao));
        assertThrows(IllegalArgumentException.class, () -> new RespostaAluno(11, 1L, null).validar(questao));
    }

    @Test
    void respostaNaoMisturaFormatosNemRepresentaTextoVazio() {
        assertThrows(IllegalArgumentException.class, () -> new RespostaAluno(1, 1L, "texto"));
        assertThrows(IllegalArgumentException.class, () -> new RespostaAluno(1, null, "  "));
        assertThrows(IllegalArgumentException.class, () -> new RespostaAluno(1, null, null));
    }

    @Test
    void zeroENotaValidaSemConfundirComPendente() {
        assertEquals(BigDecimal.ZERO, new NotaDiscursiva(1, BigDecimal.ZERO).nota());
        for (String invalida : List.of("-1", "0.001", "100000")) {
            assertThrows(IllegalArgumentException.class, () -> new NotaDiscursiva(1, new BigDecimal(invalida)));
        }
    }

    private Participacao participacao(Instant iniciada, Instant entregue) {
        return new Participacao(1, 1, 3, 1, "Ana", "Prova", "", INICIO, INICIO.plusSeconds(3600),
                iniciada, entregue, BigDecimal.TEN, null, 0, 2);
    }
}
