package br.com.fiap.repository;

import br.com.fiap.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    //--- verificação utilizando atributos da entidade: o Spring gera o SELECT pelo nome do método
    boolean existsByLogin(String login);

    //--- usado no login (UsuarioDetailsService) e para descobrir o id do usuário logado (ControleDeAcesso).
    //--- Optional porque o login pode não existir
    Optional<Usuario> findByLogin(String login);
}
