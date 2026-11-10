package br.com.fiap.repository;

import br.com.fiap.entity.Paciente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

//--- herda do JpaRepository os métodos prontos: save, findById, findAll, count, deleteById...
@Repository
public interface PacienteRepository extends JpaRepository<Paciente, Long> {

    //--- busca por parte do nome, sem diferenciar maiúsculas (operador e admin): WHERE upper(nome) LIKE upper('%maria%')
    Page<Paciente> findByNomeContainsIgnoringCase(String nome, Pageable paginacao);

    //--- só os pacientes da família logada: o Spring navega paciente -> usuario -> login (faz o JOIN sozinho)
    Page<Paciente> findByUsuarioLogin(String login, Pageable paginacao);

    //--- os dois filtros juntos: os pacientes da família logada cujo nome contém o texto
    Page<Paciente> findByUsuarioLoginAndNomeContainsIgnoringCase(String login, String nome, Pageable paginacao);

}
