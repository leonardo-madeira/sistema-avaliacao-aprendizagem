package br.com.avaliacao.api.auth;

import br.com.avaliacao.domain.usuario.UsuarioAtual;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @GetMapping("/csrf")
    public CsrfResponse csrf(CsrfToken token) {
        return new CsrfResponse(token.getToken(), token.getHeaderName());
    }

    @GetMapping("/me")
    public UsuarioAtual atual(@AuthenticationPrincipal(expression = "usuario") UsuarioAtual usuario) {
        return usuario;
    }

    public record CsrfResponse(String token, String headerName) { }
}
