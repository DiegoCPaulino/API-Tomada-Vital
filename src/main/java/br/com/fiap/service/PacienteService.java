package br.com.fiap.service;

import br.com.fiap.dto.PacienteRequest;
import br.com.fiap.dto.PacienteResponse;
import br.com.fiap.entity.Paciente;
import br.com.fiap.entity.Usuario;
import br.com.fiap.exception.RecursoNaoEncontradoException;
import br.com.fiap.exception.RegraDeNegocioException;
import br.com.fiap.repository.EquipamentoRepository;
import br.com.fiap.repository.PacienteRepository;
import br.com.fiap.repository.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;

@Service
public class PacienteService {

    private final PacienteRepository pacienteRepository;
    private final EquipamentoRepository equipamentoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ControleDeAcesso controleDeAcesso;

    public PacienteService(PacienteRepository pacienteRepository, EquipamentoRepository equipamentoRepository,
                           UsuarioRepository usuarioRepository, ControleDeAcesso controleDeAcesso) {
        this.pacienteRepository = pacienteRepository;
        this.equipamentoRepository = equipamentoRepository;
        this.usuarioRepository = usuarioRepository;
        this.controleDeAcesso = controleDeAcesso;
    }

    //--- consulta com paginação e filtro por nome (como o professor, com Objects.isNull).
    //--- RN15: a família recebe só os seus pacientes. O filtro vai na consulta (WHERE), não depois:
    //--- assim a paginação e o total (page.totalElements) já saem certos
    public Page<PacienteResponse> consultar(String nome, Pageable paginacao, Authentication authentication) {
        Page<Paciente> pacientes;

        if (controleDeAcesso.ehFamilia(authentication)) {
            String login = authentication.getName();
            if (Objects.isNull(nome)) {
                pacientes = pacienteRepository.findByUsuarioLogin(login, paginacao);
            } else {
                pacientes = pacienteRepository.findByUsuarioLoginAndNomeContainsIgnoringCase(login, nome, paginacao);
            }
        } else {
            //--- operador e admin veem todos os pacientes (a central atende qualquer família)
            if (Objects.isNull(nome)) {
                pacientes = pacienteRepository.findAll(paginacao);
            } else {
                pacientes = pacienteRepository.findByNomeContainsIgnoringCase(nome, paginacao);
            }
        }

        return pacientes.map(PacienteResponse::from);
    }

    public PacienteResponse consultarPorCodigo(Long id) {
        Optional<Paciente> retornoConsulta = pacienteRepository.findById(id);
        if (retornoConsulta.isPresent()) {
            return PacienteResponse.from(retornoConsulta.get());
        }
        throw new RecursoNaoEncontradoException("Paciente", id);
    }

    //--- o paciente nasce ligado à família logada: o dono vem do token, nunca do corpo da requisição
    public PacienteResponse cadastrar(PacienteRequest pacienteRequest, Authentication authentication) {
        String login = authentication.getName();
        Usuario dono = usuarioRepository.findByLogin(login)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário " + login + " não encontrado."));

        Paciente novoPaciente = pacienteRequest.toEntity();
        novoPaciente.setUsuario(dono);

        Paciente pacienteCadastrado = pacienteRepository.save(novoPaciente);

        return PacienteResponse.from(pacienteCadastrado);
    }

    //--- o dono não muda na alteração: só os dados do paciente
    public PacienteResponse atualizar(Long id, PacienteRequest pacienteRequest) {
        Optional<Paciente> retornoConsulta = pacienteRepository.findById(id);
        if (retornoConsulta.isEmpty()) {
            throw new RecursoNaoEncontradoException("Paciente", id);
        }

        Paciente pacienteCadastrado = retornoConsulta.get();
        pacienteCadastrado.setNome(pacienteRequest.nome());
        pacienteCadastrado.setBairro(pacienteRequest.bairro());
        pacienteCadastrado.setCidade(pacienteRequest.cidade());
        pacienteCadastrado.setTelefone(pacienteRequest.telefone());

        Paciente pacienteAtualizado = pacienteRepository.save(pacienteCadastrado);

        return PacienteResponse.from(pacienteAtualizado);
    }

    public void remover(Long id) {
        if (!pacienteRepository.existsById(id)) {
            throw new RecursoNaoEncontradoException("Paciente", id);
        }

        //--- RN05: sem cascade no paciente, então quem tem equipamento não pode sumir (o equipamento ficaria sem dono)
        if (equipamentoRepository.existsByPacienteId(id)) {
            throw new RegraDeNegocioException("Remova os equipamentos do paciente antes de apagá-lo");
        }

        pacienteRepository.deleteById(id);
    }

}
