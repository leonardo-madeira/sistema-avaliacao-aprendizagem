package br.com.avaliacao.api.questao;

import br.com.avaliacao.application.questao.QuestaoService;
import br.com.avaliacao.domain.questao.TipoQuestao;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/questoes")
public class QuestaoController {
    private final QuestaoService service;

    public QuestaoController(QuestaoService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public QuestaoResponse cadastrar(@Valid @RequestBody CriarQuestaoRequest request) {
        return QuestaoResponse.de(service.cadastrar(request.autorId(), request.tipo(),
                request.enunciado(), request.alternativasDominio()));
    }

    @GetMapping
    public List<QuestaoResponse> listar(@RequestParam(required = false) TipoQuestao tipo,
                                       @RequestParam(defaultValue = "50") int limite,
                                       @RequestParam(defaultValue = "0") int deslocamento) {
        return service.listar(tipo, limite, deslocamento).stream().map(QuestaoResponse::de).toList();
    }
}
