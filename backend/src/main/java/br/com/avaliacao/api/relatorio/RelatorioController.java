package br.com.avaliacao.api.relatorio;

import br.com.avaliacao.application.relatorio.RelatorioService;
import br.com.avaliacao.domain.relatorio.*;
import br.com.avaliacao.domain.usuario.UsuarioAtual;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/relatorios")
public class RelatorioController {
    private final RelatorioService service;
    public RelatorioController(RelatorioService service) { this.service = service; }
    @GetMapping("/desempenho")
    public List<DesempenhoRelatorio> desempenho(@ModelAttribute RelatorioQuery query,
            @AuthenticationPrincipal(expression = "usuario") UsuarioAtual usuario) {
        return service.desempenho(usuario.id(), query.paraDominio());
    }
    @GetMapping("/questoes")
    public List<QuestaoRanking> questoes(@ModelAttribute RelatorioQuery query,
            @AuthenticationPrincipal(expression = "usuario") UsuarioAtual usuario) {
        return service.questoes(usuario.id(), query.paraDominio());
    }
    @GetMapping("/participacao")
    public List<ParticipacaoRelatorio> participacao(@ModelAttribute RelatorioQuery query,
            @AuthenticationPrincipal(expression = "usuario") UsuarioAtual usuario) {
        return service.participacao(usuario.id(), query.paraDominio());
    }
}
