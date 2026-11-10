package br.com.fiap.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

//--- Recebe as exceções lançadas pelos controllers e services e devolve o erro sempre no mesmo formato (ProblemDetail).
//--- O 401 (sem token) e o 403 de URL (papel errado) NÃO passam por aqui: o Spring Security responde antes de chegar no controller
@RestControllerAdvice
public class TratadorGlobalException {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail tratar(MethodArgumentNotValidException e) {
        //--- um campo -> mensagem para cada erro do Bean Validation, para o app mostrar o erro ao lado do campo
        Map<String, String> campos = new HashMap<>();

        e.getFieldErrors().forEach(erro -> {
            campos.put(erro.getField(), erro.getDefaultMessage());
        });

        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problemDetail.setTitle("Dados inválidos");
        problemDetail.setProperty("campos", campos);

        return problemDetail;
    }

    //--- JSON quebrado ou valor que não existe num enum (ex.: "tipo": "GELADEIRA"): o Spring nem consegue montar o DTO
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail tratar(HttpMessageNotReadableException e) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problemDetail.setTitle("Requisição inválida");
        problemDetail.setDetail("O corpo da requisição está malformado ou tem um valor inválido.");
        return problemDetail;
    }

    @ExceptionHandler(RegraDeNegocioException.class)
    public ProblemDetail tratar(RegraDeNegocioException e) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problemDetail.setTitle("Regra de negócio violada");
        problemDetail.setDetail(e.getMessage());
        return problemDetail;
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ProblemDetail tratar(RecursoNaoEncontradoException e) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problemDetail.setTitle("Recurso não encontrado");
        problemDetail.setDetail(e.getMessage());
        return problemDetail;
    }

    @ExceptionHandler(AcessoNegadoException.class)
    public ProblemDetail tratar(AcessoNegadoException e) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.FORBIDDEN);
        problemDetail.setTitle("Acesso negado");
        problemDetail.setDetail(e.getMessage());
        return problemDetail;
    }

}
