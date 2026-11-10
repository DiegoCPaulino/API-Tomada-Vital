package br.com.fiap.exception;

import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

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

    //--- ordenação por um campo que não existe na entidade (ex.: ?sort=prioridade): o Spring Data não acha a coluna.
    //--- Sem este tratamento a resposta seria 500, mas o erro é de quem fez a requisição: 400
    @ExceptionHandler(PropertyReferenceException.class)
    public ProblemDetail tratar(PropertyReferenceException e) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problemDetail.setTitle("Requisição inválida");
        problemDetail.setDetail("Não é possível ordenar pelo campo " + e.getPropertyName() + ".");
        return problemDetail;
    }

    //--- valor de tipo errado na URL (ex.: ?status=XYZ ou /api/pacientes/abc): sem isto o 400 sairia fora do ProblemDetail
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail tratar(MethodArgumentTypeMismatchException e) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problemDetail.setTitle("Requisição inválida");
        problemDetail.setDetail("O valor " + e.getValue() + " não é válido para " + e.getName() + ".");
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
