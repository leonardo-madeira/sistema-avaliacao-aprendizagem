package br.com.avaliacao.infrastructure.persistence;

import br.com.avaliacao.domain.avaliacao.Avaliacao;
import br.com.avaliacao.domain.avaliacao.AvaliacaoRepository;
import br.com.avaliacao.domain.avaliacao.AvaliacaoResumo;
import br.com.avaliacao.domain.avaliacao.QuestaoAvaliacao;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Repository
public class JdbcAvaliacaoDao implements AvaliacaoRepository {
    private final JdbcTemplate jdbc;

    public JdbcAvaliacaoDao(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public Avaliacao salvar(Avaliacao avaliacao) {
        Long id = avaliacao.id();
        if (id == null) {
            id = Objects.requireNonNull(jdbc.queryForObject("""
                    INSERT INTO avaliacoes (professor_id, titulo, descricao, inicio_em, fim_em)
                    VALUES (?, ?, ?, ?, ?) RETURNING id
                    """, Long.class, avaliacao.professorId(), avaliacao.titulo(), avaliacao.descricao(),
                    Timestamp.from(avaliacao.inicioEm()), Timestamp.from(avaliacao.fimEm())));
        } else {
            jdbc.update("""
                    UPDATE avaliacoes SET titulo = ?, descricao = ?, inicio_em = ?, fim_em = ? WHERE id = ?
                    """, avaliacao.titulo(), avaliacao.descricao(), Timestamp.from(avaliacao.inicioEm()),
                    Timestamp.from(avaliacao.fimEm()), id);
            jdbc.update("DELETE FROM participacoes WHERE avaliacao_id = ?", id);
            jdbc.update("DELETE FROM avaliacao_questoes WHERE avaliacao_id = ?", id);
        }
        for (int i = 0; i < avaliacao.questoes().size(); i++) {
            QuestaoAvaliacao item = avaliacao.questoes().get(i);
            jdbc.update("""
                    INSERT INTO avaliacao_questoes (avaliacao_id, questao_id, ordem, valor) VALUES (?, ?, ?, ?)
                    """, id, item.questaoId(), i + 1, item.valor());
        }
        for (long alunoId : avaliacao.alunos()) {
            jdbc.update("INSERT INTO participacoes (avaliacao_id, aluno_id) VALUES (?, ?)", id, alunoId);
        }
        return new Avaliacao(id, avaliacao.professorId(), avaliacao.titulo(), avaliacao.descricao(),
                avaliacao.inicioEm(), avaliacao.fimEm(), avaliacao.status(), avaliacao.questoes(), avaliacao.alunos());
    }

    @Override public Optional<Avaliacao> buscar(long id, long professorId) { return carregar(id, professorId, false); }
    @Override public Optional<Avaliacao> buscarParaAlteracao(long id, long professorId) { return carregar(id, professorId, true); }

    private Optional<Avaliacao> carregar(long id, long professorId, boolean bloquear) {
        String sql = "SELECT * FROM avaliacoes WHERE id = ? AND professor_id = ?" + (bloquear ? " FOR UPDATE" : "");
        List<Cabecalho> registros = jdbc.query(sql, (rs, row) -> new Cabecalho(
                rs.getLong("id"), rs.getLong("professor_id"), rs.getString("titulo"), rs.getString("descricao"),
                rs.getTimestamp("inicio_em").toInstant(), rs.getTimestamp("fim_em").toInstant(),
                Avaliacao.Status.valueOf(rs.getString("status"))), id, professorId);
        if (registros.isEmpty()) return Optional.empty();
        Cabecalho registro = registros.getFirst();
        List<QuestaoAvaliacao> questoes = jdbc.query("""
                SELECT questao_id, valor FROM avaliacao_questoes WHERE avaliacao_id = ? ORDER BY ordem
                """, (rs, row) -> new QuestaoAvaliacao(rs.getLong("questao_id"), rs.getBigDecimal("valor")), id);
        List<Long> alunos = jdbc.query("SELECT aluno_id FROM participacoes WHERE avaliacao_id = ? ORDER BY aluno_id",
                (rs, row) -> rs.getLong("aluno_id"), id);
        return Optional.of(new Avaliacao(id, professorId, registro.titulo(), registro.descricao(), registro.inicioEm(),
                registro.fimEm(), registro.status(), questoes, alunos));
    }

    @Override
    public List<AvaliacaoResumo> listar(long professorId) {
        return jdbc.query("""
                SELECT a.*, COALESCE(q.quantidade, 0) AS quantidade_questoes,
                       COALESCE(q.total, 0) AS valor_total, COALESCE(p.quantidade, 0) AS quantidade_alunos
                FROM avaliacoes a
                LEFT JOIN (SELECT avaliacao_id, COUNT(*) AS quantidade, SUM(valor) AS total
                           FROM avaliacao_questoes GROUP BY avaliacao_id) q ON q.avaliacao_id = a.id
                LEFT JOIN (SELECT avaliacao_id, COUNT(*) AS quantidade
                           FROM participacoes GROUP BY avaliacao_id) p ON p.avaliacao_id = a.id
                WHERE a.professor_id = ? ORDER BY a.criada_em DESC, a.id DESC
                """, (rs, row) -> new AvaliacaoResumo(rs.getLong("id"), rs.getString("titulo"),
                rs.getString("descricao"), rs.getTimestamp("inicio_em").toInstant(), rs.getTimestamp("fim_em").toInstant(),
                Avaliacao.Status.valueOf(rs.getString("status")), rs.getLong("quantidade_questoes"),
                rs.getLong("quantidade_alunos"), rs.getBigDecimal("valor_total")), professorId);
    }

    @Override
    public boolean questoesProntas(List<Long> ids) {
        if (ids.isEmpty()) return true;
        Long count = new NamedParameterJdbcTemplate(jdbc).queryForObject("""
                SELECT COUNT(*) FROM questoes q JOIN tipos_questao tq ON tq.id = q.tipo_questao_id
                WHERE q.id IN (:ids) AND (
                    tq.codigo = 'TEXTO_LIVRE' OR (
                        (SELECT COUNT(*) FROM alternativas a WHERE a.questao_id = q.id) >= 2
                        AND (SELECT COUNT(*) FROM alternativas a WHERE a.questao_id = q.id AND a.correta) = 1
                    )
                )
                """, Map.of("ids", ids), Long.class);
        return count != null && count == ids.size();
    }

    @Override public void publicar(long id) { jdbc.update("UPDATE avaliacoes SET status = 'PUBLICADA' WHERE id = ?", id); }

    private record Cabecalho(long id, long professorId, String titulo, String descricao,
                            Instant inicioEm, Instant fimEm, Avaliacao.Status status) { }
}
