package br.com.avaliacao.infrastructure.persistence;

import br.com.avaliacao.domain.usuario.UsuarioRepository;
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
}
