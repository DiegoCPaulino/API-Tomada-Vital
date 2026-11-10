package br.com.fiap.repository;

import br.com.fiap.entity.Ocorrencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

//--- herda do JpaRepository os métodos prontos: save, findById, findAll, count, deleteById...
@Repository
public interface OcorrenciaRepository extends JpaRepository<Ocorrencia, Long> {

    //--- RN06: o equipamento já teve alguma queda? (se teve, é histórico e não pode ser apagado)
    boolean existsByEquipamentoId(Long equipamentoId);

}
