package br.com.avaliacao.infrastructure.persistence;

import br.com.avaliacao.domain.usuario.UsuarioRepository;
import br.com.avaliacao.domain.usuario.CredenciaisUsuario;
import java.util.Optional;
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
}
