package br.com.fiap.entity;

import jakarta.persistence.*;

import java.util.List;

@Entity //--- a pessoa que depende do aparelho elétrico; pertence a uma família (usuário)
public class Paciente extends EntidadeBase {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @SequenceGenerator(name = "paciente_seq", allocationSize = 1)
    private Long id;

    private String nome;

    private String bairro;

    private String cidade;

    private String telefone;

    //--- a FK fica do lado "muitos": cada paciente aponta para a família dona dele (coluna usuario_id)
    //--- LAZY: o usuário só é buscado no banco se alguém chamar getUsuario()
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    //--- sem cascade: o equipamento tem vida própria (é cadastrado e apagado pelo seu próprio endpoint)
    @OneToMany(mappedBy = "paciente")
    private List<Equipamento> equipamentos;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getBairro() {
        return bairro;
    }

    public void setBairro(String bairro) {
        this.bairro = bairro;
    }

    public String getCidade() {
        return cidade;
    }

    public void setCidade(String cidade) {
        this.cidade = cidade;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public List<Equipamento> getEquipamentos() {
        return equipamentos;
    }

    public void setEquipamentos(List<Equipamento> equipamentos) {
        this.equipamentos = equipamentos;
    }

}
