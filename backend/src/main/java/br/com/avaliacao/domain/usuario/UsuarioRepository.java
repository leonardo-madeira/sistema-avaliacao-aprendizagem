package br.com.avaliacao.domain.usuario;

import java.util.Optional;

public interface UsuarioRepository {
    boolean existeProfessor(long usuarioId);
    Optional<CredenciaisUsuario> buscarPorEmail(String email);
    int configurarSenhasDemonstracao(String senhaHash);
}
