package br.com.avaliacao.application.correcao;

import br.com.avaliacao.domain.NaoEncontradoException;
import br.com.avaliacao.domain.correcao.*;
import br.com.avaliacao.domain.questao.TipoQuestao;
import br.com.avaliacao.domain.realizacao.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.*;

@Service
public class CorrecaoService {
    private final CorrecaoRepository correcoes;
    private final RealizacaoRepository provas;
    public CorrecaoService(CorrecaoRepository correcoes, RealizacaoRepository provas) {
        this.correcoes = correcoes; this.provas = provas;
    }

    @Transactional(readOnly = true)
    public List<EntregaResumo> listar(long avaliacaoId, long professorId) {
        if (!correcoes.avaliacaoDoProfessor(avaliacaoId, professorId)) {
            throw new NaoEncontradoException("Avaliação publicada não encontrada.");
        }
        return correcoes.entregas(avaliacaoId);
    }

    @Transactional(readOnly = true)
    public Detalhe buscar(long id, long professorId) {
        Participacao p = carregar(id, professorId, false);
        p.exigirEntregue();
        return new Detalhe(p.id(), p.avaliacaoId(), p.alunoNome(), p.titulo(), p.valorTotal(), p.notaTotal(), correcoes.respostas(id));
    }

    @Transactional
    public void salvar(long id, long professorId, List<NotaDiscursiva> notas) {
        Participacao p = carregar(id, professorId, true);
        p.exigirEntregue();
        if (notas.isEmpty() || notas.stream().map(NotaDiscursiva::respostaId).distinct().count() != notas.size()) {
            throw new IllegalArgumentException("Informe notas sem repetir respostas.");
        }
        Map<Long, RespostaParaCorrecao> respostas = new HashMap<>();
        correcoes.respostas(id).stream().filter(r -> r.respostaId() != null).forEach(r -> respostas.put(r.respostaId(), r));
        for (NotaDiscursiva nota : notas) {
            RespostaParaCorrecao resposta = respostas.get(nota.respostaId());
            if (resposta == null || resposta.tipo() != TipoQuestao.TEXTO_LIVRE || nota.nota().compareTo(resposta.valor()) > 0) {
                throw new IllegalArgumentException("Corrija somente textos desta entrega, respeitando o valor da questão.");
            }
        }
        notas.forEach(nota -> correcoes.atribuir(id, nota));
    }

    private Participacao carregar(long id, long professorId, boolean bloquear) {
        return provas.buscarDoProfessor(id, professorId, bloquear)
                .orElseThrow(() -> new NaoEncontradoException("Entrega não encontrada."));
    }

    public record Detalhe(long id, long avaliacaoId, String alunoNome, String titulo, BigDecimal valorTotal,
                          BigDecimal notaTotal, List<RespostaParaCorrecao> respostas) { }
}
