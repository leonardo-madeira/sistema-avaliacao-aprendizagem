package br.com.avaliacao.infrastructure.persistence;

import br.com.avaliacao.domain.relatorio.*;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import java.sql.Date;
import java.util.List;

@Repository
public class JdbcRelatorioDao implements RelatorioRepository {
    private final NamedParameterJdbcTemplate jdbc;
    private final SqlRelatorios sql;
    public JdbcRelatorioDao(NamedParameterJdbcTemplate jdbc, SqlRelatorios sql) { this.jdbc = jdbc; this.sql = sql; }

    private MapSqlParameterSource parametros(long professorId, FiltroRelatorio f) {
        return new MapSqlParameterSource().addValue("professorId", professorId)
                .addValue("inicio", f.inicio() == null ? null : Date.valueOf(f.inicio()))
                .addValue("fim", f.fim() == null ? null : Date.valueOf(f.fim()))
                .addValue("busca", f.busca()).addValue("todasAvaliacoes", f.avaliacaoIds().isEmpty())
                .addValue("avaliacaoIds", f.avaliacaoIds().isEmpty() ? List.of(-1L) : f.avaliacaoIds())
                .addValue("notaMin", f.notaMin()).addValue("notaMax", f.notaMax())
                .addValue("minimoCorrigidas", f.minimoCorrigidas()).addValue("minimoRespostas", f.minimoRespostas())
                .addValue("limite", f.limite());
    }

    @Override
    public List<DesempenhoRelatorio> desempenho(long professorId, FiltroRelatorio filtro) {
        return jdbc.query(sql.obter("desempenho"), parametros(professorId, filtro), (rs, row) ->
                new DesempenhoRelatorio(rs.getLong("avaliacao_id"), rs.getString("titulo"),
                        rs.getTimestamp("inicio_em").toInstant(), rs.getBigDecimal("valor_total"),
                        rs.getLong("convidados"), rs.getLong("entregues"), rs.getLong("notas_consideradas"),
                        rs.getLong("aguardando_correcao"), rs.getBigDecimal("media_nota"),
                        rs.getBigDecimal("media_percentual"), rs.getBigDecimal("menor_nota"),
                        rs.getBigDecimal("maior_nota"), rs.getBigDecimal("media_geral_percentual")));
    }

    @Override
    public List<QuestaoRanking> questoes(long professorId, FiltroRelatorio filtro) {
        return jdbc.query(sql.obter("questoes"), parametros(professorId, filtro), (rs, row) ->
                new QuestaoRanking(rs.getLong("posicao"), rs.getLong("questao_id"), rs.getString("enunciado"),
                        rs.getLong("oportunidades"), rs.getLong("respondidas"), rs.getLong("em_branco"),
                        rs.getLong("erros"), rs.getBigDecimal("percentual_erros")));
    }

    @Override
    public List<ParticipacaoRelatorio> participacao(long professorId, FiltroRelatorio filtro) {
        return jdbc.query(sql.obter("participacao"), parametros(professorId, filtro), (rs, row) ->
                new ParticipacaoRelatorio(rs.getLong("avaliacao_id"), rs.getString("titulo"),
                        rs.getTimestamp("inicio_em").toInstant(), rs.getLong("convidados"), rs.getLong("entregues"),
                        rs.getLong("pendentes"), rs.getLong("em_andamento"), rs.getBigDecimal("percentual_entregas")));
    }
}
