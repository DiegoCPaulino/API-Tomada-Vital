package br.com.fiap.dto;

import br.com.fiap.entity.Usuario;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

//--- corpo do cadastro público. Não tem campo de papel: quem se cadastra é sempre FAMILIA (RN02)
public record UsuarioCadastroRequest(
        @Schema(description = "Nome do usuário", example = "Família de Ana Lima")
        @NotBlank(message = "O nome deve ser informado") String nome,

        @Schema(description = "Login usado para entrar no app", example = "ana.lima")
        @NotBlank(message = "O login deve ser informado")
        @Size(min = 4, max = 30, message = "O login deve ter entre 4 e 30 caracteres") String login,

        @Schema(description = "Senha (mínimo de 6 caracteres)", example = "123456")
        @NotBlank(message = "A senha deve ser informada")
        @Size(min = 6, message = "A senha deve ter no mínimo 6 caracteres") String senha) {

    //--- a senha e o papel não entram aqui: o UsuarioService criptografa a senha e define o papel
    public Usuario toEntity() {
        Usuario usuario = new Usuario();
        usuario.setNome(nome);
        usuario.setLogin(login);
        return usuario;
    }

}
