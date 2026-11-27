package br.com.avaliacao.api.relatorio;

import br.com.avaliacao.domain.usuario.CredenciaisUsuario;
import br.com.avaliacao.infrastructure.security.UsuarioPrincipal;
import br.com.avaliacao.application.usuario.ConfigurarSenhasDemoService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.mock.web.MockHttpSession;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.UUID;
import java.sql.Timestamp;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import com.jayway.jsonpath.JsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@EnabledIfEnvironmentVariable(named = "TEST_DATABASE_URL", matches = ".+")
class RelatorioIntegrationTest {
    private static final String SCHEMA = "teste_" + UUID.randomUUID().toString().replace("-", "");
    private static final String URL = System.getenv("TEST_DATABASE_URL");
    private static final String USER = System.getenv().getOrDefault("TEST_DATABASE_USER", "avaliacao");
    private static final String PASSWORD = System.getenv().getOrDefault("TEST_DATABASE_PASSWORD", "");

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder encoder;
    @Autowired ConfigurarSenhasDemoService senhasDemo;

    @DynamicPropertySource
    static void configurar(DynamicPropertyRegistry registry) throws Exception {
        try (var connection = DriverManager.getConnection(URL, USER, PASSWORD);
             var statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA " + SCHEMA);
            statement.execute("SET search_path TO " + SCHEMA);
            statement.execute(Files.readString(Path.of("../database/schema.sql")));
            statement.execute(Files.readString(Path.of("../database/carga.sql")));
        }
        registry.add("spring.datasource.url", () -> URL + (URL.contains("?") ? "&" : "?") + "currentSchema=" + SCHEMA);
        registry.add("spring.datasource.username", () -> USER);
        registry.add("spring.datasource.password", () -> PASSWORD);
    }

    @AfterAll
    static void limpar() throws Exception {
        try (var connection = DriverManager.getConnection(URL, USER, PASSWORD);
             var statement = connection.createStatement()) {
            statement.execute("DROP SCHEMA IF EXISTS " + SCHEMA + " CASCADE");
        }
    }

    private long prova;
    private long item1, item2, itemTexto, p1, p2;

    @BeforeEach
    void dadosControlados() {
        prova = criarProva("Relatorio controlado");
        item1 = item(1, 2); item2 = item(2, 3); itemTexto = item(21, 5);
        p1 = participante(3, true); p2 = participante(4, true);
        long emAndamento = participante(5, false);
        objetiva(p1, item1, 1, true); objetiva(p1, item2, 2, false); texto(p1, 3);
        objetiva(p2, item2, 2, true); texto(p2, 5);
        objetiva(emAndamento, item1, 1, true);
    }

    @Test
    void calculaMediaSomenteDeEntregasFinaisSemMultiplicarAlunos() throws Exception {
        consultar("desempenho", "")
                .andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].convidados").value(3))
                .andExpect(jsonPath("$[0].entregues").value(2)).andExpect(jsonPath("$[0].notasConsideradas").value(2))
                .andExpect(jsonPath("$[0].valorTotal").value(10)).andExpect(jsonPath("$[0].mediaNota").value(6.5))
                .andExpect(jsonPath("$[0].mediaPercentual").value(65)).andExpect(jsonPath("$[0].mediaGeralPercentual").value(65))
                .andExpect(jsonPath("$[0].menorNota").value(5)).andExpect(jsonPath("$[0].maiorNota").value(8));
    }

    @Test
    void naoTransformaCorrecaoPendenteEmZero() throws Exception {
        jdbc.update("UPDATE respostas SET nota_obtida = NULL WHERE participacao_id = ? AND avaliacao_questao_id = ?", p1, itemTexto);
        consultar("desempenho", "").andExpect(jsonPath("$[0].notasConsideradas").value(1))
                .andExpect(jsonPath("$[0].aguardandoCorrecao").value(1)).andExpect(jsonPath("$[0].mediaNota").value(8));
    }

    @Test
    void zeroCorrigidoContinuaNaMedia() throws Exception {
        jdbc.update("UPDATE respostas SET alternativa_id = (SELECT id FROM alternativas WHERE questao_id = 1 AND NOT correta ORDER BY ordem LIMIT 1) WHERE participacao_id = ? AND avaliacao_questao_id = ?", p1, item1);
        jdbc.update("UPDATE respostas SET nota_obtida = 0 WHERE participacao_id = ? AND avaliacao_questao_id = ?", p1, itemTexto);
        consultar("desempenho", "&notaMax=0").andExpect(jsonPath("$[0].notasConsideradas").value(1))
                .andExpect(jsonPath("$[0].mediaNota").value(0)).andExpect(jsonPath("$[0].mediaPercentual").value(0));
    }

    @Test
    void aplicaFaixaDeNotasEHavingSemFiltrarContagemDeConvidados() throws Exception {
        consultar("desempenho", "&notaMin=70&notaMax=100")
                .andExpect(jsonPath("$[0].mediaNota").value(8)).andExpect(jsonPath("$[0].notasConsideradas").value(1))
                .andExpect(jsonPath("$[0].convidados").value(3));
        consultar("desempenho", "&minimoCorrigidas=3").andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void rankingIncluiBrancosSomenteDasEntregasEPreservaEmpates() throws Exception {
        consultar("questoes", "").andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].questaoId").value(1)).andExpect(jsonPath("$[0].oportunidades").value(2))
                .andExpect(jsonPath("$[0].respondidas").value(1)).andExpect(jsonPath("$[0].emBranco").value(1))
                .andExpect(jsonPath("$[0].percentualErros").value(50)).andExpect(jsonPath("$[1].percentualErros").value(50))
                .andExpect(jsonPath("$[0].posicao").value(1)).andExpect(jsonPath("$[1].posicao").value(1));
        consultar("questoes", "&limite=1").andExpect(jsonPath("$.length()").value(1));
        consultar("questoes", "&minimoRespostas=3").andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void contaParticipacaoSemMultiplicarPelaQuantidadeDeQuestoes() throws Exception {
        consultar("participacao", "").andExpect(jsonPath("$[0].convidados").value(3))
                .andExpect(jsonPath("$[0].entregues").value(2)).andExpect(jsonPath("$[0].pendentes").value(1))
                .andExpect(jsonPath("$[0].emAndamento").value(1))
                .andExpect(jsonPath("$[0].percentualEntregas").value(org.hamcrest.Matchers.closeTo(66.666666, 0.00001), Double.class));
    }

    @Test
    void preservaAvaliacoesSemNotasESemParticipantes() throws Exception {
        long vazia = criarProva("Sem participantes");
        for (String name : new String[]{"desempenho", "participacao"}) {
            mvc.perform(get("/api/relatorios/" + name).with(user(professor(1))).param("avaliacaoIds", String.valueOf(vazia)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].convidados").value(0));
        }
        mvc.perform(get("/api/relatorios/desempenho").with(user(professor(1))).param("avaliacaoIds", String.valueOf(vazia)))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].mediaNota").isEmpty());
    }

    @Test
    void filtraDatasBuscaEListaDeAvaliacoes() throws Exception {
        consultar("participacao", "&inicio=2030-01-02&fim=2030-01-02&busca=CONTROLADO")
                .andExpect(jsonPath("$.length()").value(1));
        consultar("participacao", "&inicio=2030-01-03").andExpect(jsonPath("$.length()").value(0));
        consultar("participacao", "&busca=inexistente").andExpect(jsonPath("$.length()").value(0));
        long segunda = criarProva("Outra prova");
        mvc.perform(get("/api/relatorios/participacao").with(user(professor(1))).param("avaliacaoIds", prova + "," + segunda))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void respeitaDiaDeBrasiliaNaViradaUtc() throws Exception {
        jdbc.update("UPDATE avaliacoes SET inicio_em = ?, fim_em = ? WHERE id = ?", Timestamp.from(Instant.parse("2030-01-02T01:00:00Z")), Timestamp.from(Instant.parse("2030-01-02T02:00:00Z")), prova);
        consultar("participacao", "&inicio=2030-01-01&fim=2030-01-01").andExpect(jsonPath("$.length()").value(1));
        consultar("participacao", "&inicio=2030-01-02").andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void naoPermiteConsultarDadosDeOutroProfessor() throws Exception {
        for (String name : new String[]{"desempenho", "questoes", "participacao"}) {
            mvc.perform(get("/api/relatorios/" + name).with(user(professor(2))).param("avaliacaoIds", String.valueOf(prova)).param("professorId", "1"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
            mvc.perform(get("/api/relatorios/" + name).with(user(aluno()))).andExpect(status().isForbidden());
            mvc.perform(get("/api/relatorios/" + name)).andExpect(status().isUnauthorized());
        }
    }

    @Test
    void rejeitaFiltrosInvalidos() throws Exception {
        for (String params : new String[]{"&inicio=2030-02-01&fim=2030-01-01", "&notaMin=90&notaMax=10", "&limite=51", "&minimoRespostas=0", "&minimoCorrigidas=-1", "&inicio=nao-e-data", "&avaliacaoIds=abc"}) {
            mvc.perform(get("/api/relatorios/desempenho?avaliacaoIds=" + prova + params).with(user(professor(1))))
                    .andExpect(status().isBadRequest());
        }
    }

    private org.springframework.test.web.servlet.ResultActions consultar(String name, String params) throws Exception {
        return mvc.perform(get("/api/relatorios/" + name + "?avaliacaoIds=" + prova + params).with(user(professor(1))))
                .andExpect(status().isOk());
    }
    private long criarProva(String titulo) {
        return jdbc.queryForObject("INSERT INTO avaliacoes(professor_id,titulo,inicio_em,fim_em,status) VALUES (1,?,?,?,'PUBLICADA') RETURNING id", Long.class,
                titulo, Timestamp.from(Instant.parse("2030-01-02T12:00:00Z")), Timestamp.from(Instant.parse("2030-01-02T14:00:00Z")));
    }
    private long item(long questao, int valor) {
        return jdbc.queryForObject("INSERT INTO avaliacao_questoes(avaliacao_id,questao_id,ordem,valor) VALUES (?,?,?,?) RETURNING id", Long.class, prova, questao, questao == 21 ? 3 : questao, valor);
    }
    private long participante(long aluno, boolean entregue) {
        return jdbc.queryForObject("INSERT INTO participacoes(avaliacao_id,aluno_id,iniciada_em,entregue_em) VALUES (?,?,?,?) RETURNING id", Long.class, prova, aluno,
                Timestamp.from(Instant.parse("2030-01-02T12:10:00Z")), entregue ? Timestamp.from(Instant.parse("2030-01-02T13:00:00Z")) : null);
    }
    private void objetiva(long p, long item, long questao, boolean certa) {
        jdbc.update("INSERT INTO respostas(participacao_id,avaliacao_questao_id,alternativa_id) VALUES (?,?,(SELECT id FROM alternativas WHERE questao_id = ? AND correta = ? ORDER BY ordem LIMIT 1))", p, item, questao, certa);
    }
    private void texto(long p, int nota) { jdbc.update("INSERT INTO respostas(participacao_id,avaliacao_questao_id,texto_resposta,nota_obtida) VALUES (?,?,'Resposta de teste',?)", p, itemTexto, nota); }
    private UsuarioPrincipal professor(long id) { return new UsuarioPrincipal(new CredenciaisUsuario(id, "Professor", "professor@example.test", null, "PROFESSOR")); }
    private UsuarioPrincipal aluno() { return new UsuarioPrincipal(new CredenciaisUsuario(3, "Ana", "aluno01@example.test", null, "ALUNO")); }
}
