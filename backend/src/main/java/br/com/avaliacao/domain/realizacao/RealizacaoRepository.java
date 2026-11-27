package br.com.avaliacao.domain.realizacao;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface RealizacaoRepository {
    List<Participacao> listarDoAluno(long alunoId);
    Optional<Participacao> buscarDoAluno(long id, long alunoId, boolean bloquear);
    Optional<Participacao> buscarDoProfessor(long id, long professorId, boolean bloquear);
    List<QuestaoParaResposta> questoes(long avaliacaoId);
    List<RespostaAluno> respostas(long participacaoId);
    void iniciar(long id, Instant agora);
    void substituirRespostas(long id, List<RespostaAluno> respostas, Instant agora);
    void entregar(long id, Instant agora);
}
