package br.com.avaliacao.domain.relatorio;

import java.util.List;

public interface RelatorioRepository {
    List<DesempenhoRelatorio> desempenho(long professorId, FiltroRelatorio filtro);
    List<QuestaoRanking> questoes(long professorId, FiltroRelatorio filtro);
    List<ParticipacaoRelatorio> participacao(long professorId, FiltroRelatorio filtro);
}
