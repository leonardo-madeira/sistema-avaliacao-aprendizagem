package br.com.avaliacao.application.usuario;

import br.com.avaliacao.domain.usuario.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

@Service
public class ConfigurarSenhasDemoService {
    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;

    public ConfigurarSenhasDemoService(UsuarioRepository usuarios, PasswordEncoder encoder) {
        this.usuarios = usuarios;
        this.encoder = encoder;
    }

    @Transactional
    public int configurar(String senha) {
        if (senha == null || senha.length() < 8 || senha.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("DEMO_PASSWORD deve ter pelo menos 8 caracteres e no máximo 72 bytes.");
        }
        return usuarios.configurarSenhasDemonstracao(encoder.encode(senha));
    }
}
