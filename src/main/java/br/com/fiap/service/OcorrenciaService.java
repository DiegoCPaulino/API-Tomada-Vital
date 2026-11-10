package br.com.fiap.service;

import br.com.fiap.dto.OcorrenciaRequest;
import br.com.fiap.dto.OcorrenciaResponse;
import br.com.fiap.entity.Equipamento;
import br.com.fiap.entity.Ocorrencia;
import br.com.fiap.enums.StatusOcorrencia;
import br.com.fiap.exception.RecursoNaoEncontradoException;
import br.com.fiap.exception.RegraDeNegocioException;
import br.com.fiap.repository.EquipamentoRepository;
import br.com.fiap.repository.OcorrenciaRepository;
import br.com.fiap.repository.OcorrenciaSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

//--- Sem cache nas ocorrências (contrato §7): os minutos restantes mudam a cada minuto, e uma resposta
//--- guardada mostraria um tempo de bateria errado. Num sistema de emergência, isso é perigoso.
//--- Cada método pega o "agora" UMA vez e passa adiante: todas as contas da mesma resposta usam o mesmo relógio
@Service
public class OcorrenciaService {

    private final OcorrenciaRepository ocorrenciaRepository;
    private final EquipamentoRepository equipamentoRepository;
    private final ControleDeAcesso controleDeAcesso;

    public OcorrenciaService(OcorrenciaRepository ocorrenciaRepository, EquipamentoRepository equipamentoRepository,
                             ControleDeAcesso controleDeAcesso) {
        this.ocorrenciaRepository = ocorrenciaRepository;
        this.equipamentoRepository = equipamentoRepository;
        this.controleDeAcesso = controleDeAcesso;
    }

    //--- o aviso de queda: o mesmo caminho para o dispositivo IoT e para o botão do app
    public OcorrenciaResponse registrarQueda(OcorrenciaRequest ocorrenciaRequest, Authentication authentication) {
        LocalDateTime agora = agora();
        String codigo = ocorrenciaRequest.codigoDispositivo();

        //--- RN07: o alerta chega pelo código do dispositivo; código que não existe = 404
        Equipamento equipamento = equipamentoRepository.findByCodigoDispositivo(codigo)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Dispositivo " + codigo + " não cadastrado"));

        //--- RN15: a família só avisa queda de um equipamento seu (o admin avisa de qualquer um)
        controleDeAcesso.garantirAcesso(authentication, equipamento.getPaciente().getUsuario().getId());

        //--- RN08: a mesma queda não entra duas vezes na fila
        if (ocorrenciaRepository.existsByEquipamentoIdAndStatusNot(equipamento.getId(), StatusOcorrencia.RESOLVIDA)) {
            throw new RegraDeNegocioException("Já existe uma queda em aberto para o dispositivo " + codigo);
        }

        //--- RN09: a conta mora na RegraDePrioridade (a mesma do Oracle, do app e do Arduino).
        //--- A previsão de esgotamento é gravada uma vez e nunca muda: é ela que ordena a fila
        int minutosNoAlerta = RegraDePrioridade.calcularMinutosNoAlerta(equipamento.getAutonomiaMinutos(), ocorrenciaRequest.bateriaPercentual());

        Ocorrencia novaOcorrencia = ocorrenciaRequest.toEntity();
        novaOcorrencia.setEquipamento(equipamento);
        novaOcorrencia.setInicio(agora);
        novaOcorrencia.setMinutosNoAlerta(minutosNoAlerta);
        novaOcorrencia.setPrevisaoEsgotamento(RegraDePrioridade.calcularPrevisaoEsgotamento(agora, minutosNoAlerta));
        novaOcorrencia.setStatus(StatusOcorrencia.ABERTA);

        Ocorrencia ocorrenciaCadastrada = ocorrenciaRepository.save(novaOcorrencia);

        return OcorrenciaResponse.from(ocorrenciaCadastrada, agora, posicaoNaFila(ocorrenciaCadastrada));
    }

    //--- a fila inteira, já na ordem do JPQL (quem fica sem bateria primeiro). A posição é o lugar na lista
    public List<OcorrenciaResponse> consultarFila() {
        LocalDateTime agora = agora();
        List<Ocorrencia> fila = ocorrenciaRepository.consultarFila(StatusOcorrencia.RESOLVIDA);

        List<OcorrenciaResponse> resposta = new ArrayList<>();
        for (int i = 0; i < fila.size(); i++) {
            resposta.add(OcorrenciaResponse.from(fila.get(i), agora, i + 1)); //--- i começa em 0; a 1ª posição é 1
        }
        return resposta;
    }

    //--- filtro dinâmico: só entram no WHERE os filtros que vieram na requisição
    public Page<OcorrenciaResponse> consultar(StatusOcorrencia status, String bairro, Integer minutosMaximos,
                                              Pageable paginacao, Authentication authentication) {
        LocalDateTime agora = agora();
        List<Specification<Ocorrencia>> filtros = new ArrayList<>();

        if (Objects.nonNull(status)) {
            filtros.add(OcorrenciaSpecifications.comStatus(status));
        }
        if (Objects.nonNull(bairro)) {
            filtros.add(OcorrenciaSpecifications.noBairro(bairro));
        }
        if (Objects.nonNull(minutosMaximos)) {
            filtros.add(OcorrenciaSpecifications.esgotaEmAte(minutosMaximos, agora));
        }
        //--- RN15: para a família, o filtro "só as minhas" entra sempre (vai na consulta, então a paginação sai certa)
        if (controleDeAcesso.ehFamilia(authentication)) {
            filtros.add(OcorrenciaSpecifications.doUsuario(authentication.getName()));
        }

        //--- allOf junta os filtros com AND. Lista vazia = sem WHERE (o allOf começa do Specification.unrestricted())
        Page<Ocorrencia> ocorrencias = ocorrenciaRepository.findAll(Specification.allOf(filtros), paginacao);

        return ocorrencias.map(ocorrencia -> OcorrenciaResponse.from(ocorrencia, agora, posicaoNaFila(ocorrencia)));
    }

    public OcorrenciaResponse consultarPorCodigo(Long id, Authentication authentication) {
        LocalDateTime agora = agora();
        Ocorrencia ocorrencia = buscarOcorrencia(id);

        //--- RN15: a família só vê as ocorrências dos seus pacientes (sem cache, a conferência pode ficar aqui)
        controleDeAcesso.garantirAcesso(authentication, ocorrencia.getEquipamento().getPaciente().getUsuario().getId());

        return OcorrenciaResponse.from(ocorrencia, agora, posicaoNaFila(ocorrencia));
    }

    //--- posição = quantas abertas acabam a bateria antes desta + 1. Resolvida não está mais na fila: null
    private Integer posicaoNaFila(Ocorrencia ocorrencia) {
        if (ocorrencia.getStatus() == StatusOcorrencia.RESOLVIDA) {
            return null;
        }
        long antes = ocorrenciaRepository.countByStatusNotAndPrevisaoEsgotamentoLessThan(
                StatusOcorrencia.RESOLVIDA, ocorrencia.getPrevisaoEsgotamento());
        return (int) antes + 1;
    }

    private Ocorrencia buscarOcorrencia(Long id) {
        //--- mensagem pronta no feminino (o construtor (recurso, id) escreveria "não encontrado")
        return ocorrenciaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ocorrência [id=" + id + "] não encontrada."));
    }

    //--- sem os nanossegundos: o horário sai limpo no JSON (2026-11-23T19:30:15) e igual ao que fica gravado no banco
    private LocalDateTime agora() {
        return LocalDateTime.now().withNano(0);
    }

}
