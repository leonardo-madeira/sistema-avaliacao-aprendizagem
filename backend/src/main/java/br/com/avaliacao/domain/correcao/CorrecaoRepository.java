package br.com.avaliacao.domain.correcao;

import java.util.List;

public interface CorrecaoRepository {
    boolean avaliacaoDoProfessor(long avaliacaoId, long professorId);
    List<EntregaResumo> entregas(long avaliacaoId);
    List<RespostaParaCorrecao> respostas(long participacaoId);
    void atribuir(long participacaoId, NotaDiscursiva nota);
}
