package br.com.fiap.exception;

//--- lançada quando o registro procurado não existe: o TratadorGlobalException transforma em 404
public class RecursoNaoEncontradoException extends RuntimeException {

    private static final String MSG = "%s [id=%d] não encontrado.";

    //--- busca pelo id (ex.: "Paciente [id=99] não encontrado."), como a AlunoNaoEncontradoException do professor
    public RecursoNaoEncontradoException(String recurso, Long id) {
        super(String.format(MSG, recurso, id));
    }

    //--- busca por outro campo, quando não há id (ex.: o código do dispositivo TV-999)
    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }

}
