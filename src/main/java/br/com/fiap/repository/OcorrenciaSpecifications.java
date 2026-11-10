package br.com.fiap.repository;

import br.com.fiap.entity.Equipamento;
import br.com.fiap.entity.Ocorrencia;
import br.com.fiap.entity.Paciente;
import br.com.fiap.entity.Usuario;
import br.com.fiap.enums.StatusOcorrencia;
import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

//--- Filtro dinâmico das ocorrências (requisito do enunciado: Specification).
//--- Por que Specification aqui: o operador combina os filtros livremente (status, bairro, minutos, ou qualquer mistura).
//--- Com query method seria preciso um método para cada combinação (findByStatus, findByStatusAndBairro, ...).
//--- Cada método abaixo é UM pedaço do WHERE; o OcorrenciaService junta só os que foram informados
public class OcorrenciaSpecifications {

    //--- WHERE o.status = :status
    public static Specification<Ocorrencia> comStatus(StatusOcorrencia status) {
        return (root, query, builder) -> builder.equal(root.get("status"), status);
    }

    //--- o bairro é do paciente: ocorrência -> equipamento -> paciente. Compara em minúsculas ("centro" acha "Centro")
    public static Specification<Ocorrencia> noBairro(String bairro) {
        return (root, query, builder) -> {
            Join<Ocorrencia, Equipamento> equipamento = root.join("equipamento");
            Join<Equipamento, Paciente> paciente = equipamento.join("paciente");
            return builder.equal(builder.lower(paciente.get("bairro")), bairro.toLowerCase());
        };
    }

    //--- só as abertas cuja bateria acaba em até X minutos a partir de agora.
    //--- Ex.: minutosMaximos=120 = as CRÍTICAS agora (as de aparelho sem bateria já estão com a previsão no passado)
    public static Specification<Ocorrencia> esgotaEmAte(Integer minutos, LocalDateTime agora) {
        return (root, query, builder) -> builder.and(
                builder.notEqual(root.get("status"), StatusOcorrencia.RESOLVIDA),
                builder.lessThanOrEqualTo(root.get("previsaoEsgotamento"), agora.plusMinutes(minutos)));
    }

    //--- RN15: só as ocorrências da família logada: ocorrência -> equipamento -> paciente -> usuário (login do token)
    public static Specification<Ocorrencia> doUsuario(String login) {
        return (root, query, builder) -> {
            Join<Ocorrencia, Equipamento> equipamento = root.join("equipamento");
            Join<Equipamento, Paciente> paciente = equipamento.join("paciente");
            Join<Paciente, Usuario> usuario = paciente.join("usuario");
            return builder.equal(usuario.get("login"), login);
        };
    }

}
