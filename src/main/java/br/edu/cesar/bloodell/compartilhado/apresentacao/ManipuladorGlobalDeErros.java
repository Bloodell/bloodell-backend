package br.edu.cesar.bloodell.compartilhado.apresentacao;

import br.edu.cesar.bloodell.compartilhado.excecao.CapacidadeExcedidaException;
import br.edu.cesar.bloodell.compartilhado.excecao.ConflitoException;
import br.edu.cesar.bloodell.compartilhado.excecao.RecursoNaoEncontradoException;
import br.edu.cesar.bloodell.compartilhado.excecao.RegraDeNegocioException;
import br.edu.cesar.bloodell.config.FiltroDeCorrelacao;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

@RestControllerAdvice
public class ManipuladorGlobalDeErros {

    private static final Logger LOG = LoggerFactory.getLogger(ManipuladorGlobalDeErros.class);

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ProblemaApi> tratarNaoEncontrado(RecursoNaoEncontradoException excecao,
                                                           HttpServletRequest requisicao) {
        return montar("recurso-nao-encontrado", "Recurso nao encontrado",
                HttpStatus.NOT_FOUND, excecao.getMessage(), requisicao, null);
    }

    @ExceptionHandler(RegraDeNegocioException.class)
    public ResponseEntity<ProblemaApi> tratarRegraDeNegocio(RegraDeNegocioException excecao,
                                                            HttpServletRequest requisicao) {
        return montar("regra-de-negocio", "Regra de negocio violada",
                HttpStatus.UNPROCESSABLE_ENTITY, excecao.getMessage(), requisicao, null);
    }

    @ExceptionHandler(ConflitoException.class)
    public ResponseEntity<ProblemaApi> tratarConflito(ConflitoException excecao,
                                                      HttpServletRequest requisicao) {
        return montar("conflito", "Conflito com o estado atual do recurso",
                HttpStatus.CONFLICT, excecao.getMessage(), requisicao, null);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ProblemaApi> tratarConcorrencia(OptimisticLockingFailureException excecao,
                                                          HttpServletRequest requisicao) {
        LOG.warn("Conflito de concorrencia em {}: {}", requisicao.getRequestURI(), excecao.getMessage());
        return montar("conflito-de-concorrencia", "Outro processo alterou este recurso primeiro",
                HttpStatus.CONFLICT,
                "O recurso foi alterado por outra operacao simultanea. Consulte o estado atual e tente novamente.",
                requisicao, null);
    }

    @ExceptionHandler(CapacidadeExcedidaException.class)
    public ResponseEntity<ProblemaApi> tratarCapacidade(CapacidadeExcedidaException excecao,
                                                        HttpServletRequest requisicao) {
        ProblemaApi problema = ProblemaApi.de("capacidade-excedida", "Servico temporariamente saturado",
                HttpStatus.SERVICE_UNAVAILABLE.value(), excecao.getMessage(),
                requisicao.getRequestURI(), FiltroDeCorrelacao.correlacaoAtual(), null);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .header("Retry-After", "2")
                .body(problema);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemaApi> tratarValidacao(MethodArgumentNotValidException excecao,
                                                       HttpServletRequest requisicao) {
        List<ProblemaApi.ErroDeCampo> campos = new ArrayList<>();
        for (FieldError erro : excecao.getBindingResult().getFieldErrors()) {
            campos.add(new ProblemaApi.ErroDeCampo(erro.getField(), erro.getDefaultMessage()));
        }
        return montar("entrada-invalida", "Entrada invalida", HttpStatus.BAD_REQUEST,
                "Um ou mais campos nao passaram na validacao.", requisicao, campos);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemaApi> tratarArgumentoInvalido(IllegalArgumentException excecao,
                                                               HttpServletRequest requisicao) {
        return montar("entrada-invalida", "Entrada invalida", HttpStatus.BAD_REQUEST,
                excecao.getMessage(), requisicao, null);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemaApi> tratarCorpoIlegivel(HttpMessageNotReadableException excecao,
                                                           HttpServletRequest requisicao) {
        return montar("corpo-ilegivel", "Corpo da requisicao ilegivel", HttpStatus.BAD_REQUEST,
                "Nao foi possivel interpretar o JSON enviado.", requisicao, null);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ProblemaApi> tratarRotaInexistente(NoHandlerFoundException excecao,
                                                             HttpServletRequest requisicao) {
        return montar("rota-inexistente", "Rota inexistente", HttpStatus.NOT_FOUND,
                "Nao existe recurso em " + requisicao.getRequestURI(), requisicao, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemaApi> tratarInesperado(Exception excecao, HttpServletRequest requisicao) {
        LOG.error("Erro inesperado em {}", requisicao.getRequestURI(), excecao);
        return montar("erro-interno", "Erro interno", HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocorreu um erro inesperado. Use o campo de correlacao para localizar o log.",
                requisicao, null);
    }

    private ResponseEntity<ProblemaApi> montar(String tipo, String titulo, HttpStatus status,
                                               String detalhe, HttpServletRequest requisicao,
                                               List<ProblemaApi.ErroDeCampo> campos) {
        ProblemaApi problema = ProblemaApi.de(tipo, titulo, status.value(), detalhe,
                requisicao.getRequestURI(), FiltroDeCorrelacao.correlacaoAtual(), campos);
        return ResponseEntity.status(status).body(problema);
    }
}
