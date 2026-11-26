package br.com.avaliacao.domain.questao;

import java.util.List;

public interface QuestaoRepository {
    Questao salvar(Questao questao);
    List<Questao> listar(TipoQuestao tipo, int limite, int deslocamento);
    long contar(TipoQuestao tipo);
}
