package br.com.avaliacao.api.questao;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@EnabledIfEnvironmentVariable(named = "TEST_DATABASE_URL", matches = ".+")
class QuestaoIntegrationTest {
    private static final String SCHEMA = "teste_" + UUID.randomUUID().toString().replace("-", "");
    private static final String URL = System.getenv("TEST_DATABASE_URL");
    private static final String USER = System.getenv().getOrDefault("TEST_DATABASE_USER", "avaliacao");
    private static final String PASSWORD = System.getenv().getOrDefault("TEST_DATABASE_PASSWORD", "");

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

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
    void cargaAtendeVolumeEPendencias() {
        assertEquals(22, quantidade("usuarios"));
        assertEquals(30, quantidade("questoes"));
        assertEquals(5, quantidade("avaliacoes"));
        assertEquals(100, quantidade("participacoes"));
        assertEquals(608, quantidade("respostas"));
        assertEquals(11L, jdbc.queryForObject("SELECT COUNT(*) FROM respostas WHERE nota_obtida IS NULL", Long.class));
        assertEquals(0L, jdbc.queryForObject("""
                SELECT COUNT(*) FROM respostas r
                JOIN alternativas a ON a.id = r.alternativa_id
                JOIN avaliacao_questoes aq ON aq.id = r.avaliacao_questao_id
                WHERE r.nota_obtida <> CASE WHEN a.correta THEN aq.valor ELSE 0 END
                """, Long.class));
    }

    @Test
    void cadastraObjetivaComAlternativasOrdenadas() throws Exception {
        mvc.perform(post("/api/questoes").contentType("application/json").content("""
                {"autorId":1,"tipo":"MULTIPLA_ESCOLHA","enunciado":"Questao criada pela API",
                 "alternativas":[{"texto":"CREATE TABLE","correta":true},{"texto":"DELETE","correta":false}]}
                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.alternativas.length()").value(2))
                .andExpect(jsonPath("$.alternativas[0].texto").value("CREATE TABLE"));
        assertEquals(31, quantidade("questoes"));
        assertEquals(82, quantidade("alternativas"));
    }

    @Test
    void cadastraTextoLivreSemAlternativas() throws Exception {
        mvc.perform(post("/api/questoes").contentType("application/json").content("""
                {"autorId":2,"tipo":"TEXTO_LIVRE","enunciado":"Explique uma transacao."}
                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.alternativas").isEmpty());
    }

    @Test
    void paginaQuestoesSemCortarAlternativas() throws Exception {
        mvc.perform(get("/api/questoes").param("limite", "2").param("deslocamento", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(2))
                .andExpect(jsonPath("$[0].alternativas.length()").value(4))
                .andExpect(jsonPath("$[1].id").value(3));
    }

    @Test
    void filtraPorTipo() throws Exception {
        mvc.perform(get("/api/questoes").param("tipo", "TEXTO_LIVRE").param("limite", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].id").value(21))
                .andExpect(jsonPath("$[0].alternativas").isEmpty());
    }

    @Test
    void rejeitaGabaritoAmbiguoSemGravarQuestao() throws Exception {
        mvc.perform(post("/api/questoes").contentType("application/json").content("""
                {"autorId":1,"tipo":"MULTIPLA_ESCOLHA","enunciado":"Invalida",
                 "alternativas":[{"texto":"A","correta":true},{"texto":"B","correta":true}]}
                """))
                .andExpect(status().isBadRequest());
        assertEquals(30, quantidade("questoes"));
    }

    @Test
    void rejeitaAlunoComoAutor() throws Exception {
        mvc.perform(post("/api/questoes").contentType("application/json").content("""
                {"autorId":3,"tipo":"TEXTO_LIVRE","enunciado":"Nao pode"}
                """))
                .andExpect(status().isBadRequest());
        assertEquals(30, quantidade("questoes"));
    }

    @Test
    void rejeitaEntradaMalformada() throws Exception {
        mvc.perform(post("/api/questoes").contentType("application/json").content("""
                {"autorId":1,"tipo":"INEXISTENTE","enunciado":"Invalida"}
                """))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/questoes").contentType("application/json").content("""
                {"autorId":1,"tipo":"MULTIPLA_ESCOLHA","enunciado":"Invalida","alternativas":[null]}
                """))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/questoes").param("limite", "101"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void desfazQuestaoEAlternativasSeUmaGravacaoFalhar() throws Exception {
        jdbc.execute("""
                CREATE FUNCTION falhar_alternativa_teste() RETURNS TRIGGER LANGUAGE plpgsql AS $$
                BEGIN
                    IF NEW.texto = 'forcar_erro_integracao' THEN
                        RAISE EXCEPTION 'Falha de integracao controlada';
                    END IF;
                    RETURN NEW;
                END;
                $$;
                CREATE TRIGGER falhar_alternativa_teste BEFORE INSERT ON alternativas
                FOR EACH ROW EXECUTE FUNCTION falhar_alternativa_teste();
                """);
        try {
            mvc.perform(post("/api/questoes").contentType("application/json").content("""
                    {"autorId":1,"tipo":"MULTIPLA_ESCOLHA","enunciado":"Deve reverter tudo",
                     "alternativas":[{"texto":"Primeira","correta":true},
                                     {"texto":"forcar_erro_integracao","correta":false}]}
                    """))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.detail").value("Não foi possível concluir a operação."));
            assertEquals(30, quantidade("questoes"));
            assertEquals(80, quantidade("alternativas"));
        } finally {
            jdbc.execute("DROP TRIGGER falhar_alternativa_teste ON alternativas; DROP FUNCTION falhar_alternativa_teste()");
        }
    }

    private int quantidade(String tabela) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + tabela, Integer.class);
    }
}
