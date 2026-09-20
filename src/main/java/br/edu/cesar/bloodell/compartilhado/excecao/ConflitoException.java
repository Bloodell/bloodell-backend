package br.edu.cesar.bloodell.compartilhado.excecao;

public class ConflitoException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ConflitoException(String mensagem) {
        super(mensagem);
    }
}
