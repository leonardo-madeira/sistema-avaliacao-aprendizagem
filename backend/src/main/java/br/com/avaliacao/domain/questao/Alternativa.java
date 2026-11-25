package br.com.avaliacao.domain.questao;

public record Alternativa(Long id, String texto, boolean correta) {
    public Alternativa {
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException("A alternativa precisa de um texto.");
        }
        texto = texto.strip();
    }
}
