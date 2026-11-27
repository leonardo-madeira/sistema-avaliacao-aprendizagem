package br.com.avaliacao.infrastructure.persistence;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Component
public class SqlRelatorios {
    private final Map<String, String> consultas = new HashMap<>();

    public SqlRelatorios() {
        try (var input = new ClassPathResource("sql/relatorios.sql").getInputStream()) {
            String texto = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            String[] blocos = texto.split("(?m)^-- relatorio: ");
            for (int i = 1; i < blocos.length; i++) {
                int quebra = blocos[i].indexOf('\n');
                consultas.put(blocos[i].substring(0, quebra).strip(), blocos[i].substring(quebra + 1).strip());
            }
            if (!consultas.keySet().containsAll(java.util.List.of("desempenho", "questoes", "participacao"))) {
                throw new IllegalStateException("Consultas dos relatórios não encontradas.");
            }
        } catch (IOException exception) { throw new UncheckedIOException(exception); }
    }

    public String obter(String nome) {
        String consulta = consultas.get(nome);
        if (consulta == null) throw new IllegalArgumentException("Relatório desconhecido.");
        return consulta;
    }
}
