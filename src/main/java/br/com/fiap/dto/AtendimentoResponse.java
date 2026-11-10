package br.com.fiap.dto;

import br.com.fiap.entity.Atendimento;
import br.com.fiap.enums.AcaoAtendimento;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.hateoas.server.core.Relation;

import java.time.LocalDateTime;

//--- @Relation: nome do item e da lista no HAL ("_embedded": {"atendimentos": [...]})
@Relation(collectionRelation = "atendimentos", itemRelation = "atendimento")
public record AtendimentoResponse(
        @Schema(description = "Identificador do atendimento")
        Long id,

        @Schema(description = "Identificador da ocorrência atendida")
        Long ocorrenciaId,

        @Schema(description = "Quando o passo foi registrado")
        LocalDateTime dataHora,

        @Schema(description = "O que a central fez")
        AcaoAtendimento acao,

        @Schema(description = "Detalhe do passo (pode ser nulo)")
        String observacao) {

    //--- getOcorrencia().getId() não vai ao banco: o id já está na coluna ocorrencia_id do atendimento
    public static AtendimentoResponse from(Atendimento atendimento) {
        return new AtendimentoResponse(
                atendimento.getId(),
                atendimento.getOcorrencia().getId(),
                atendimento.getDataHora(),
                atendimento.getAcao(),
                atendimento.getObservacao()
        );
    }

}
