package br.com.fiap.service;

import br.com.fiap.dto.AtendimentoRequest;
import br.com.fiap.dto.AtendimentoResponse;
import br.com.fiap.dto.OcorrenciaRequest;
import br.com.fiap.dto.OcorrenciaResponse;
import br.com.fiap.dto.ResolverRequest;
import br.com.fiap.entity.Atendimento;
import br.com.fiap.entity.Equipamento;
import br.com.fiap.entity.Ocorrencia;
import br.com.fiap.enums.AcaoAtendimento;
import br.com.fiap.enums.StatusOcorrencia;
import br.com.fiap.exception.RecursoNaoEncontradoException;
import br.com.fiap.exception.RegraDeNegocioException;
import br.com.fiap.repository.AtendimentoRepository;
import br.com.fiap.repository.EquipamentoRepository;
import br.com.fiap.repository.OcorrenciaRepository;
import br.com.fiap.repository.OcorrenciaSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final AtendimentoRepository atendimentoRepository;
    private final ControleDeAcesso controleDeAcesso;

    public OcorrenciaService(OcorrenciaRepository ocorrenciaRepository, EquipamentoRepository equipamentoRepository,
                             AtendimentoRepository atendimentoRepository, ControleDeAcesso controleDeAcesso) {
        this.ocorrenciaRepository = ocorrenciaRepository;
        this.equipamentoRepository = equipamentoRepository;
        this.atendimentoRepository = atendimentoRepository;
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
        controleDeAcesso.garantirAcesso(authentication, usuarioIdDono(ocorrencia));

        return OcorrenciaResponse.from(ocorrencia, agora, posicaoNaFila(ocorrencia));
    }

    //--- a linha do tempo que a família acompanha pelo app, do primeiro ao último passo
    public List<AtendimentoResponse> consultarAtendimentos(Long id, Authentication authentication) {
        Ocorrencia ocorrencia = buscarOcorrencia(id);
        controleDeAcesso.garantirAcesso(authentication, usuarioIdDono(ocorrencia));

        return atendimentoRepository.findByOcorrenciaIdOrderByDataHoraAsc(id)
                .stream()
                .map(AtendimentoResponse::from)
                .toList();
    }

    //--- @Transactional: aqui duas gravações precisam acontecer JUNTAS — o novo atendimento e a mudança de status
    //--- da ocorrência (RN12). Se uma falhar, a outra é desfeita (nunca fica "atendimento gravado com status ABERTA").
    //--- O professor não usa porque os métodos dele gravam uma coisa só (como o nosso registrarQueda)
    @Transactional
    public AtendimentoResponse registrarAtendimento(Long id, AtendimentoRequest atendimentoRequest) {
        Ocorrencia ocorrencia = buscarOcorrencia(id);

        //--- RN10: ocorrência encerrada é histórico
        if (ocorrencia.getStatus() == StatusOcorrencia.RESOLVIDA) {
            throw new RegraDeNegocioException("A ocorrência já foi resolvida e não recebe novos atendimentos");
        }

        //--- RN11: a volta da energia encerra a ocorrência, por isso só entra pelo resolver
        if (atendimentoRequest.acao() == AcaoAtendimento.ENERGIA_RESTABELECIDA) {
            throw new RegraDeNegocioException("A energia restabelecida é registrada pelo resolver da ocorrência");
        }

        //--- RN12: o primeiro atendimento tira a ocorrência de ABERTA (alguém da central assumiu o caso)
        if (ocorrencia.getStatus() == StatusOcorrencia.ABERTA) {
            ocorrencia.setStatus(StatusOcorrencia.EM_ATENDIMENTO);
        }

        Atendimento novoAtendimento = atendimentoRequest.toEntity();
        novoAtendimento.setDataHora(agora());
        ocorrencia.adicionarAtendimento(novoAtendimento);

        //--- salva a ocorrência e o cascade grava o atendimento junto; o flush manda tudo ao banco agora (gera o id)
        Ocorrencia ocorrenciaAtualizada = ocorrenciaRepository.saveAndFlush(ocorrencia);

        //--- o atendimento gravado (já com o id gerado) é o último da lista da ocorrência
        Atendimento atendimentoCadastrado = ocorrenciaAtualizada.getAtendimentos().getLast();
        return AtendimentoResponse.from(atendimentoCadastrado);
    }

    //--- @Transactional pelo mesmo motivo: fim, status RESOLVIDA e o atendimento final vão juntos ou não vão
    @Transactional
    public OcorrenciaResponse resolver(Long id, ResolverRequest resolverRequest) {
        LocalDateTime agora = agora();
        Ocorrencia ocorrencia = buscarOcorrencia(id);

        if (ocorrencia.getStatus() == StatusOcorrencia.RESOLVIDA) {
            throw new RegraDeNegocioException("A ocorrência já foi resolvida");
        }

        //--- RN13: só resolve o que está em atendimento (alguém da central precisa ter atendido antes)
        if (ocorrencia.getStatus() != StatusOcorrencia.EM_ATENDIMENTO) {
            throw new RegraDeNegocioException("A ocorrência precisa ser atendida antes de ser resolvida");
        }

        //--- o corpo é opcional: sem observação, fica o texto padrão
        String observacao = "Energia restabelecida";
        if (Objects.nonNull(resolverRequest) && Objects.nonNull(resolverRequest.observacao())
                && !resolverRequest.observacao().isBlank()) {
            observacao = resolverRequest.observacao();
        }

        //--- RN14: fecha a ocorrência e acrescenta (por cascade) o último passo da linha do tempo
        Atendimento encerramento = new Atendimento();
        encerramento.setDataHora(agora);
        encerramento.setAcao(AcaoAtendimento.ENERGIA_RESTABELECIDA);
        encerramento.setObservacao(observacao);

        ocorrencia.setFim(agora);
        ocorrencia.setStatus(StatusOcorrencia.RESOLVIDA);
        ocorrencia.adicionarAtendimento(encerramento);

        Ocorrencia ocorrenciaResolvida = ocorrenciaRepository.saveAndFlush(ocorrencia);

        //--- resolvida sai da fila: posição nula
        return OcorrenciaResponse.from(ocorrenciaResolvida, agora, null);
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

    //--- o dono da ocorrência é a família do paciente: ocorrência -> equipamento -> paciente -> usuário
    private Long usuarioIdDono(Ocorrencia ocorrencia) {
        return ocorrencia.getEquipamento().getPaciente().getUsuario().getId();
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
