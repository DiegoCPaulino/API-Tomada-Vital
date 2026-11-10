package br.com.fiap.dto;

import br.com.fiap.entity.Equipamento;
import br.com.fiap.entity.Ocorrencia;
import br.com.fiap.entity.Paciente;
import br.com.fiap.enums.Prioridade;
import br.com.fiap.enums.StatusOcorrencia;
import br.com.fiap.enums.TipoEquipamento;
import br.com.fiap.service.RegraDePrioridade;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.hateoas.server.core.Relation;

import java.time.LocalDateTime;

//--- @Relation: nome do item e da lista no HAL ("_embedded": {"ocorrencias": [...]})
@Relation(collectionRelation = "ocorrencias", itemRelation = "ocorrencia")
public record OcorrenciaResponse(
        @Schema(description = "Identificador da ocorrência")
        Long id,

        @Schema(description = "Identificador do equipamento que avisou a queda")
        Long equipamentoId,

        @Schema(description = "Código do dispositivo Tomada Vital (formato TV-000)")
        String codigoDispositivo,

        @Schema(description = "Tipo do aparelho elétrico")
        TipoEquipamento tipoEquipamento,

        @Schema(description = "Identificador do paciente")
        Long pacienteId,

        @Schema(description = "Nome do paciente")
        String nomePaciente,

        @Schema(description = "Bairro do paciente (para a equipe saber para onde ir)")
        String bairro,

        @Schema(description = "Cidade do paciente")
        String cidade,

        @Schema(description = "Identificador do usuário (família) dono do paciente")
        Long usuarioId,

        @Schema(description = "Quando a luz caiu")
        LocalDateTime inicio,

        @Schema(description = "Quando a energia voltou (nulo enquanto a ocorrência está aberta)")
        LocalDateTime fim,

        @Schema(description = "Bateria do aparelho no momento da queda, em porcentagem")
        Integer bateriaPercentual,

        @Schema(description = "Quanto a bateria aguentava na hora da queda: autonomia x bateria / 100")
        Integer minutosNoAlerta,

        @Schema(description = "Horário em que a bateria acaba (início + minutos no alerta). A fila é ordenada por ele")
        LocalDateTime previsaoEsgotamento,

        @Schema(description = "Minutos de bateria que ainda restam (calculado na resposta: agora, ou o fim se resolvida). Nunca menor que 0")
        Integer minutosRestantes,

        @Schema(description = "CRITICA (até 120 min ou aparelho sem bateria), ALTA (121 a 360) ou MODERADA (acima de 360). Calculada na resposta")
        Prioridade prioridade,

        @Schema(description = "ABERTA, EM_ATENDIMENTO ou RESOLVIDA")
        StatusOcorrencia status,

        @Schema(description = "Posição na fila de atendimento (1 = a primeira a ser atendida). Nulo se resolvida")
        Integer posicaoNaFila) {

    //--- minutosRestantes e prioridade NÃO são colunas: mudam a cada minuto, então são calculados aqui,
    //--- em toda resposta, pela RegraDePrioridade. O "agora" vem de fora (um só por requisição) para que
    //--- todas as ocorrências da mesma resposta sejam calculadas com o mesmo relógio
    public static OcorrenciaResponse from(Ocorrencia ocorrencia, LocalDateTime agora, Integer posicaoNaFila) {
        Equipamento equipamento = ocorrencia.getEquipamento();
        Paciente paciente = equipamento.getPaciente();

        //--- resolvida: os minutos ficam congelados no momento em que a energia voltou (fim); aberta: conta até agora
        LocalDateTime referencia = ocorrencia.getStatus() == StatusOcorrencia.RESOLVIDA ? ocorrencia.getFim() : agora;
        int minutosRestantes = RegraDePrioridade.calcularMinutosRestantes(ocorrencia.getPrevisaoEsgotamento(), referencia);
        Prioridade prioridade = RegraDePrioridade.classificar(equipamento.getAutonomiaMinutos(), minutosRestantes);

        return new OcorrenciaResponse(
                ocorrencia.getId(),
                equipamento.getId(),
                equipamento.getCodigoDispositivo(),
                equipamento.getTipo(),
                paciente.getId(),
                paciente.getNome(),
                paciente.getBairro(),
                paciente.getCidade(),
                paciente.getUsuario().getId(),
                ocorrencia.getInicio(),
                ocorrencia.getFim(),
                ocorrencia.getBateriaPercentual(),
                ocorrencia.getMinutosNoAlerta(),
                ocorrencia.getPrevisaoEsgotamento(),
                minutosRestantes,
                prioridade,
                ocorrencia.getStatus(),
                posicaoNaFila
        );
    }

}
