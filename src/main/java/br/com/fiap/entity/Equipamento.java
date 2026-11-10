package br.com.fiap.entity;

import br.com.fiap.enums.TipoEquipamento;
import jakarta.persistence.*;

import java.util.List;

@Entity //--- o aparelho ligado na tomada onde fica o dispositivo Tomada Vital
public class Equipamento extends EntidadeBase {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @SequenceGenerator(name = "equipamento_seq", allocationSize = 1)
    private Long id;

    @Enumerated(EnumType.STRING)
    private TipoEquipamento tipo;

    private Integer autonomiaMinutos; //--- quanto a bateria cheia aguenta; 0 = aparelho sem bateria

    private String codigoDispositivo; //--- código do dispositivo na tomada (TV-001); único, conferido no service

    //--- a FK fica do lado "muitos": cada equipamento aponta para o seu paciente (coluna paciente_id)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paciente_id", nullable = false)
    private Paciente paciente;

    //--- sem cascade: a ocorrência é histórico e não deve sumir junto com o equipamento
    @OneToMany(mappedBy = "equipamento")
    private List<Ocorrencia> ocorrencias;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TipoEquipamento getTipo() {
        return tipo;
    }

    public void setTipo(TipoEquipamento tipo) {
        this.tipo = tipo;
    }

    public Integer getAutonomiaMinutos() {
        return autonomiaMinutos;
    }

    public void setAutonomiaMinutos(Integer autonomiaMinutos) {
        this.autonomiaMinutos = autonomiaMinutos;
    }

    public String getCodigoDispositivo() {
        return codigoDispositivo;
    }

    public void setCodigoDispositivo(String codigoDispositivo) {
        this.codigoDispositivo = codigoDispositivo;
    }

    public Paciente getPaciente() {
        return paciente;
    }

    public void setPaciente(Paciente paciente) {
        this.paciente = paciente;
    }

    public List<Ocorrencia> getOcorrencias() {
        return ocorrencias;
    }

    public void setOcorrencias(List<Ocorrencia> ocorrencias) {
        this.ocorrencias = ocorrencias;
    }

}
