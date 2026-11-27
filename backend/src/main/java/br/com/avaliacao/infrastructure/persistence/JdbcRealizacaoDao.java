package br.com.avaliacao.infrastructure.persistence;

import br.com.avaliacao.domain.questao.TipoQuestao;
import br.com.avaliacao.domain.realizacao.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.math.BigDecimal;
import java.util.*;

@Repository
public class JdbcRealizacaoDao implements RealizacaoRepository {
    private final JdbcTemplate jdbc;
    private static final String BASE = """
            SELECT p.id, p.avaliacao_id, p.aluno_id, a.professor_id, u.nome AS aluno_nome,
                   a.titulo, a.descricao, a.inicio_em, a.fim_em, p.iniciada_em, p.entregue_em,
                   rp.valor_total, rp.nota_total, rp.quantidade_correcao_pendente, rp.quantidade_questoes
            FROM participacoes p JOIN avaliacoes a ON a.id = p.avaliacao_id
            JOIN usuarios u ON u.id = p.aluno_id
            JOIN resultados_participacoes rp ON rp.participacao_id = p.id
            WHERE a.status = 'PUBLICADA'
            """;

    public JdbcRealizacaoDao(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public List<Participacao> listarDoAluno(long alunoId) {
        return jdbc.query(BASE + " AND p.aluno_id = ? ORDER BY a.inicio_em DESC, p.id DESC", this::mapear, alunoId);
    }

    @Override
    public Optional<Participacao> buscarDoAluno(long id, long alunoId, boolean bloquear) {
        return jdbc.query(BASE + " AND p.id = ? AND p.aluno_id = ?" + (bloquear ? " FOR UPDATE OF p" : ""),
                this::mapear, id, alunoId).stream().findFirst();
    }

    @Override
    public Optional<Participacao> buscarDoProfessor(long id, long professorId, boolean bloquear) {
        return jdbc.query(BASE + " AND p.id = ? AND a.professor_id = ?" + (bloquear ? " FOR UPDATE OF p" : ""),
                this::mapear, id, professorId).stream().findFirst();
    }

    private Participacao mapear(ResultSet rs, int row) throws SQLException {
        return new Participacao(rs.getLong("id"), rs.getLong("avaliacao_id"), rs.getLong("aluno_id"),
                rs.getLong("professor_id"), rs.getString("aluno_nome"), rs.getString("titulo"),
                rs.getString("descricao"), instante(rs, "inicio_em"), instante(rs, "fim_em"),
                instante(rs, "iniciada_em"), instante(rs, "entregue_em"), rs.getBigDecimal("valor_total"),
                rs.getBigDecimal("nota_total"), rs.getLong("quantidade_correcao_pendente"), rs.getLong("quantidade_questoes"));
    }

    private Instant instante(ResultSet rs, String campo) throws SQLException {
        Timestamp value = rs.getTimestamp(campo);
        return value == null ? null : value.toInstant();
    }

    @Override
    public List<QuestaoParaResposta> questoes(long avaliacaoId) {
        return jdbc.query("""
                SELECT aq.id, q.enunciado, tq.codigo, aq.valor, a.id AS alternativa_id, a.texto
                FROM avaliacao_questoes aq JOIN questoes q ON q.id = aq.questao_id
                JOIN tipos_questao tq ON tq.id = q.tipo_questao_id
                LEFT JOIN alternativas a ON a.questao_id = q.id
                WHERE aq.avaliacao_id = ? ORDER BY aq.ordem, a.ordem
                """, rs -> {
            var itens = new LinkedHashMap<Long, Item>();
            while (rs.next()) {
                long id = rs.getLong("id");
                Item item = itens.get(id);
                if (item == null) {
                    item = new Item(id, rs.getString("enunciado"), TipoQuestao.valueOf(rs.getString("codigo")),
                            rs.getBigDecimal("valor"), new ArrayList<>());
                    itens.put(id, item);
                }
                Long alternativaId = rs.getObject("alternativa_id", Long.class);
                if (alternativaId != null) item.alternativas().add(new QuestaoParaResposta.Opcao(alternativaId, rs.getString("texto")));
            }
            return itens.values().stream().map(item -> new QuestaoParaResposta(item.id(), item.enunciado(), item.tipo(),
                    item.valor(), List.copyOf(item.alternativas()))).toList();
        }, avaliacaoId);
    }

    @Override
    public List<RespostaAluno> respostas(long id) {
        return jdbc.query("""
                SELECT avaliacao_questao_id, alternativa_id, texto_resposta FROM respostas
                WHERE participacao_id = ? ORDER BY avaliacao_questao_id
                """, (rs, row) -> new RespostaAluno(rs.getLong("avaliacao_questao_id"),
                rs.getObject("alternativa_id", Long.class), rs.getString("texto_resposta")), id);
    }

    @Override public void iniciar(long id, Instant agora) {
        jdbc.update("UPDATE participacoes SET iniciada_em = COALESCE(iniciada_em, ?) WHERE id = ?", Timestamp.from(agora), id);
    }

    @Override
    public void substituirRespostas(long id, List<RespostaAluno> respostas, Instant agora) {
        jdbc.update("DELETE FROM respostas WHERE participacao_id = ?", id);
        for (RespostaAluno resposta : respostas) {
            jdbc.update("""
                    INSERT INTO respostas (participacao_id, avaliacao_questao_id, alternativa_id, texto_resposta, respondida_em)
                    VALUES (?, ?, ?, ?, ?)
                    """, id, resposta.avaliacaoQuestaoId(), resposta.alternativaId(), resposta.texto(), Timestamp.from(agora));
        }
    }

    @Override public void entregar(long id, Instant agora) {
        jdbc.update("UPDATE participacoes SET entregue_em = ? WHERE id = ?", Timestamp.from(agora), id);
    }

    private record Item(long id, String enunciado, TipoQuestao tipo, BigDecimal valor,
                        List<QuestaoParaResposta.Opcao> alternativas) { }
}
