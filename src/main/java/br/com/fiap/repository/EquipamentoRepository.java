package br.com.fiap.repository;

import br.com.fiap.entity.Equipamento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

//--- herda do JpaRepository os métodos prontos: save, findById, findAll, count, deleteById...
@Repository
public interface EquipamentoRepository extends JpaRepository<Equipamento, Long> {

    //--- acha o aparelho pelo código do dispositivo que mandou o alerta (usado no registro da queda)
    Optional<Equipamento> findByCodigoDispositivo(String codigoDispositivo);

    //--- RN03 no cadastro: já existe equipamento com este código?
    boolean existsByCodigoDispositivo(String codigoDispositivo);

    //--- RN03 na alteração: existe OUTRO equipamento (id diferente) com este código? Ignora o próprio registro
    boolean existsByCodigoDispositivoAndIdNot(String codigoDispositivo, Long id);

    //--- os equipamentos de um paciente (filtro ?pacienteId= da listagem)
    Page<Equipamento> findByPacienteId(Long pacienteId, Pageable paginacao);

    //--- só os equipamentos da família logada: equipamento -> paciente -> usuario -> login (dois JOINs gerados pelo Spring)
    Page<Equipamento> findByPacienteUsuarioLogin(String login, Pageable paginacao);

    //--- RN05: o paciente tem algum equipamento? (se tiver, não pode ser apagado)
    boolean existsByPacienteId(Long pacienteId);

}
