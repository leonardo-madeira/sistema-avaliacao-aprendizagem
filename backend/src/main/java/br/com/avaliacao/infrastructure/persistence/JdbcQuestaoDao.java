package br.com.avaliacao.infrastructure.persistence;

import br.com.avaliacao.domain.questao.Alternativa;
import br.com.avaliacao.domain.questao.Questao;
import br.com.avaliacao.domain.questao.QuestaoRepository;
import br.com.avaliacao.domain.questao.TipoQuestao;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

@Repository
public class JdbcQuestaoDao implements QuestaoRepository {
    private final JdbcTemplate jdbc;

    public JdbcQuestaoDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Questao salvar(Questao questao) {
        Long id = Objects.requireNonNull(jdbc.queryForObject("""
                INSERT INTO questoes (tipo_questao_id, autor_id, enunciado)
                SELECT id, ?, ? FROM tipos_questao WHERE codigo = ?
                RETURNING id
                """, Long.class, questao.autorId(), questao.enunciado(), questao.tipo().name()));

        List<Alternativa> alternativas = new ArrayList<>();
        for (int i = 0; i < questao.alternativas().size(); i++) {
            Alternativa alternativa = questao.alternativas().get(i);
            Long alternativaId = jdbc.queryForObject("""
                    INSERT INTO alternativas (questao_id, texto, ordem, correta)
                    VALUES (?, ?, ?, ?) RETURNING id
                    """, Long.class, id, alternativa.texto(), i + 1, alternativa.correta());
            alternativas.add(new Alternativa(alternativaId, alternativa.texto(), alternativa.correta()));
        }
        return new Questao(id, questao.autorId(), questao.tipo(), questao.enunciado(), alternativas);
    }

    @Override
    public List<Questao> listar(TipoQuestao tipo, int limite, int deslocamento) {
        String codigo = tipo == null ? "" : tipo.name();
        return jdbc.query("""
                WITH pagina AS (
                    SELECT q.id, q.autor_id, q.enunciado, tq.codigo
                    FROM questoes q
                    JOIN tipos_questao tq ON tq.id = q.tipo_questao_id
                    WHERE (? = '' OR tq.codigo = ?)
                    ORDER BY q.id
                    LIMIT ? OFFSET ?
                )
                SELECT p.*, a.id AS alternativa_id, a.texto AS alternativa_texto, a.correta
                FROM pagina p
                LEFT JOIN alternativas a ON a.questao_id = p.id
                ORDER BY p.id, a.ordem
                """, rs -> {
            var registros = new LinkedHashMap<Long, RegistroQuestao>();
            while (rs.next()) {
                long id = rs.getLong("id");
                RegistroQuestao registro = registros.get(id);
                if (registro == null) {
                    registro = new RegistroQuestao(id, rs.getLong("autor_id"),
                            TipoQuestao.valueOf(rs.getString("codigo")), rs.getString("enunciado"));
                    registros.put(id, registro);
                }
                Long alternativaId = rs.getObject("alternativa_id", Long.class);
                if (alternativaId != null) {
                    registro.alternativas.add(new Alternativa(alternativaId,
                            rs.getString("alternativa_texto"), rs.getBoolean("correta")));
                }
            }
            return registros.values().stream().map(RegistroQuestao::paraDominio).toList();
        }, codigo, codigo, limite, deslocamento);
    }

    private static final class RegistroQuestao {
        private final long id;
        private final long autorId;
        private final TipoQuestao tipo;
        private final String enunciado;
        private final List<Alternativa> alternativas = new ArrayList<>();

        private RegistroQuestao(long id, long autorId, TipoQuestao tipo, String enunciado) {
            this.id = id;
            this.autorId = autorId;
            this.tipo = tipo;
            this.enunciado = enunciado;
        }

        private Questao paraDominio() {
            return new Questao(id, autorId, tipo, enunciado, alternativas);
        }
    }
}
