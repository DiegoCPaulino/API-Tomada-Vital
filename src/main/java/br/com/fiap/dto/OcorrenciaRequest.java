package br.com.fiap.dto;

import br.com.fiap.entity.Ocorrencia;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

//--- corpo do aviso de queda: é o mesmo que o dispositivo IoT manda pela rede celular quando a luz cai
//--- (o código do dispositivo identifica o aparelho; a bateria é medida na hora da queda)
public record OcorrenciaRequest(
        @Schema(description = "Código do dispositivo Tomada Vital que percebeu a queda (formato TV-000)", example = "TV-011")
        @NotBlank(message = "O código do dispositivo deve ser informado") String codigoDispositivo,

        @Schema(description = "Bateria do aparelho no momento da queda, em porcentagem (0 a 100)", example = "50")
        @NotNull(message = "A bateria deve ser informada")
        @Min(value = 0, message = "A bateria deve estar entre 0 e 100")
        @Max(value = 100, message = "A bateria deve estar entre 0 e 100") Integer bateriaPercentual) {

    //--- o resto (equipamento, início, minutos, previsão e status) é preenchido pelo OcorrenciaService com a regra de negócio
    public Ocorrencia toEntity() {
        Ocorrencia ocorrencia = new Ocorrencia();
        ocorrencia.setBateriaPercentual(bateriaPercentual);
        return ocorrencia;
    }

}
