package br.com.fiap.repository;

import br.com.fiap.entity.Ocorrencia;
import br.com.fiap.enums.StatusOcorrencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

//--- herda do JpaRepository os métodos prontos: save, findById, findAll, count, deleteById...
//--- JpaSpecificationExecutor acrescenta o findAll(Specification, Pageable), usado pelo filtro dinâmico (OcorrenciaSpecifications)
@Repository
public interface OcorrenciaRepository extends JpaRepository<Ocorrencia, Long>, JpaSpecificationExecutor<Ocorrencia> {

    //--- RN06: o equipamento já teve alguma queda? (se teve, é histórico e não pode ser apagado)
    boolean existsByEquipamentoId(Long equipamentoId);

    //--- RN08: o equipamento já tem uma queda que ainda não foi resolvida (status diferente de RESOLVIDA)?
    boolean existsByEquipamentoIdAndStatusNot(Long equipamentoId, StatusOcorrencia status);

    //--- posição na fila: quantas ocorrências abertas acabam a bateria ANTES desta (a posição é esse número + 1)
    long countByStatusNotAndPrevisaoEsgotamentoLessThan(StatusOcorrencia status, LocalDateTime previsaoEsgotamento);

    //--- A FILA DE ATENDIMENTO: a consulta principal do sistema, em JPQL (entidades e atributos, não tabelas).
    //--- Ordem: quem fica sem bateria primeiro vem primeiro (previsaoEsgotamento); no empate, quem caiu antes (inicio).
    //--- JOIN FETCH: traz o equipamento e o paciente NA MESMA consulta. Sem ele, como os dois são LAZY, cada item da
    //--- fila faria mais um select do equipamento e outro do paciente (o problema "N+1": 1 consulta + N consultas extras)
    @Query("SELECT o FROM Ocorrencia o JOIN FETCH o.equipamento e JOIN FETCH e.paciente p " +
            "WHERE o.status <> :resolvida ORDER BY o.previsaoEsgotamento ASC, o.inicio ASC")
    List<Ocorrencia> consultarFila(@Param("resolvida") StatusOcorrencia resolvida);

}
