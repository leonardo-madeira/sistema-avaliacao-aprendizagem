package br.com.avaliacao.api.correcao;

import br.com.avaliacao.application.correcao.CorrecaoService;
import br.com.avaliacao.domain.correcao.EntregaResumo;
import br.com.avaliacao.domain.usuario.UsuarioAtual;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api")
public class CorrecaoController {
    private final CorrecaoService service;
    public CorrecaoController(CorrecaoService service) { this.service = service; }

    @GetMapping("/avaliacoes/{id}/entregas")
    public List<EntregaResumo> listar(@PathVariable long id,
            @AuthenticationPrincipal(expression = "usuario") UsuarioAtual usuario) {
        return service.listar(id, usuario.id());
    }

    @GetMapping("/correcoes/{id}")
    public CorrecaoService.Detalhe buscar(@PathVariable long id,
            @AuthenticationPrincipal(expression = "usuario") UsuarioAtual usuario) {
        return service.buscar(id, usuario.id());
    }

    @PutMapping("/correcoes/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void salvar(@PathVariable long id, @Valid @RequestBody SalvarNotasRequest request,
            @AuthenticationPrincipal(expression = "usuario") UsuarioAtual usuario) {
        service.salvar(id, usuario.id(), request.paraDominio());
    }
}
