package br.com.avaliacao.application.questao;

import br.com.avaliacao.domain.questao.Alternativa;
import br.com.avaliacao.domain.questao.Questao;
import br.com.avaliacao.domain.questao.QuestaoRepository;
import br.com.avaliacao.domain.questao.TipoQuestao;
import br.com.avaliacao.domain.usuario.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class QuestaoService {
    private final QuestaoRepository questoes;
    private final UsuarioRepository usuarios;

    public QuestaoService(QuestaoRepository questoes, UsuarioRepository usuarios) {
        this.questoes = questoes;
        this.usuarios = usuarios;
    }

    @Transactional
    public Questao cadastrar(long autorId, TipoQuestao tipo, String enunciado, List<Alternativa> alternativas) {
        Questao questao = new Questao(null, autorId, tipo, enunciado, alternativas);
        if (!usuarios.existeProfessor(autorId)) {
            throw new IllegalArgumentException("O autor deve ser um professor cadastrado.");
        }
        return questoes.salvar(questao);
    }

    @Transactional(readOnly = true)
    public List<Questao> listar(TipoQuestao tipo, int limite, int deslocamento) {
        if (limite < 1 || limite > 100 || deslocamento < 0) {
            throw new IllegalArgumentException("Use limite entre 1 e 100 e deslocamento não negativo.");
        }
        return questoes.listar(tipo, limite, deslocamento);
    }
}
