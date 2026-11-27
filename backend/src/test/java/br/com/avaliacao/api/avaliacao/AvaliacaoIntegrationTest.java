package br.com.avaliacao.api.avaliacao;

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
class AvaliacaoIntegrationTest {
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

    @Test
    void criaAvaliacoesComQuestoesExistentesEParticipantes() throws Exception {
        long id = criar(completa());
        mvc.perform(get("/api/avaliacoes/" + id).with(user(professor(1))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.professorId").value(1))
                .andExpect(jsonPath("$.status").value("RASCUNHO"))
                .andExpect(jsonPath("$.questoes[0].questaoId").value(1))
                .andExpect(jsonPath("$.questoes[1].questaoId").value(21))
                .andExpect(jsonPath("$.alunos.length()").value(2))
                .andExpect(jsonPath("$.valorTotal").value(10));
        assertEquals(52, quantidade("avaliacao_questoes"));
        assertEquals(102, quantidade("participacoes"));
    }

    @Test
    void alteraOrdemPontuacaoEAlunosDoRascunho() throws Exception {
        long id = criar(completa());
        String alterada = body("Prova alterada", "[{\"questaoId\":21,\"valor\":4.5},{\"questaoId\":1,\"valor\":1.5}]", "[5]");
        mvc.perform(put("/api/avaliacoes/" + id).with(user(professor(1))).with(csrf())
                .contentType("application/json").content(alterada))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.questoes[0].questaoId").value(21))
                .andExpect(jsonPath("$.questoes[1].valor").value(1.5))
                .andExpect(jsonPath("$.alunos[0]").value(5))
                .andExpect(jsonPath("$.valorTotal").value(6));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM participacoes WHERE avaliacao_id = ?", Integer.class, id));
        assertEquals(21L, jdbc.queryForObject("SELECT questao_id FROM avaliacao_questoes WHERE avaliacao_id = ? AND ordem = 1", Long.class, id));
    }

    @Test
    void publicaEBloqueiaAlteracoesPosteriores() throws Exception {
        long id = criar(completa());
        mvc.perform(post("/api/avaliacoes/" + id + "/publicar").with(user(professor(1))).with(csrf()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PUBLICADA"));
        mvc.perform(put("/api/avaliacoes/" + id).with(user(professor(1))).with(csrf())
                .contentType("application/json").content(completa())).andExpect(status().isConflict());
        mvc.perform(post("/api/avaliacoes/" + id + "/publicar").with(user(professor(1))).with(csrf()))
                .andExpect(status().isConflict());
        assertEquals("PUBLICADA", jdbc.queryForObject("SELECT status FROM avaliacoes WHERE id = ?", String.class, id));
    }

    @Test
    void permiteSalvarRascunhoVazioMasNaoPublicar() throws Exception {
        long id = criar(body("Rascunho inicial", "[]", "[]"));
        mvc.perform(post("/api/avaliacoes/" + id + "/publicar").with(user(professor(1))).with(csrf()))
                .andExpect(status().isBadRequest());
        assertEquals("RASCUNHO", jdbc.queryForObject("SELECT status FROM avaliacoes WHERE id = ?", String.class, id));
    }

    @Test
    void rejeitaReferenciasInexistentesOuProfessorComoAlunoSemGravar() throws Exception {
        for (String request : new String[]{body("Invalida", "[{\"questaoId\":999999,\"valor\":1}]", "[3]"),
                body("Invalida", "[{\"questaoId\":1,\"valor\":1}]", "[1]"),
                body("Invalida", "[{\"questaoId\":1,\"valor\":1}]", "[999999]")}) {
            mvc.perform(post("/api/avaliacoes").with(user(professor(1))).with(csrf())
                    .contentType("application/json").content(request)).andExpect(status().isBadRequest());
        }
        assertEquals(5, quantidade("avaliacoes"));
    }

    @Test
    void rejeitaDuplicidadeEValorInvalido() throws Exception {
        for (String request : new String[]{body("Invalida", "[{\"questaoId\":1,\"valor\":1},{\"questaoId\":1,\"valor\":2}]", "[3]"),
                body("Invalida", "[{\"questaoId\":1,\"valor\":1}]", "[3,3]"),
                body("Invalida", "[{\"questaoId\":1,\"valor\":0}]", "[3]")}) {
            mvc.perform(post("/api/avaliacoes").with(user(professor(1))).with(csrf())
                    .contentType("application/json").content(request)).andExpect(status().isBadRequest());
        }
        assertEquals(5, quantidade("avaliacoes"));
    }

    @Test
    void validaGabaritoNovamenteNaPublicacao() throws Exception {
        long id = criar(completa());
        jdbc.update("UPDATE alternativas SET correta = FALSE WHERE questao_id = 1");
        mvc.perform(post("/api/avaliacoes/" + id + "/publicar").with(user(professor(1))).with(csrf()))
                .andExpect(status().isBadRequest());
        assertEquals("RASCUNHO", jdbc.queryForObject("SELECT status FROM avaliacoes WHERE id = ?", String.class, id));
    }

    @Test
    void restringeAvaliacaoAoProfessorResponsavel() throws Exception {
        long id = criar(completa());
        mvc.perform(get("/api/avaliacoes/" + id).with(user(professor(2)))).andExpect(status().isNotFound());
        mvc.perform(put("/api/avaliacoes/" + id).with(user(professor(2))).with(csrf())
                .contentType("application/json").content(completa())).andExpect(status().isNotFound());
        mvc.perform(post("/api/avaliacoes/" + id + "/publicar").with(user(professor(2))).with(csrf()))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/avaliacoes").with(user(professor(2))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void listaSomenteAlunosEImpedeAcessoDeAlunoOuAnonimo() throws Exception {
        mvc.perform(get("/api/alunos").with(user(professor(1))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(20))
                .andExpect(jsonPath("$[0].senhaHash").doesNotExist());
        var aluno = new UsuarioPrincipal(new CredenciaisUsuario(3, "Ana", "aluno01@example.test", null, "ALUNO"));
        mvc.perform(get("/api/avaliacoes").with(user(aluno))).andExpect(status().isForbidden());
        mvc.perform(get("/api/alunos").with(user(aluno))).andExpect(status().isForbidden());
        mvc.perform(post("/api/avaliacoes").with(user(aluno)).with(csrf())
                .contentType("application/json").content(completa())).andExpect(status().isForbidden());
        mvc.perform(get("/api/avaliacoes")).andExpect(status().isUnauthorized());
    }

    @Test
    void resumoNaoMultiplicaPontuacaoPeloNumeroDeParticipantes() throws Exception {
        mvc.perform(get("/api/avaliacoes").with(user(professor(1))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].quantidadeQuestoes").value(10))
                .andExpect(jsonPath("$[0].quantidadeAlunos").value(20))
                .andExpect(jsonPath("$[0].valorTotal").value(10));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void reverteAvaliacaoQuestoesEParticipantesSeUmaGravacaoFalhar() throws Exception {
        jdbc.execute("""
                CREATE FUNCTION falhar_participante_teste() RETURNS TRIGGER LANGUAGE plpgsql AS $$
                BEGIN
                    IF NEW.aluno_id = 4 THEN RAISE EXCEPTION 'Falha controlada no participante'; END IF;
                    RETURN NEW;
                END;
                $$;
                CREATE TRIGGER falhar_participante_teste BEFORE INSERT ON participacoes
                FOR EACH ROW EXECUTE FUNCTION falhar_participante_teste();
                """);
        try {
            mvc.perform(post("/api/avaliacoes").with(user(professor(1))).with(csrf())
                    .contentType("application/json").content(completa())).andExpect(status().isInternalServerError());
            assertEquals(5, quantidade("avaliacoes"));
            assertEquals(50, quantidade("avaliacao_questoes"));
            assertEquals(100, quantidade("participacoes"));
        } finally {
            jdbc.execute("DROP TRIGGER falhar_participante_teste ON participacoes; DROP FUNCTION falhar_participante_teste()");
        }
    }

    private long criar(String body) throws Exception {
        String response = mvc.perform(post("/api/avaliacoes").with(user(professor(1))).with(csrf())
                .contentType("application/json").content(body)).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(response, "$.id")).longValue();
    }

    private String completa() {
        return body("Prova de SQL", "[{\"questaoId\":1,\"valor\":2},{\"questaoId\":21,\"valor\":8}]", "[3,4]");
    }

    private String body(String titulo, String questoes, String alunos) {
        return """
                {"titulo":"%s","descricao":"Orientacoes da prova.","inicioEm":"2030-01-01T12:00:00Z",
                 "fimEm":"2030-01-01T14:00:00Z","questoes":%s,"alunos":%s}
                """.formatted(titulo, questoes, alunos);
    }

    private UsuarioPrincipal professor(long id) {
        return new UsuarioPrincipal(new CredenciaisUsuario(id, "Professor", "professor@example.test", null, "PROFESSOR"));
    }

    private int quantidade(String tabela) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + tabela, Integer.class);
    }
}
