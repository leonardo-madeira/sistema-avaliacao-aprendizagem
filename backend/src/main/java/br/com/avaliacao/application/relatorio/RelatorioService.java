package br.com.avaliacao.application.relatorio;

import br.com.avaliacao.domain.relatorio.*;
import br.com.avaliacao.domain.usuario.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class RelatorioService {
    private final RelatorioRepository relatorios;
    private final UsuarioRepository usuarios;
    public RelatorioService(RelatorioRepository relatorios, UsuarioRepository usuarios) {
        this.relatorios = relatorios; this.usuarios = usuarios;
    }
    public List<DesempenhoRelatorio> desempenho(long id, FiltroRelatorio filtro) {
        validar(id); return relatorios.desempenho(id, filtro);
    }
    public List<QuestaoRanking> questoes(long id, FiltroRelatorio filtro) {
        validar(id); return relatorios.questoes(id, filtro);
    }
    public List<ParticipacaoRelatorio> participacao(long id, FiltroRelatorio filtro) {
        validar(id); return relatorios.participacao(id, filtro);
    }
    private void validar(long id) {
        if (!usuarios.existeProfessor(id)) throw new IllegalArgumentException("Professor não encontrado.");
    }
}
