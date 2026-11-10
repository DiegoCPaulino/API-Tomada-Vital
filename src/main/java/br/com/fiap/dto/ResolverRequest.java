package br.com.fiap.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

//--- corpo OPCIONAL do resolver: só a observação do último atendimento (ENERGIA_RESTABELECIDA)
public record ResolverRequest(
        @Schema(description = "Observação do encerramento (opcional, até 200 caracteres). Sem ela fica \"Energia restabelecida\"",
                example = "Rede religada pela concessionária")
        @Size(max = 200, message = "A observação tem no máximo 200 caracteres") String observacao) {
}
