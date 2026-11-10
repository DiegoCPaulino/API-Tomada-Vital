package br.com.fiap.service;

import br.com.fiap.dto.UsuarioCadastroRequest;
import br.com.fiap.dto.UsuarioResponse;
import br.com.fiap.entity.Usuario;
import br.com.fiap.enums.Papel;
import br.com.fiap.exception.RegraDeNegocioException;
import br.com.fiap.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

//--- cadastro de usuários: requisito do enunciado (o professor não tem, porque usa usuários fixos em memória)
@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UsuarioResponse cadastrar(UsuarioCadastroRequest usuarioRequest) {
        //--- RN01: o login é único (conferido antes de salvar, como o professor faz com o RM)
        if (usuarioRepository.existsByLogin(usuarioRequest.login())) {
            throw new RegraDeNegocioException("Já existe um usuário com o login " + usuarioRequest.login());
        }

        Usuario novoUsuario = usuarioRequest.toEntity();

        //--- a senha é guardada só criptografada (BCrypt): nem quem acessa o banco consegue ler a senha original
        novoUsuario.setSenha(passwordEncoder.encode(usuarioRequest.senha()));

        //--- RN02: o cadastro público cria sempre FAMILIA. Operador e admin não se cadastram sozinhos
        novoUsuario.setPapel(Papel.FAMILIA);

        Usuario usuarioCadastrado = usuarioRepository.save(novoUsuario);

        return UsuarioResponse.from(usuarioCadastrado);
    }

}
