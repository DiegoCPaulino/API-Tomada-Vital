package br.com.fiap.repository;

import br.com.fiap.entity.Atendimento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

//--- herda do JpaRepository os métodos prontos: save, findById, findAll, count, deleteById...
@Repository
public interface AtendimentoRepository extends JpaRepository<Atendimento, Long> {
}
