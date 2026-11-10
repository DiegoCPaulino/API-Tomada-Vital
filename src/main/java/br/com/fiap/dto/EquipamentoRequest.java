package br.com.fiap.dto;

import br.com.fiap.entity.Equipamento;
import br.com.fiap.enums.TipoEquipamento;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

//--- corpo do cadastro e da alteração do equipamento. O paciente vem pelo id (não o objeto inteiro), como no professor
public record EquipamentoRequest(
        @Schema(description = "Identificador do paciente que usa o aparelho. Na alteração não pode mudar", example = "10")
        @NotNull(message = "O paciente deve ser informado") Long pacienteId,

        @Schema(description = "Tipo do aparelho elétrico ligado na tomada", example = "OUTRO")
        @NotNull(message = "O tipo do equipamento deve ser informado") TipoEquipamento tipo,

        @Schema(description = "Minutos que o aparelho funciona com a bateria cheia. 0 = aparelho sem bateria; máximo 1440 (24 h)", example = "120")
        @NotNull(message = "A autonomia deve ser informada")
        @Min(value = 0, message = "A autonomia deve estar entre 0 e 1440 minutos")
        @Max(value = 1440, message = "A autonomia deve estar entre 0 e 1440 minutos") Integer autonomiaMinutos,

        @Schema(description = "Código do dispositivo Tomada Vital ligado na tomada do aparelho (formato TV-000). Único", example = "TV-012")
        @NotBlank(message = "O código do dispositivo deve ser informado")
        @Pattern(regexp = "TV-\\d{3}", message = "O código tem o formato TV-000") String codigoDispositivo) {

    //--- o paciente não entra aqui: o EquipamentoService busca o paciente pelo id e confere o dono
    public Equipamento toEntity() {
        Equipamento equipamento = new Equipamento();
        equipamento.setTipo(tipo);
        equipamento.setAutonomiaMinutos(autonomiaMinutos);
        equipamento.setCodigoDispositivo(codigoDispositivo);
        return equipamento;
    }

}
