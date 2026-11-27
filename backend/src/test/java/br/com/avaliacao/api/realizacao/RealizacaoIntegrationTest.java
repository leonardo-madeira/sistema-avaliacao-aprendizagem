package br.com.avaliacao.api.realizacao;

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
import org.junit.jupiter.api.BeforeEach;
import java.time.*;
import java.sql.Timestamp;
import java.math.BigDecimal;
import java.util.List;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@org.springframework.context.annotation.Import(RealizacaoIntegrationTest.TimeConfig.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@EnabledIfEnvironmentVariable(named = "TEST_DATABASE_URL", matches = ".+")
class RealizacaoIntegrationTest {
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

    private static final Instant AGORA = Instant.parse("2030-01-01T12:00:00Z");
    private long participacao;

    @org.springframework.boot.test.context.TestConfiguration
    static class TimeConfig {
        @org.springframework.context.annotation.Bean
        @org.springframework.context.annotation.Primary
        Clock testClock() { return Clock.fixed(AGORA, ZoneOffset.UTC); }
    }

    @BeforeEach
    void prepararJanela() {
        jdbc.update("UPDATE avaliacoes SET inicio_em = ?, fim_em = ? WHERE id = 5",
                Timestamp.from(AGORA.minusSeconds(60)), Timestamp.from(AGORA.plusSeconds(3600)));
        participacao = jdbc.queryForObject("SELECT id FROM participacoes WHERE avaliacao_id = 5 AND aluno_id = 3", Long.class);
    }

    @Test
    void listaSomenteAvaliacoesPublicadasEDestinadasAoAluno() throws Exception {
        mvc.perform(get("/api/minhas-avaliacoes").with(user(aluno(3))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$[0].situacao").value("DISPONIVEL"));
        jdbc.update("UPDATE avaliacoes SET status = 'RASCUNHO' WHERE id = 5");
        mvc.perform(get("/api/minhas-avaliacoes").with(user(aluno(3))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(4));
        mvc.perform(post(url("iniciar")).with(user(aluno(3))).with(csrf())).andExpect(status().isNotFound());
    }

    @Test
    void bloqueiaOutraContaEModosDeAcesso() throws Exception {
        mvc.perform(get("/api/minhas-avaliacoes/" + participacao).with(user(aluno(4)))).andExpect(status().isNotFound());
        mvc.perform(post(url("iniciar")).with(user(aluno(4))).with(csrf())).andExpect(status().isNotFound());
        mvc.perform(get("/api/minhas-avaliacoes").with(user(professor(1)))).andExpect(status().isForbidden());
        mvc.perform(get("/api/minhas-avaliacoes")).andExpect(status().isUnauthorized());
        mvc.perform(post(url("iniciar")).with(user(aluno(3)))).andExpect(status().isForbidden());
    }

    @Test
    void exigeInicioEJanelaDoServidor() throws Exception {
        mvc.perform(put(url("respostas")).with(user(aluno(3))).with(csrf()).contentType("application/json").content("{\"respostas\":[]}"))
                .andExpect(status().isConflict());
        jdbc.update("UPDATE avaliacoes SET inicio_em = ?, fim_em = ? WHERE id = 5",
                Timestamp.from(AGORA.plusSeconds(60)), Timestamp.from(AGORA.plusSeconds(3600)));
        mvc.perform(post(url("iniciar")).with(user(aluno(3))).with(csrf())).andExpect(status().isConflict());
        mvc.perform(get("/api/minhas-avaliacoes").with(user(aluno(3))))
                .andExpect(jsonPath("$[0].situacao").value("AGENDADA"));
        jdbc.update("UPDATE avaliacoes SET inicio_em = ?, fim_em = ? WHERE id = 5",
                Timestamp.from(AGORA.minusSeconds(3600)), Timestamp.from(AGORA));
        mvc.perform(post(url("iniciar")).with(user(aluno(3))).with(csrf())).andExpect(status().isConflict());
    }

    @Test
    void iniciaSalvaERecuperaSemExporNotasOuGabaritos() throws Exception {
        iniciar();
        salvar(completa());
        String json = mvc.perform(get("/api/minhas-avaliacoes/" + participacao).with(user(aluno(3))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.respostas.length()").value(2))
                .andExpect(jsonPath("$.avaliacao.notaTotal").isEmpty())
                .andReturn().getResponse().getContentAsString();
        for (String campo : List.of("correta", "notaObtida", "notaParcial")) {
            assertTrue(((List<?>) com.jayway.jsonpath.JsonPath.read(json, "$.." + campo)).isEmpty());
        }
        assertEquals(0, new BigDecimal("1").compareTo(jdbc.queryForObject(
                "SELECT nota_obtida FROM respostas WHERE participacao_id = ? AND avaliacao_questao_id = ?",
                BigDecimal.class, participacao, objetiva())));
        mvc.perform(post(url("iniciar")).with(user(aluno(3))).with(csrf())).andExpect(status().isNoContent());
        assertEquals(2, quantidadeRespostas());
    }

    @Test
    void limparRespostaRemoveValorAnterior() throws Exception {
        iniciar(); salvar(completa()); salvar("{\"respostas\":[]}");
        assertEquals(0, quantidadeRespostas());
    }

    @Test
    void rejeitaQuestaoOuAlternativaDeOutraProvaSemApagarRespostasSalvas() throws Exception {
        iniciar(); salvar(completa());
        long itemAlheio = jdbc.queryForObject("SELECT id FROM avaliacao_questoes WHERE avaliacao_id = 1 ORDER BY ordem LIMIT 1", Long.class);
        long alternativaAlheia = jdbc.queryForObject("SELECT id FROM alternativas WHERE questao_id = 1 ORDER BY ordem LIMIT 1", Long.class);
        for (String body : List.of(objetivaBody(itemAlheio, alternativa()), objetivaBody(objetiva(), alternativaAlheia),
                "{\"respostas\":[{\"avaliacaoQuestaoId\":" + objetiva() + ",\"texto\":\"Nao e texto\"}]}",
                "{\"respostas\":[{\"avaliacaoQuestaoId\":" + texto() + ",\"alternativaId\":" + alternativa() + "}]}")) {
            mvc.perform(put(url("respostas")).with(user(aluno(3))).with(csrf()).contentType("application/json").content(body))
                    .andExpect(status().isBadRequest());
        }
        assertEquals(2, quantidadeRespostas());
    }

    @Test
    void rejeitaDuplicidadeENotaEnviadaNaoAlteraCorrecaoAutomatica() throws Exception {
        iniciar();
        String duplicada = "{\"respostas\":[{\"avaliacaoQuestaoId\":%d,\"alternativaId\":%d},{\"avaliacaoQuestaoId\":%d,\"alternativaId\":%d}]}"
                .formatted(objetiva(), alternativa(), objetiva(), alternativa());
        mvc.perform(put(url("respostas")).with(user(aluno(3))).with(csrf()).contentType("application/json").content(duplicada))
                .andExpect(status().isBadRequest());
        salvar("{\"respostas\":[{\"avaliacaoQuestaoId\":%d,\"alternativaId\":%d,\"notaObtida\":999}]}".formatted(objetiva(), alternativa()));
        assertEquals(0, BigDecimal.ONE.compareTo(jdbc.queryForObject("SELECT nota_obtida FROM respostas WHERE participacao_id = ?", BigDecimal.class, participacao)));
    }

    @Test
    void entregaBloqueiaNovaTentativaEPermiteConsultarDepoisDoPrazo() throws Exception {
        iniciar(); entregar(completa());
        mvc.perform(post(url("iniciar")).with(user(aluno(3))).with(csrf())).andExpect(status().isConflict());
        mvc.perform(put(url("respostas")).with(user(aluno(3))).with(csrf()).contentType("application/json").content(completa()))
                .andExpect(status().isConflict());
        mvc.perform(post(url("entregar")).with(user(aluno(3))).with(csrf()).contentType("application/json").content(completa()))
                .andExpect(status().isConflict());
        jdbc.update("UPDATE avaliacoes SET inicio_em = ?, fim_em = ? WHERE id = 5", Timestamp.from(AGORA.minusSeconds(7200)), Timestamp.from(AGORA.minusSeconds(1)));
        mvc.perform(get("/api/minhas-avaliacoes/" + participacao).with(user(aluno(3))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.avaliacao.situacao").value("ENTREGUE"));
    }

    @Test
    void bloqueiaSalvamentoQuandoPrazoTermina() throws Exception {
        iniciar();
        jdbc.update("UPDATE avaliacoes SET fim_em = ? WHERE id = 5", Timestamp.from(AGORA));
        mvc.perform(put(url("respostas")).with(user(aluno(3))).with(csrf()).contentType("application/json").content(completa()))
                .andExpect(status().isConflict());
        assertEquals(0, quantidadeRespostas());
    }

    @Test
    void corrigeTextoESomaComNotaObjetivaIncluindoZero() throws Exception {
        iniciar(); entregar(completa());
        long resposta = respostaTexto();
        mvc.perform(get("/api/correcoes/" + participacao).with(user(professor(1))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.respostas.length()").value(10));
        atribuir(resposta, "0.75");
        mvc.perform(get("/api/minhas-avaliacoes").with(user(aluno(3))))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].notaTotal").value(1.75))
                .andExpect(jsonPath("$[0].correcoesPendentes").value(0));
        atribuir(resposta, "0");
        mvc.perform(get("/api/minhas-avaliacoes").with(user(aluno(3))))
                .andExpect(jsonPath("$[0].notaTotal").value(1));
    }

    @Test
    void naoCorrigeAntesDaEntregaNemPermiteOutroProfessorOuAluno() throws Exception {
        iniciar(); salvar(completa());
        mvc.perform(get("/api/correcoes/" + participacao).with(user(professor(1)))).andExpect(status().isConflict());
        entregar(completa());
        mvc.perform(get("/api/correcoes/" + participacao).with(user(professor(2)))).andExpect(status().isNotFound());
        mvc.perform(get("/api/avaliacoes/5/entregas").with(user(professor(2)))).andExpect(status().isNotFound());
        mvc.perform(get("/api/correcoes/" + participacao).with(user(aluno(3)))).andExpect(status().isForbidden());
        mvc.perform(get("/api/avaliacoes/5/entregas").with(user(professor(1))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(20));
    }

    @Test
    void rejeitaNotaAcimaDoValorENotaManualEmObjetiva() throws Exception {
        iniciar(); entregar(completa());
        long objetivaResposta = jdbc.queryForObject("SELECT id FROM respostas WHERE participacao_id = ? AND avaliacao_questao_id = ?", Long.class, participacao, objetiva());
        for (String body : List.of(notas(respostaTexto(), "2"), notas(objetivaResposta, "0.5"), notas(999999, "0.5"), notas(respostaTexto(), "0.001"))) {
            mvc.perform(put("/api/correcoes/" + participacao).with(user(professor(1))).with(csrf()).contentType("application/json").content(body))
                    .andExpect(status().isBadRequest());
        }
        assertNull(jdbc.queryForObject("SELECT nota_obtida FROM respostas WHERE id = ?", BigDecimal.class, respostaTexto()));
    }

    @Test
    void permiteEntregaEmBrancoComNotaZero() throws Exception {
        iniciar(); entregar("{\"respostas\":[]}");
        mvc.perform(get("/api/minhas-avaliacoes").with(user(aluno(3))))
                .andExpect(jsonPath("$[0].notaTotal").value(0));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void falhaNaEntregaRestauraRespostasAnterioresENaoMarcaEntregue() throws Exception {
        iniciar(); salvar(completa());
        jdbc.execute("""
                CREATE FUNCTION falhar_entrega_teste() RETURNS TRIGGER LANGUAGE plpgsql AS $$
                BEGIN
                    IF NEW.texto_resposta = 'falhar_entrega_teste' THEN RAISE EXCEPTION 'Falha controlada'; END IF;
                    RETURN NEW;
                END; $$;
                CREATE TRIGGER falhar_entrega_teste BEFORE INSERT ON respostas FOR EACH ROW EXECUTE FUNCTION falhar_entrega_teste();
                """);
        try {
            mvc.perform(post(url("entregar")).with(user(aluno(3))).with(csrf()).contentType("application/json")
                    .content(completa().replace("Resposta textual.", "falhar_entrega_teste"))).andExpect(status().isInternalServerError());
            assertEquals(2, quantidadeRespostas());
            assertEquals("Resposta textual.", jdbc.queryForObject("SELECT texto_resposta FROM respostas WHERE id = ?", String.class, respostaTexto()));
            assertNull(jdbc.queryForObject("SELECT entregue_em FROM participacoes WHERE id = ?", Timestamp.class, participacao));
        } finally {
            jdbc.execute("DROP TRIGGER falhar_entrega_teste ON respostas; DROP FUNCTION falhar_entrega_teste()"); limparParticipacao();
        }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void falhaNaSegundaNotaRevertePrimeiraNota() throws Exception {
        iniciar();
        List<Long> itens = jdbc.query("SELECT aq.id FROM avaliacao_questoes aq JOIN questoes q ON q.id = aq.questao_id WHERE aq.avaliacao_id = 5 AND q.tipo_questao_id = 2 ORDER BY aq.ordem", (rs,row) -> rs.getLong(1));
        entregar("{\"respostas\":[{\"avaliacaoQuestaoId\":%d,\"texto\":\"Primeira\"},{\"avaliacaoQuestaoId\":%d,\"texto\":\"Segunda\"}]}".formatted(itens.get(0), itens.get(1)));
        List<Long> ids = jdbc.query("SELECT id FROM respostas WHERE participacao_id = ? ORDER BY avaliacao_questao_id", (rs,row) -> rs.getLong(1), participacao);
        jdbc.execute("""
                CREATE FUNCTION falhar_nota_teste() RETURNS TRIGGER LANGUAGE plpgsql AS $$
                BEGIN
                    IF NEW.id = %d THEN RAISE EXCEPTION 'Falha controlada na nota'; END IF;
                    RETURN NEW;
                END; $$;
                CREATE TRIGGER falhar_nota_teste BEFORE UPDATE ON respostas FOR EACH ROW EXECUTE FUNCTION falhar_nota_teste();
                """.formatted(ids.get(1)));
        try {
            String body = "{\"notas\":[{\"respostaId\":%d,\"nota\":0.5},{\"respostaId\":%d,\"nota\":0.75}]}".formatted(ids.get(0),ids.get(1));
            mvc.perform(put("/api/correcoes/" + participacao).with(user(professor(1))).with(csrf()).contentType("application/json").content(body))
                    .andExpect(status().isInternalServerError());
            assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM respostas WHERE participacao_id = ? AND nota_obtida IS NOT NULL", Integer.class, participacao));
        } finally {
            jdbc.execute("DROP TRIGGER falhar_nota_teste ON respostas; DROP FUNCTION falhar_nota_teste()"); limparParticipacao();
        }
    }

    private void limparParticipacao() {
        jdbc.update("DELETE FROM respostas WHERE participacao_id = ?", participacao);
        jdbc.update("UPDATE participacoes SET iniciada_em = NULL, entregue_em = NULL WHERE id = ?", participacao);
    }
    private void iniciar() throws Exception { mvc.perform(post(url("iniciar")).with(user(aluno(3))).with(csrf())).andExpect(status().isNoContent()); }
    private void salvar(String body) throws Exception { mvc.perform(put(url("respostas")).with(user(aluno(3))).with(csrf()).contentType("application/json").content(body)).andExpect(status().isNoContent()); }
    private void entregar(String body) throws Exception { mvc.perform(post(url("entregar")).with(user(aluno(3))).with(csrf()).contentType("application/json").content(body)).andExpect(status().isNoContent()); }
    private void atribuir(long id, String nota) throws Exception { mvc.perform(put("/api/correcoes/" + participacao).with(user(professor(1))).with(csrf()).contentType("application/json").content(notas(id, nota))).andExpect(status().isNoContent()); }
    private String notas(long id, String nota) { return "{\"notas\":[{\"respostaId\":%d,\"nota\":%s}]}".formatted(id, nota); }
    private String url(String acao) { return "/api/minhas-avaliacoes/" + participacao + "/" + acao; }
    private long objetiva() { return item(1); }
    private long texto() { return item(2); }
    private long item(long tipo) { return jdbc.queryForObject("SELECT aq.id FROM avaliacao_questoes aq JOIN questoes q ON q.id = aq.questao_id WHERE aq.avaliacao_id = 5 AND q.tipo_questao_id = ? ORDER BY aq.ordem LIMIT 1", Long.class, tipo); }
    private long alternativa() { return jdbc.queryForObject("SELECT a.id FROM alternativas a JOIN avaliacao_questoes aq ON aq.questao_id = a.questao_id WHERE aq.id = ? AND a.correta", Long.class, objetiva()); }
    private long respostaTexto() { return jdbc.queryForObject("SELECT id FROM respostas WHERE participacao_id = ? AND avaliacao_questao_id = ?", Long.class, participacao, texto()); }
    private int quantidadeRespostas() { return jdbc.queryForObject("SELECT COUNT(*) FROM respostas WHERE participacao_id = ?", Integer.class, participacao); }
    private String completa() { return "{\"respostas\":[{\"avaliacaoQuestaoId\":%d,\"alternativaId\":%d},{\"avaliacaoQuestaoId\":%d,\"texto\":\"Resposta textual.\"}]}".formatted(objetiva(), alternativa(), texto()); }
    private String objetivaBody(long item, long alternativa) { return "{\"respostas\":[{\"avaliacaoQuestaoId\":%d,\"alternativaId\":%d}]}".formatted(item, alternativa); }
    private UsuarioPrincipal aluno(long id) { return new UsuarioPrincipal(new CredenciaisUsuario(id, "Aluno", "aluno@example.test", null, "ALUNO")); }
    private UsuarioPrincipal professor(long id) { return new UsuarioPrincipal(new CredenciaisUsuario(id, "Professor", "professor@example.test", null, "PROFESSOR")); }
}
