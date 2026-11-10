package br.com.fiap.dto;

import br.com.fiap.entity.Paciente;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.hateoas.server.core.Relation;

import java.time.LocalDate;

//--- @Relation: nome do item e da lista no HAL. Sem ela a lista sairia como "pacienteResponseList"
@Relation(collectionRelation = "pacientes", itemRelation = "paciente")
public record PacienteResponse(
        @Schema(description = "Identificador do paciente")
        Long id,

        @Schema(description = "Nome completo da pessoa que depende do aparelho elétrico")
        String nome,

        @Schema(description = "Bairro onde o paciente mora")
        String bairro,

        @Schema(description = "Cidade onde o paciente mora")
        String cidade,

        @Schema(description = "Telefone da família")
        String telefone,

        @Schema(description = "Identificador do usuário (família) dono do paciente")
        Long usuarioId,

        @Schema(description = "Data em que o paciente foi cadastrado")
        LocalDate cadastradoEm,

        @Schema(description = "Data da última alteração do paciente (nulo se nunca foi alterado)")
        LocalDate atualizadoEm) {

    //--- getUsuario().getId() não vai ao banco: o id do usuário já está na coluna usuario_id do paciente
    public static PacienteResponse from(Paciente paciente) {
        return new PacienteResponse(
                paciente.getId(),
                paciente.getNome(),
                paciente.getBairro(),
                paciente.getCidade(),
                paciente.getTelefone(),
                paciente.getUsuario().getId(),
                paciente.getCadastradoEm(),
                paciente.getAtualizadoEm()
        );
    }

}
