package br.com.fiap.dto;

import br.com.fiap.entity.Usuario;
import br.com.fiap.enums.Papel;
import io.swagger.v3.oas.annotations.media.Schema;

//--- resposta do cadastro. Nunca devolve a senha, nem criptografada
public record UsuarioResponse(
        @Schema(description = "Identificador do usuário")
        Long id,

        @Schema(description = "Nome de quem cuida do paciente (a família que usa o app)")
        String nome,

        @Schema(description = "Login do usuário")
        String login,

        @Schema(description = "Papel do usuário (o cadastro público cria sempre FAMILIA)")
        Papel papel) {

    public static UsuarioResponse from(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNome(),
                usuario.getLogin(),
                usuario.getPapel()
        );
    }

}
