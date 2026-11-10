package br.com.fiap.entity;

import br.com.fiap.enums.Papel;
import jakarta.persistence.*;

import java.util.List;

@Entity //--- quem faz login na API: família, operador da central ou administrador
public class Usuario extends EntidadeBase {

    @Id //--- chave primária | PK
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @SequenceGenerator(name = "usuario_seq", allocationSize = 1) //--- allocationSize = 1: os ids saem 1, 2, 3... sem pular
    private Long id;

    private String login; //--- único: conferido no service com existsByLogin antes de salvar

    private String senha; //--- guardada sempre criptografada com BCrypt, nunca em texto puro

    private String nome;

    @Enumerated(EnumType.STRING) //--- grava o nome (FAMILIA) e não a posição (0) do enum
    private Papel papel;

    //--- lado "um" da relação: mappedBy diz que quem guarda a FK é o atributo "usuario" do Paciente
    @OneToMany(mappedBy = "usuario")
    private List<Paciente> pacientes;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public Papel getPapel() {
        return papel;
    }

    public void setPapel(Papel papel) {
        this.papel = papel;
    }

    public List<Paciente> getPacientes() {
        return pacientes;
    }

    public void setPacientes(List<Paciente> pacientes) {
        this.pacientes = pacientes;
    }

}
