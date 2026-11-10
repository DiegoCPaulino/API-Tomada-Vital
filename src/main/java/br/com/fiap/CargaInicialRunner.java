package br.com.fiap;

import br.com.fiap.entity.*;
import br.com.fiap.enums.AcaoAtendimento;
import br.com.fiap.enums.Papel;
import br.com.fiap.enums.StatusOcorrencia;
import br.com.fiap.enums.TipoEquipamento;
import br.com.fiap.repository.*;
import br.com.fiap.service.RegraDePrioridade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

//--- Roda uma vez sempre que a aplicação sobe e grava os dados de teste do grupo (os mesmos do Oracle e do app).
//--- A ordem de gravação define os ids: usuários -> pacientes -> equipamentos -> ocorrências (com os atendimentos)
@Component
public class CargaInicialRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(CargaInicialRunner.class);

    private static final String SENHA_DE_TESTE = "123456"; //--- senha dos usuários de teste (vai no README)

    private final UsuarioRepository usuarioRepository;
    private final PacienteRepository pacienteRepository;
    private final EquipamentoRepository equipamentoRepository;
    private final OcorrenciaRepository ocorrenciaRepository;
    private final AtendimentoRepository atendimentoRepository;
    private final PasswordEncoder passwordEncoder;

    public CargaInicialRunner(UsuarioRepository usuarioRepository,
                              PacienteRepository pacienteRepository,
                              EquipamentoRepository equipamentoRepository,
                              OcorrenciaRepository ocorrenciaRepository,
                              AtendimentoRepository atendimentoRepository,
                              PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.pacienteRepository = pacienteRepository;
        this.equipamentoRepository = equipamentoRepository;
        this.ocorrenciaRepository = ocorrenciaRepository;
        this.atendimentoRepository = atendimentoRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional //--- tudo numa transação só: se algo falhar no meio, nada fica gravado pela metade
    public void run(String... args) {
        //--- guarda de idempotência: o banco é em arquivo (H2) ou em volume (MySQL) e sobrevive ao restart.
        //--- Sem esta conferência, os dados seriam gravados de novo (duplicados) a cada vez que a aplicação sobe
        if (usuarioRepository.existsByLogin("admin")) {
            log.info("Carga inicial já existente");
            return;
        }

        //--- as quedas abertas são relativas ao momento em que a aplicação sobe: com data fixa,
        //--- no dia da apresentação todas já estariam com 0 minutos de bateria
        LocalDateTime agora = LocalDateTime.now().withSecond(0).withNano(0);

        //--- 1) usuários: familia1 a familia10 (a familiaN é dona do paciente N), operador e admin
        List<Usuario> familias = new ArrayList<>();
        for (int i = 0; i < PACIENTES.size(); i++) {
            String nomePaciente = PACIENTES.get(i)[0];
            familias.add(salvarUsuario("familia" + (i + 1), "Família de " + nomePaciente, Papel.FAMILIA));
        }
        salvarUsuario("operador", "Operador da Central", Papel.OPERADOR);
        salvarUsuario("admin", "Administrador", Papel.ADMIN);

        //--- 2) pacientes, cada um ligado à sua família
        List<Paciente> pacientes = new ArrayList<>();
        for (int i = 0; i < PACIENTES.size(); i++) {
            pacientes.add(salvarPaciente(PACIENTES.get(i), familias.get(i)));
        }

        //--- 3) equipamentos (a lista começa em 0: pacientes.get(0) é o paciente 1, a Maria)
        Equipamento tv001 = salvarEquipamento(pacientes.get(0), TipoEquipamento.CONCENTRADOR_O2, 480, "TV-001");
        Equipamento tv002 = salvarEquipamento(pacientes.get(0), TipoEquipamento.ASPIRADOR, 240, "TV-002");
        Equipamento tv003 = salvarEquipamento(pacientes.get(1), TipoEquipamento.VENTILADOR, 360, "TV-003");
        Equipamento tv004 = salvarEquipamento(pacientes.get(2), TipoEquipamento.CONCENTRADOR_O2, 300, "TV-004");
        Equipamento tv005 = salvarEquipamento(pacientes.get(3), TipoEquipamento.BOMBA_INFUSAO, 600, "TV-005");
        Equipamento tv006 = salvarEquipamento(pacientes.get(4), TipoEquipamento.CONCENTRADOR_O2, 480, "TV-006");
        Equipamento tv007 = salvarEquipamento(pacientes.get(5), TipoEquipamento.ASPIRADOR, 180, "TV-007");
        Equipamento tv008 = salvarEquipamento(pacientes.get(6), TipoEquipamento.VENTILADOR, 0, "TV-008");
        Equipamento tv009 = salvarEquipamento(pacientes.get(7), TipoEquipamento.CONCENTRADOR_O2, 420, "TV-009");
        Equipamento tv010 = salvarEquipamento(pacientes.get(8), TipoEquipamento.VENTILADOR, 720, "TV-010");
        salvarEquipamento(pacientes.get(9), TipoEquipamento.OUTRO, 120, "TV-011"); //--- sem queda: livre para a demonstração ao vivo

        //--- 4) ocorrências abertas (início "X minutos antes de agora"), com os seus atendimentos
        Ocorrencia ocorrencia1 = novaOcorrencia(tv001, agora.minusMinutes(38), null, 20, StatusOcorrencia.EM_ATENDIMENTO);
        ocorrencia1.adicionarAtendimento(novoAtendimento(agora.minusMinutes(22), AcaoAtendimento.EQUIPE_A_CAMINHO, "Previsão de chegada em 30 minutos"));
        ocorrenciaRepository.save(ocorrencia1); //--- o cascade grava o atendimento junto

        ocorrenciaRepository.save(novaOcorrencia(tv003, agora.minusMinutes(35), null, 70, StatusOcorrencia.ABERTA));
        ocorrenciaRepository.save(novaOcorrencia(tv004, agora.minusMinutes(34), null, 50, StatusOcorrencia.ABERTA));
        ocorrenciaRepository.save(novaOcorrencia(tv005, agora.minusMinutes(32), null, 90, StatusOcorrencia.ABERTA));

        Ocorrencia ocorrencia5 = novaOcorrencia(tv008, agora.minusMinutes(30), null, 0, StatusOcorrencia.EM_ATENDIMENTO);
        ocorrencia5.adicionarAtendimento(novoAtendimento(agora.minusMinutes(28), AcaoAtendimento.EQUIPE_A_CAMINHO, "Aparelho sem bateria: prioridade máxima"));
        ocorrencia5.adicionarAtendimento(novoAtendimento(agora.minusMinutes(5), AcaoAtendimento.GERADOR_ENTREGUE, "Gerador portátil instalado na casa"));
        ocorrenciaRepository.save(ocorrencia5);

        ocorrenciaRepository.save(novaOcorrencia(tv006, agora.minusMinutes(28), null, 35, StatusOcorrencia.ABERTA));
        ocorrenciaRepository.save(novaOcorrencia(tv007, agora.minusMinutes(25), null, 60, StatusOcorrencia.ABERTA));

        //--- 5) ocorrências resolvidas: datas fixas, são o histórico real dos apagões
        Ocorrencia ocorrencia8 = novaOcorrencia(tv009, LocalDateTime.of(2025, 12, 10, 18, 30), LocalDateTime.of(2025, 12, 10, 22, 10), 80, StatusOcorrencia.RESOLVIDA);
        ocorrencia8.adicionarAtendimento(novoAtendimento(LocalDateTime.of(2025, 12, 10, 18, 50), AcaoAtendimento.EQUIPE_A_CAMINHO, "Equipe saiu da base Cotia"));
        ocorrencia8.adicionarAtendimento(novoAtendimento(LocalDateTime.of(2025, 12, 10, 19, 20), AcaoAtendimento.GERADOR_ENTREGUE, "Gerador entregue à família"));
        ocorrencia8.adicionarAtendimento(novoAtendimento(LocalDateTime.of(2025, 12, 10, 22, 10), AcaoAtendimento.ENERGIA_RESTABELECIDA, "Rede religada pela concessionária"));
        ocorrenciaRepository.save(ocorrencia8);

        Ocorrencia ocorrencia9 = novaOcorrencia(tv010, LocalDateTime.of(2025, 12, 10, 19, 0), LocalDateTime.of(2025, 12, 11, 1, 0), 100, StatusOcorrencia.RESOLVIDA);
        ocorrencia9.adicionarAtendimento(novoAtendimento(LocalDateTime.of(2025, 12, 10, 21, 0), AcaoAtendimento.PACIENTE_REMOVIDO, "Paciente levada a ponto de apoio com gerador"));
        ocorrencia9.adicionarAtendimento(novoAtendimento(LocalDateTime.of(2025, 12, 11, 1, 0), AcaoAtendimento.ENERGIA_RESTABELECIDA, "Rede religada e paciente voltou para casa"));
        ocorrenciaRepository.save(ocorrencia9);

        Ocorrencia ocorrencia10 = novaOcorrencia(tv002, LocalDateTime.of(2024, 10, 11, 17, 40), LocalDateTime.of(2024, 10, 11, 21, 10), 100, StatusOcorrencia.RESOLVIDA);
        ocorrencia10.adicionarAtendimento(novoAtendimento(LocalDateTime.of(2024, 10, 11, 18, 10), AcaoAtendimento.EQUIPE_A_CAMINHO, "Equipe a caminho"));
        ocorrencia10.adicionarAtendimento(novoAtendimento(LocalDateTime.of(2024, 10, 11, 21, 10), AcaoAtendimento.ENERGIA_RESTABELECIDA, "Rede religada pela concessionária"));
        ocorrenciaRepository.save(ocorrencia10);

        //--- log de conferência: as contagens precisam bater com docs/dados-carga.md
        log.info("Carga inicial: {} usuários, {} pacientes, {} equipamentos, {} ocorrências, {} atendimentos",
                usuarioRepository.count(), pacienteRepository.count(), equipamentoRepository.count(),
                ocorrenciaRepository.count(), atendimentoRepository.count());
    }

    private Usuario salvarUsuario(String login, String nome, Papel papel) {
        Usuario usuario = new Usuario();
        usuario.setLogin(login);
        usuario.setNome(nome);
        usuario.setSenha(passwordEncoder.encode(SENHA_DE_TESTE)); //--- no banco fica só o hash BCrypt
        usuario.setPapel(papel);
        return usuarioRepository.save(usuario);
    }

    private Paciente salvarPaciente(String[] dados, Usuario familia) {
        Paciente paciente = new Paciente();
        paciente.setNome(dados[0]);
        paciente.setBairro(dados[1]);
        paciente.setCidade(dados[2]);
        paciente.setTelefone(dados[3]);
        paciente.setUsuario(familia);
        return pacienteRepository.save(paciente);
    }

    private Equipamento salvarEquipamento(Paciente paciente, TipoEquipamento tipo, int autonomiaMinutos, String codigoDispositivo) {
        Equipamento equipamento = new Equipamento();
        equipamento.setPaciente(paciente);
        equipamento.setTipo(tipo);
        equipamento.setAutonomiaMinutos(autonomiaMinutos);
        equipamento.setCodigoDispositivo(codigoDispositivo);
        return equipamentoRepository.save(equipamento);
    }

    private Ocorrencia novaOcorrencia(Equipamento equipamento, LocalDateTime inicio, LocalDateTime fim,
                                      int bateriaPercentual, StatusOcorrencia status) {
        //--- os minutos e a previsão saem sempre da regra de negócio, nunca são digitados
        int minutosNoAlerta = RegraDePrioridade.calcularMinutosNoAlerta(equipamento.getAutonomiaMinutos(), bateriaPercentual);

        Ocorrencia ocorrencia = new Ocorrencia();
        ocorrencia.setEquipamento(equipamento);
        ocorrencia.setInicio(inicio);
        ocorrencia.setFim(fim);
        ocorrencia.setBateriaPercentual(bateriaPercentual);
        ocorrencia.setMinutosNoAlerta(minutosNoAlerta);
        ocorrencia.setPrevisaoEsgotamento(RegraDePrioridade.calcularPrevisaoEsgotamento(inicio, minutosNoAlerta));
        ocorrencia.setStatus(status);
        return ocorrencia;
    }

    private Atendimento novoAtendimento(LocalDateTime dataHora, AcaoAtendimento acao, String observacao) {
        Atendimento atendimento = new Atendimento();
        atendimento.setDataHora(dataHora);
        atendimento.setAcao(acao);
        atendimento.setObservacao(observacao);
        return atendimento;
    }


    //--- pacientes na ordem dos ids: nome, bairro, cidade, telefone
    private static final List<String[]> PACIENTES = List.of(
            new String[]{"Maria Aparecida Souza", "Parque Pinheiros", "Taboão da Serra", "(11) 90000-0001"},
            new String[]{"José Carlos Lima", "Jardim Record", "Taboão da Serra", "(11) 90000-0002"},
            new String[]{"Antônia Ferreira", "Campo Limpo", "São Paulo", "(11) 90000-0003"},
            new String[]{"Sebastião Rocha", "Jabaquara", "São Paulo", "(11) 90000-0004"},
            new String[]{"Helena Martins", "Santo Amaro", "São Paulo", "(11) 90000-0005"},
            new String[]{"Raimundo Alves", "Pedreira", "São Paulo", "(11) 90000-0006"},
            new String[]{"Lúcia Gomes", "Centro", "Cotia", "(11) 90000-0007"},
            new String[]{"Francisco Nunes", "Granja Viana", "Cotia", "(11) 90000-0008"},
            new String[]{"Teresa Cardoso", "Rudge Ramos", "São Bernardo do Campo", "(11) 90000-0009"},
            new String[]{"Benedito Ramos", "Vila Mariana", "São Paulo", "(11) 90000-0010"}
    );

}
