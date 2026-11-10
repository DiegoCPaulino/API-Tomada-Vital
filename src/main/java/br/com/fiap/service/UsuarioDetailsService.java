package br.com.fiap.service;

import br.com.fiap.entity.Usuario;
import br.com.fiap.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

//--- O professor usa usuários fixos em memória (InMemoryUserDetailsManager). Aqui eles vêm do banco,
//--- porque o enunciado exige cadastro de usuários. O DaoAuthenticationProvider chama este serviço no login
//--- para buscar o usuário e depois confere a senha digitada com a senha BCrypt guardada
@Service
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String login) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByLogin(login)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário " + login + " não encontrado."));

        //--- .roles(...) grava o papel com o prefixo ROLE_ (FAMILIA vira ROLE_FAMILIA), que é o que o hasRole procura
        return User
                .withUsername(usuario.getLogin())
                .password(usuario.getSenha()) //--- a senha já está criptografada no banco
                .roles(usuario.getPapel().name())
                .build();
    }

}
