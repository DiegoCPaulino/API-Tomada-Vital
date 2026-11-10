package br.com.fiap.repository;

import br.com.fiap.entity.Ocorrencia;
import br.com.fiap.enums.StatusOcorrencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

//--- herda do JpaRepository os métodos prontos: save, findById, findAll, count, deleteById...
@Repository
public interface OcorrenciaRepository extends JpaRepository<Ocorrencia, Long> {

    //--- RN06: o equipamento já teve alguma queda? (se teve, é histórico e não pode ser apagado)
    boolean existsByEquipamentoId(Long equipamentoId);

    //--- RN08: o equipamento já tem uma queda que ainda não foi resolvida (status diferente de RESOLVIDA)?
    boolean existsByEquipamentoIdAndStatusNot(Long equipamentoId, StatusOcorrencia status);

    //--- posição na fila: quantas ocorrências abertas acabam a bateria ANTES desta (a posição é esse número + 1)
    long countByStatusNotAndPrevisaoEsgotamentoLessThan(StatusOcorrencia status, LocalDateTime previsaoEsgotamento);

}
