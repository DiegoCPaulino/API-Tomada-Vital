package br.com.fiap.service;

import br.com.fiap.dto.EquipamentoRequest;
import br.com.fiap.dto.EquipamentoResponse;
import br.com.fiap.entity.Equipamento;
import br.com.fiap.entity.Paciente;
import br.com.fiap.exception.RecursoNaoEncontradoException;
import br.com.fiap.exception.RegraDeNegocioException;
import br.com.fiap.repository.EquipamentoRepository;
import br.com.fiap.repository.OcorrenciaRepository;
import br.com.fiap.repository.PacienteRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;

@Service
public class EquipamentoService {

    private final EquipamentoRepository equipamentoRepository;
    private final PacienteRepository pacienteRepository;
    private final OcorrenciaRepository ocorrenciaRepository;
    private final ControleDeAcesso controleDeAcesso;

    public EquipamentoService(EquipamentoRepository equipamentoRepository, PacienteRepository pacienteRepository,
                              OcorrenciaRepository ocorrenciaRepository, ControleDeAcesso controleDeAcesso) {
        this.equipamentoRepository = equipamentoRepository;
        this.pacienteRepository = pacienteRepository;
        this.ocorrenciaRepository = ocorrenciaRepository;
        this.controleDeAcesso = controleDeAcesso;
    }

    //--- consulta com paginação e filtro opcional por paciente.
    //--- RN15: a família recebe só os seus equipamentos (o filtro vai na consulta, não depois)
    public Page<EquipamentoResponse> consultar(Long pacienteId, Pageable paginacao, Authentication authentication) {
        Page<Equipamento> equipamentos;

        if (controleDeAcesso.ehFamilia(authentication)) {
            if (Objects.isNull(pacienteId)) {
                equipamentos = equipamentoRepository.findByPacienteUsuarioLogin(authentication.getName(), paginacao);
            } else {
                //--- a família pediu um paciente específico: ele precisa existir (404) e ser dela (403)
                Paciente paciente = buscarPaciente(pacienteId);
                controleDeAcesso.garantirAcesso(authentication, paciente.getUsuario().getId());
                equipamentos = equipamentoRepository.findByPacienteId(pacienteId, paginacao);
            }
        } else {
            //--- operador e admin veem os equipamentos de todos
            if (Objects.isNull(pacienteId)) {
                equipamentos = equipamentoRepository.findAll(paginacao);
            } else {
                equipamentos = equipamentoRepository.findByPacienteId(pacienteId, paginacao);
            }
        }

        return equipamentos.map(EquipamentoResponse::from);
    }

    //--- @Cacheable: na 1ª consulta do id executa o método (SELECT) e guarda a resposta; nas próximas devolve a guardada
    //--- sem ir ao banco. Guarda o DTO (nunca a entidade: fora da transação, o LAZY daria LazyInitializationException).
    //--- Não recebe Authentication: a chave é só o id e a conferência do dono fica no controller, DEPOIS do cache
    //--- (se ficasse aqui dentro, uma resposta vinda do cache pularia a conferência)
    @Cacheable(value = "equipamentos", key = "#id")
    public EquipamentoResponse consultarPorCodigo(Long id) {
        Optional<Equipamento> retornoConsulta = equipamentoRepository.findById(id);
        if (retornoConsulta.isPresent()) {
            return EquipamentoResponse.from(retornoConsulta.get());
        }
        throw new RecursoNaoEncontradoException("Equipamento", id);
    }

    public EquipamentoResponse cadastrar(EquipamentoRequest equipamentoRequest, Authentication authentication) {
        //--- o paciente precisa existir (404) e ser da família logada (403); o admin cadastra para qualquer paciente
        Paciente paciente = buscarPaciente(equipamentoRequest.pacienteId());
        controleDeAcesso.garantirAcesso(authentication, paciente.getUsuario().getId());

        //--- RN03: o código do dispositivo é único (é por ele que a queda chega na API)
        if (equipamentoRepository.existsByCodigoDispositivo(equipamentoRequest.codigoDispositivo())) {
            throw new RegraDeNegocioException("Já existe um equipamento com o código " + equipamentoRequest.codigoDispositivo());
        }

        Equipamento novoEquipamento = equipamentoRequest.toEntity();
        novoEquipamento.setPaciente(paciente);

        Equipamento equipamentoCadastrado = equipamentoRepository.save(novoEquipamento);

        return EquipamentoResponse.from(equipamentoCadastrado);
    }

    //--- @CacheEvict: depois de alterar, tira o id do cache; a próxima consulta busca os dados novos no banco
    @CacheEvict(value = "equipamentos", key = "#id")
    public EquipamentoResponse atualizar(Long id, EquipamentoRequest equipamentoRequest) {
        Optional<Equipamento> retornoConsulta = equipamentoRepository.findById(id);
        if (retornoConsulta.isEmpty()) {
            throw new RecursoNaoEncontradoException("Equipamento", id);
        }

        Equipamento equipamentoCadastrado = retornoConsulta.get();

        //--- RN04: o equipamento não muda de paciente (as quedas antigas ficariam no paciente errado).
        //--- Também impede a família de "passar" o aparelho para o paciente de outra família
        if (!equipamentoCadastrado.getPaciente().getId().equals(equipamentoRequest.pacienteId())) {
            throw new RegraDeNegocioException("O equipamento não pode ser transferido para outro paciente");
        }

        //--- RN03 na alteração: o código pode continuar o mesmo, mas não pode ser o de OUTRO equipamento
        if (equipamentoRepository.existsByCodigoDispositivoAndIdNot(equipamentoRequest.codigoDispositivo(), id)) {
            throw new RegraDeNegocioException("Já existe um equipamento com o código " + equipamentoRequest.codigoDispositivo());
        }

        equipamentoCadastrado.setTipo(equipamentoRequest.tipo());
        equipamentoCadastrado.setAutonomiaMinutos(equipamentoRequest.autonomiaMinutos());
        equipamentoCadastrado.setCodigoDispositivo(equipamentoRequest.codigoDispositivo());

        Equipamento equipamentoAtualizado = equipamentoRepository.save(equipamentoCadastrado);

        return EquipamentoResponse.from(equipamentoAtualizado);
    }

    //--- @CacheEvict: depois de apagar, tira o id do cache; senão a consulta continuaria achando o registro apagado
    @CacheEvict(value = "equipamentos", key = "#id")
    public void remover(Long id) {
        if (!equipamentoRepository.existsById(id)) {
            throw new RecursoNaoEncontradoException("Equipamento", id);
        }

        //--- RN06: as quedas são histórico; apagar o equipamento deixaria as ocorrências sem aparelho
        if (ocorrenciaRepository.existsByEquipamentoId(id)) {
            throw new RegraDeNegocioException("O equipamento tem histórico de quedas e não pode ser apagado");
        }

        equipamentoRepository.deleteById(id);
    }

    private Paciente buscarPaciente(Long pacienteId) {
        return pacienteRepository.findById(pacienteId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Paciente", pacienteId));
    }

}
