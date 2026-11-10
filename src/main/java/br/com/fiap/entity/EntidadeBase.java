package br.com.fiap.entity;

import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

import java.time.LocalDate;

//--- @MappedSuperclass: não vira tabela; os campos daqui entram na tabela de cada entidade que estende esta classe
@MappedSuperclass
public class EntidadeBase {

    private LocalDate cadastradoEm;

    private LocalDate atualizadoEm;

    public LocalDate getCadastradoEm() {
        return cadastradoEm;
    }

    //--- chamado pelo JPA logo antes do INSERT
    @PrePersist
    public void configurarDataCadastrado() {
        this.cadastradoEm = LocalDate.now();
    }

    public LocalDate getAtualizadoEm() {
        return atualizadoEm;
    }

    //--- chamado pelo JPA logo antes do UPDATE
    @PreUpdate
    public void configurarDataDeAtualizacao() {
        this.atualizadoEm = LocalDate.now();
    }

}
