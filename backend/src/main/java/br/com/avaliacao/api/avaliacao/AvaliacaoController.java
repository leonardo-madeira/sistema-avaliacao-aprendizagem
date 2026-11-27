package br.com.avaliacao.api.avaliacao;

import br.com.avaliacao.application.avaliacao.AvaliacaoService;
import br.com.avaliacao.domain.avaliacao.AvaliacaoResumo;
import br.com.avaliacao.domain.usuario.AlunoResumo;
import br.com.avaliacao.domain.usuario.UsuarioAtual;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class AvaliacaoController {
    private final AvaliacaoService service;
    public AvaliacaoController(AvaliacaoService service) { this.service = service; }

    @GetMapping("/avaliacoes")
    public List<AvaliacaoResumo> listar(@AuthenticationPrincipal(expression = "usuario") UsuarioAtual usuario) {
        return service.listar(usuario.id());
    }

    @GetMapping("/avaliacoes/{id}")
    public AvaliacaoResponse buscar(@PathVariable long id,
                                    @AuthenticationPrincipal(expression = "usuario") UsuarioAtual usuario) {
        return AvaliacaoResponse.de(service.buscar(id, usuario.id()));
    }

    @PostMapping("/avaliacoes")
    @ResponseStatus(HttpStatus.CREATED)
    public AvaliacaoResponse criar(@Valid @RequestBody SalvarAvaliacaoRequest request,
                                   @AuthenticationPrincipal(expression = "usuario") UsuarioAtual usuario) {
        return AvaliacaoResponse.de(service.salvar(request.paraDominio(null, usuario.id())));
    }

    @PutMapping("/avaliacoes/{id}")
    public AvaliacaoResponse alterar(@PathVariable long id, @Valid @RequestBody SalvarAvaliacaoRequest request,
                                     @AuthenticationPrincipal(expression = "usuario") UsuarioAtual usuario) {
        return AvaliacaoResponse.de(service.salvar(request.paraDominio(id, usuario.id())));
    }

    @PostMapping("/avaliacoes/{id}/publicar")
    public AvaliacaoResponse publicar(@PathVariable long id,
                                      @AuthenticationPrincipal(expression = "usuario") UsuarioAtual usuario) {
        return AvaliacaoResponse.de(service.publicar(id, usuario.id()));
    }

    @GetMapping("/alunos")
    public List<AlunoResumo> alunos() { return service.listarAlunos(); }
}
