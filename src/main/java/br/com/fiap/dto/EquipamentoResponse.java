package br.com.fiap.dto;

import br.com.fiap.entity.Equipamento;
import br.com.fiap.enums.TipoEquipamento;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.hateoas.server.core.Relation;

import java.time.LocalDate;

//--- @Relation: nome do item e da lista no HAL ("_embedded": {"equipamentos": [...]})
@Relation(collectionRelation = "equipamentos", itemRelation = "equipamento")
public record EquipamentoResponse(
        @Schema(description = "Identificador do equipamento")
        Long id,

        @Schema(description = "Identificador do paciente que usa o aparelho")
        Long pacienteId,

        @Schema(description = "Identificador do usuário (família) dono do paciente")
        Long usuarioId,

        @Schema(description = "Tipo do aparelho elétrico")
        TipoEquipamento tipo,

        @Schema(description = "Minutos que o aparelho funciona com a bateria cheia. 0 = aparelho sem bateria")
        Integer autonomiaMinutos,

        @Schema(description = "Código do dispositivo Tomada Vital (formato TV-000)")
        String codigoDispositivo,

        @Schema(description = "Data em que o equipamento foi cadastrado")
        LocalDate cadastradoEm,

        @Schema(description = "Data da última alteração do equipamento (nulo se nunca foi alterado)")
        LocalDate atualizadoEm) {

    //--- o usuarioId vem do paciente: é o dono que o ControleDeAcesso confere
    public static EquipamentoResponse from(Equipamento equipamento) {
        return new EquipamentoResponse(
                equipamento.getId(),
                equipamento.getPaciente().getId(),
                equipamento.getPaciente().getUsuario().getId(),
                equipamento.getTipo(),
                equipamento.getAutonomiaMinutos(),
                equipamento.getCodigoDispositivo(),
                equipamento.getCadastradoEm(),
                equipamento.getAtualizadoEm()
        );
    }

}
