package br.com.fiap.repository;

import br.com.fiap.entity.Atendimento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

//--- herda do JpaRepository os métodos prontos: save, findById, findAll, count, deleteById...
@Repository
public interface AtendimentoRepository extends JpaRepository<Atendimento, Long> {

    //--- a linha do tempo de uma ocorrência (tela do app): o Spring gera o WHERE e o ORDER BY pelo nome do método
    List<Atendimento> findByOcorrenciaIdOrderByDataHoraAsc(Long ocorrenciaId);

}
