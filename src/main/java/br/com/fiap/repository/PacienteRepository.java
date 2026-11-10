package br.com.fiap.repository;

import br.com.fiap.entity.Paciente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

//--- herda do JpaRepository os métodos prontos: save, findById, findAll, count, deleteById...
@Repository
public interface PacienteRepository extends JpaRepository<Paciente, Long> {
}
