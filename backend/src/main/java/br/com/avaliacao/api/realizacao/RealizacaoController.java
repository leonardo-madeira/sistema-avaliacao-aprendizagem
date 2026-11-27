package br.com.avaliacao.api.realizacao;

import br.com.avaliacao.application.realizacao.RealizacaoService;
import br.com.avaliacao.domain.usuario.UsuarioAtual;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/minhas-avaliacoes")
public class RealizacaoController {
    private final RealizacaoService service;
    public RealizacaoController(RealizacaoService service) { this.service = service; }

    @GetMapping
    public List<RealizacaoService.Resumo> listar(@AuthenticationPrincipal(expression = "usuario") UsuarioAtual usuario) {
        return service.listar(usuario.id());
    }

    @GetMapping("/{id}")
    public RealizacaoService.Detalhe buscar(@PathVariable long id,
            @AuthenticationPrincipal(expression = "usuario") UsuarioAtual usuario) {
        return service.buscar(id, usuario.id());
    }

    @PostMapping("/{id}/iniciar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void iniciar(@PathVariable long id, @AuthenticationPrincipal(expression = "usuario") UsuarioAtual usuario) {
        service.iniciar(id, usuario.id());
    }

    @PutMapping("/{id}/respostas")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void salvar(@PathVariable long id, @Valid @RequestBody SalvarRespostasRequest request,
            @AuthenticationPrincipal(expression = "usuario") UsuarioAtual usuario) {
        service.salvar(id, usuario.id(), request.paraDominio(), false);
    }

    @PostMapping("/{id}/entregar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void entregar(@PathVariable long id, @Valid @RequestBody SalvarRespostasRequest request,
            @AuthenticationPrincipal(expression = "usuario") UsuarioAtual usuario) {
        service.salvar(id, usuario.id(), request.paraDominio(), true);
    }
}
