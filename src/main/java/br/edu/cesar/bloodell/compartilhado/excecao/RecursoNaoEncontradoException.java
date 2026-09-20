package br.edu.cesar.bloodell.compartilhado.excecao;

public class RecursoNaoEncontradoException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }

    public static RecursoNaoEncontradoException de(String recurso, Object identificador) {
        return new RecursoNaoEncontradoException(recurso + " nao encontrado(a): " + identificador);
    }
}
