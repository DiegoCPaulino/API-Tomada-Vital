package br.com.fiap.dto;

import br.com.fiap.entity.Atendimento;
import br.com.fiap.enums.AcaoAtendimento;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

//--- corpo de um passo do atendimento registrado pela central (operador ou admin)
public record AtendimentoRequest(
        @Schema(description = "O que a central fez. ENERGIA_RESTABELECIDA não é aceita aqui: entra pelo resolver", example = "EQUIPE_A_CAMINHO")
        @NotNull(message = "A ação deve ser informada") AcaoAtendimento acao,

        @Schema(description = "Detalhe do passo (opcional, até 200 caracteres)", example = "Previsão de chegada em 30 minutos")
        @Size(max = 200, message = "A observação tem no máximo 200 caracteres") String observacao) {

    //--- a data e hora (agora) e a ocorrência são preenchidas pelo OcorrenciaService
    public Atendimento toEntity() {
        Atendimento atendimento = new Atendimento();
        atendimento.setAcao(acao);
        atendimento.setObservacao(observacao);
        return atendimento;
    }

}
