package br.com.avaliacao.infrastructure.security;

import br.com.avaliacao.domain.usuario.CredenciaisUsuario;
import br.com.avaliacao.domain.usuario.UsuarioAtual;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public final class UsuarioPrincipal implements UserDetails, CredentialsContainer {
    private final long id;
    private final String nome;
    private final String email;
    private final String perfil;
    private String senhaHash;

    public UsuarioPrincipal(CredenciaisUsuario usuario) {
        id = usuario.id();
        nome = usuario.nome();
        email = usuario.email();
        perfil = usuario.perfil();
        senhaHash = usuario.senhaHash();
    }

    public long getId() { return id; }
    public String getNome() { return nome; }
    public String getPerfil() { return perfil; }
    public UsuarioAtual getUsuario() { return new UsuarioAtual(id, nome, email, perfil); }
    @Override public String getUsername() { return email; }
    @Override public String getPassword() { return senhaHash; }
    @Override public void eraseCredentials() { senhaHash = null; }
    @Override public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + perfil));
    }
}
