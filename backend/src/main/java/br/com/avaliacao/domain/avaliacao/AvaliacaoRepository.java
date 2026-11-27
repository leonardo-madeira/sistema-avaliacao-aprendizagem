package br.com.avaliacao.domain.avaliacao;

import java.util.List;
import java.util.Optional;

public interface AvaliacaoRepository {
    Avaliacao salvar(Avaliacao avaliacao);
    Optional<Avaliacao> buscar(long id, long professorId);
    Optional<Avaliacao> buscarParaAlteracao(long id, long professorId);
    List<AvaliacaoResumo> listar(long professorId);
    boolean questoesProntas(List<Long> ids);
    void publicar(long id);
}
