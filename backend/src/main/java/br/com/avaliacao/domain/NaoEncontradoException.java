package br.com.avaliacao.domain;

public class NaoEncontradoException extends RuntimeException {
    public NaoEncontradoException(String mensagem) { super(mensagem); }
}
