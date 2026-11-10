package br.com.fiap.entity;

import br.com.fiap.enums.StatusOcorrencia;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity //--- uma queda de energia na casa do paciente, avisada pelo dispositivo
public class Ocorrencia extends EntidadeBase {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @SequenceGenerator(name = "ocorrencia_seq", allocationSize = 1)
    private Long id;

    private LocalDateTime inicio; //--- quando a luz caiu

    private LocalDateTime fim; //--- quando a energia voltou; nulo enquanto a ocorrência está aberta

    private Integer bateriaPercentual; //--- bateria do aparelho no momento da queda (0 a 100)

    private Integer minutosNoAlerta; //--- autonomia x bateria / 100: quanto a bateria aguentava na hora da queda

    //--- inicio + minutosNoAlerta: o horário em que a bateria acaba. A fila é ordenada por este campo,
    //--- que não muda com o tempo, por isso a ordem da fila nunca precisa ser recalculada
    private LocalDateTime previsaoEsgotamento;

    @Enumerated(EnumType.STRING)
    private StatusOcorrencia status;

    //--- a FK fica do lado "muitos": cada ocorrência aponta para o equipamento que avisou a queda
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipamento_id", nullable = false)
    private Equipamento equipamento;

    //--- o único cascade do modelo: o atendimento não existe sem a ocorrência,
    //--- então salvar (ou apagar) a ocorrência salva (ou apaga) os atendimentos dela junto
    @OneToMany(mappedBy = "ocorrencia", cascade = {
            CascadeType.PERSIST,
            CascadeType.MERGE,
            CascadeType.REMOVE})
    private List<Atendimento> atendimentos = new ArrayList<>(); //--- lista já criada para adicionarAtendimento funcionar em ocorrência nova

    //--- liga os dois lados da relação: o atendimento aponta para esta ocorrência e entra na lista dela
    public void adicionarAtendimento(Atendimento atendimento) {
        atendimento.setOcorrencia(this);
        atendimentos.add(atendimento);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getInicio() {
        return inicio;
    }

    public void setInicio(LocalDateTime inicio) {
        this.inicio = inicio;
    }

    public LocalDateTime getFim() {
        return fim;
    }

    public void setFim(LocalDateTime fim) {
        this.fim = fim;
    }

    public Integer getBateriaPercentual() {
        return bateriaPercentual;
    }

    public void setBateriaPercentual(Integer bateriaPercentual) {
        this.bateriaPercentual = bateriaPercentual;
    }

    public Integer getMinutosNoAlerta() {
        return minutosNoAlerta;
    }

    public void setMinutosNoAlerta(Integer minutosNoAlerta) {
        this.minutosNoAlerta = minutosNoAlerta;
    }

    public LocalDateTime getPrevisaoEsgotamento() {
        return previsaoEsgotamento;
    }

    public void setPrevisaoEsgotamento(LocalDateTime previsaoEsgotamento) {
        this.previsaoEsgotamento = previsaoEsgotamento;
    }

    public StatusOcorrencia getStatus() {
        return status;
    }

    public void setStatus(StatusOcorrencia status) {
        this.status = status;
    }

    public Equipamento getEquipamento() {
        return equipamento;
    }

    public void setEquipamento(Equipamento equipamento) {
        this.equipamento = equipamento;
    }

    public List<Atendimento> getAtendimentos() {
        return atendimentos;
    }

    public void setAtendimentos(List<Atendimento> atendimentos) {
        this.atendimentos = atendimentos;
    }

}
