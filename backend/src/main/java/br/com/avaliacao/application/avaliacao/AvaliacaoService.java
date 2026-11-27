package br.com.avaliacao.application.avaliacao;

import br.com.avaliacao.domain.ConflitoException;
import br.com.avaliacao.domain.NaoEncontradoException;
import br.com.avaliacao.domain.avaliacao.Avaliacao;
import br.com.avaliacao.domain.avaliacao.AvaliacaoRepository;
import br.com.avaliacao.domain.avaliacao.AvaliacaoResumo;
import br.com.avaliacao.domain.avaliacao.QuestaoAvaliacao;
import br.com.avaliacao.domain.usuario.AlunoResumo;
import br.com.avaliacao.domain.usuario.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AvaliacaoService {
    private final AvaliacaoRepository avaliacoes;
    private final UsuarioRepository usuarios;

    public AvaliacaoService(AvaliacaoRepository avaliacoes, UsuarioRepository usuarios) {
        this.avaliacoes = avaliacoes;
        this.usuarios = usuarios;
    }

    @Transactional
    public Avaliacao salvar(Avaliacao avaliacao) {
        if (avaliacao.status() != Avaliacao.Status.RASCUNHO) {
            throw new IllegalArgumentException("Use a publicação após salvar o rascunho.");
        }
        if (!usuarios.existeProfessor(avaliacao.professorId())) {
            throw new IllegalArgumentException("O responsável deve ser um professor cadastrado.");
        }
        if (avaliacao.id() != null) exigirRascunho(carregar(avaliacao.id(), avaliacao.professorId(), true));
        validarReferencias(avaliacao);
        return avaliacoes.salvar(avaliacao);
    }

    @Transactional
    public Avaliacao publicar(long id, long professorId) {
        Avaliacao atual = carregar(id, professorId, true);
        exigirRascunho(atual);
        Avaliacao publicada = atual.publicar();
        validarReferencias(publicada);
        avaliacoes.publicar(id);
        return publicada;
    }

    @Transactional(readOnly = true)
    public Avaliacao buscar(long id, long professorId) {
        return carregar(id, professorId, false);
    }

    @Transactional(readOnly = true)
    public List<AvaliacaoResumo> listar(long professorId) {
        return avaliacoes.listar(professorId);
    }

    @Transactional(readOnly = true)
    public List<AlunoResumo> listarAlunos() { return usuarios.listarAlunos(); }

    private Avaliacao carregar(long id, long professorId, boolean bloquear) {
        return (bloquear ? avaliacoes.buscarParaAlteracao(id, professorId) : avaliacoes.buscar(id, professorId))
                .orElseThrow(() -> new NaoEncontradoException("Avaliação não encontrada."));
    }

    private void exigirRascunho(Avaliacao avaliacao) {
        if (avaliacao.status() != Avaliacao.Status.RASCUNHO) {
            throw new ConflitoException("Uma avaliação publicada não pode ser alterada.");
        }
    }

    private void validarReferencias(Avaliacao avaliacao) {
        if (!avaliacoes.questoesProntas(avaliacao.questoes().stream().map(QuestaoAvaliacao::questaoId).toList())) {
            throw new IllegalArgumentException("Confira as questões: objetivas precisam de pelo menos duas alternativas e um gabarito.");
        }
        if (!usuarios.saoAlunos(avaliacao.alunos())) {
            throw new IllegalArgumentException("Selecione somente alunos cadastrados como participantes.");
        }
    }
}
