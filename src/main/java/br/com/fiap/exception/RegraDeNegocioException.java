package br.com.fiap.exception;

//--- lançada quando uma regra de negócio do contrato (RN01, RN03...) é violada: vira 400 com a mensagem da regra
public class RegraDeNegocioException extends RuntimeException {

    public RegraDeNegocioException(String mensagem) {
        super(mensagem);
    }

}
