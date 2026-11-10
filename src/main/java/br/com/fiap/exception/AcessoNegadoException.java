package br.com.fiap.exception;

//--- lançada pelo ControleDeAcesso quando uma família tenta mexer no registro de outra família: vira 403
public class AcessoNegadoException extends RuntimeException {

    public AcessoNegadoException(String mensagem) {
        super(mensagem);
    }

}
