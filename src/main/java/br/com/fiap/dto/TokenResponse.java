package br.com.fiap.dto;

import io.swagger.v3.oas.annotations.media.Schema;

//--- resposta do login: o app guarda o accessToken e manda em toda requisição no cabeçalho Authorization: Bearer <token>
public record TokenResponse(
        @Schema(description = "Token JWT assinado")
        String accessToken,

        @Schema(description = "Tipo do token", example = "Bearer")
        String tokenType,

        @Schema(description = "Validade do token em segundos", example = "3600")
        Integer expiresIn) {
}
