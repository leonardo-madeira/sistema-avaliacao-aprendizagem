package br.com.avaliacao.domain.usuario;

public record CredenciaisUsuario(long id, String nome, String email, String senhaHash, String perfil) {
}
