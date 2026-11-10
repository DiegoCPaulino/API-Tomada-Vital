package br.com.fiap.entity;

import br.com.fiap.enums.AcaoAtendimento;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity //--- cada passo que a central registra numa ocorrência (equipe a caminho, gerador entregue...)
public class Atendimento extends EntidadeBase {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @SequenceGenerator(name = "atendimento_seq", allocationSize = 1)
    private Long id;

    private LocalDateTime dataHora;

    @Enumerated(EnumType.STRING)
    private AcaoAtendimento acao;

    private String observacao; //--- até 200 caracteres (a validação fica no DTO de entrada)

    //--- a FK fica do lado "muitos": cada atendimento aponta para a sua ocorrência (coluna ocorrencia_id)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ocorrencia_id", nullable = false)
    private Ocorrencia ocorrencia;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public void setDataHora(LocalDateTime dataHora) {
        this.dataHora = dataHora;
    }

    public AcaoAtendimento getAcao() {
        return acao;
    }

    public void setAcao(AcaoAtendimento acao) {
        this.acao = acao;
    }

    public String getObservacao() {
        return observacao;
    }

    public void setObservacao(String observacao) {
        this.observacao = observacao;
    }

    public Ocorrencia getOcorrencia() {
        return ocorrencia;
    }

    public void setOcorrencia(Ocorrencia ocorrencia) {
        this.ocorrencia = ocorrencia;
    }

}
