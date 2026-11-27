package br.com.avaliacao.infrastructure.persistence;

import br.com.avaliacao.domain.correcao.*;
import br.com.avaliacao.domain.questao.TipoQuestao;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.sql.Timestamp;
import java.util.List;

@Repository
public class JdbcCorrecaoDao implements CorrecaoRepository {
    private final JdbcTemplate jdbc;
    public JdbcCorrecaoDao(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public boolean avaliacaoDoProfessor(long id, long professorId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM avaliacoes WHERE id = ? AND professor_id = ? AND status = 'PUBLICADA')
                """, Boolean.class, id, professorId));
    }

    @Override
    public List<EntregaResumo> entregas(long avaliacaoId) {
        return jdbc.query("""
                SELECT p.id, u.nome, p.entregue_em, rp.valor_total, rp.nota_total,
                       CASE WHEN p.entregue_em IS NOT NULL THEN rp.nota_parcial ELSE NULL END AS nota_parcial,
                       CASE WHEN p.entregue_em IS NOT NULL THEN rp.quantidade_correcao_pendente ELSE 0 END AS pendentes
                FROM participacoes p JOIN usuarios u ON u.id = p.aluno_id
                JOIN resultados_participacoes rp ON rp.participacao_id = p.id
                WHERE p.avaliacao_id = ? ORDER BY u.nome, p.id
                """, (rs, row) -> {
            Timestamp entregue = rs.getTimestamp("entregue_em");
            return new EntregaResumo(rs.getLong("id"), rs.getString("nome"), entregue == null ? null : entregue.toInstant(),
                    rs.getBigDecimal("valor_total"), rs.getBigDecimal("nota_parcial"), rs.getBigDecimal("nota_total"), rs.getLong("pendentes"));
        }, avaliacaoId);
    }

    @Override
    public List<RespostaParaCorrecao> respostas(long id) {
        return jdbc.query("""
                SELECT r.id AS resposta_id, aq.id, q.enunciado, tq.codigo, aq.valor,
                       COALESCE(r.texto_resposta, a.texto) AS resposta, r.nota_obtida
                FROM participacoes p JOIN avaliacao_questoes aq ON aq.avaliacao_id = p.avaliacao_id
                JOIN questoes q ON q.id = aq.questao_id JOIN tipos_questao tq ON tq.id = q.tipo_questao_id
                LEFT JOIN respostas r ON r.participacao_id = p.id AND r.avaliacao_questao_id = aq.id
                LEFT JOIN alternativas a ON a.id = r.alternativa_id
                WHERE p.id = ? ORDER BY aq.ordem
                """, (rs, row) -> new RespostaParaCorrecao(rs.getObject("resposta_id", Long.class), rs.getLong("id"),
                rs.getString("enunciado"), TipoQuestao.valueOf(rs.getString("codigo")), rs.getBigDecimal("valor"),
                rs.getString("resposta"), rs.getBigDecimal("nota_obtida")), id);
    }

    @Override
    public void atribuir(long id, NotaDiscursiva nota) {
        int alteradas = jdbc.update("UPDATE respostas SET nota_obtida = ? WHERE id = ? AND participacao_id = ?",
                nota.nota(), nota.respostaId(), id);
        if (alteradas != 1) throw new IllegalArgumentException("Resposta não encontrada nesta entrega.");
    }
}
