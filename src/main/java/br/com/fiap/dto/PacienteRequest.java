package br.com.fiap.dto;

import br.com.fiap.entity.Paciente;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

//--- corpo do cadastro e da alteração do paciente. Não tem o dono: no cadastro, o dono é sempre a família logada
public record PacienteRequest(
        @Schema(description = "Nome completo da pessoa que depende do aparelho elétrico", example = "Maria Aparecida Souza")
        @NotBlank(message = "O nome deve ser informado") String nome,

        @Schema(description = "Bairro onde o paciente mora (usado pelo operador para mandar a equipe)", example = "Parque Pinheiros")
        @NotBlank(message = "O bairro deve ser informado") String bairro,

        @Schema(description = "Cidade onde o paciente mora", example = "Taboão da Serra")
        @NotBlank(message = "A cidade deve ser informada") String cidade,

        @Schema(description = "Telefone para a central falar com a família durante a queda de energia", example = "(11) 90000-0001")
        @NotBlank(message = "O telefone deve ser informado") String telefone) {

    //--- o usuário (dono) não entra aqui: o PacienteService liga o paciente à família logada
    public Paciente toEntity() {
        Paciente paciente = new Paciente();
        paciente.setNome(nome);
        paciente.setBairro(bairro);
        paciente.setCidade(cidade);
        paciente.setTelefone(telefone);
        return paciente;
    }

}
