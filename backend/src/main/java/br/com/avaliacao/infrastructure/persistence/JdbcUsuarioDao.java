package br.com.avaliacao.infrastructure.persistence;

import br.com.avaliacao.domain.usuario.UsuarioRepository;
import br.com.avaliacao.domain.usuario.CredenciaisUsuario;
import java.util.Optional;
import java.util.List;
import java.util.Map;
import br.com.avaliacao.domain.usuario.AlunoResumo;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcUsuarioDao implements UsuarioRepository {
    private final JdbcTemplate jdbc;

    public JdbcUsuarioDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean existeProfessor(long usuarioId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM usuarios WHERE id = ? AND perfil = 'PROFESSOR')
                """, Boolean.class, usuarioId));
    }

    @Override
    public Optional<CredenciaisUsuario> buscarPorEmail(String email) {
        return jdbc.query("""
                SELECT id, nome, email, senha_hash, perfil FROM usuarios WHERE email = ?
                """, (rs, row) -> new CredenciaisUsuario(rs.getLong("id"), rs.getString("nome"),
                rs.getString("email"), rs.getString("senha_hash"), rs.getString("perfil")), email)
                .stream().findFirst();
    }

    @Override
    public int configurarSenhasDemonstracao(String senhaHash) {
        return jdbc.update("""
                UPDATE usuarios SET senha_hash = ? WHERE senha_hash = '!conta_demonstracao_sem_senha'
                """, senhaHash);
    }

    @Override
    public List<AlunoResumo> listarAlunos() {
        return jdbc.query("""
                SELECT id, nome, email FROM usuarios WHERE perfil = 'ALUNO' ORDER BY nome, id
                """, (rs, row) -> new AlunoResumo(rs.getLong("id"), rs.getString("nome"), rs.getString("email")));
    }

    @Override
    public boolean saoAlunos(List<Long> ids) {
        if (ids.isEmpty()) return true;
        Long count = new NamedParameterJdbcTemplate(jdbc).queryForObject("""
                SELECT COUNT(*) FROM usuarios WHERE perfil = 'ALUNO' AND id IN (:ids)
                """, Map.of("ids", ids), Long.class);
        return count != null && count == ids.size();
    }
}
