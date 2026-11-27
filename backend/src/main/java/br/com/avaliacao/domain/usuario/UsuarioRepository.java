package br.com.avaliacao.domain.usuario;

import java.util.Optional;
import java.util.List;

public interface UsuarioRepository {
    boolean existeProfessor(long usuarioId);
    Optional<CredenciaisUsuario> buscarPorEmail(String email);
    int configurarSenhasDemonstracao(String senhaHash);
    List<AlunoResumo> listarAlunos();
    boolean saoAlunos(List<Long> ids);
}
