package br.com.fiap.service;

import br.com.fiap.entity.Usuario;
import br.com.fiap.enums.Papel;
import br.com.fiap.exception.AcessoNegadoException;
import br.com.fiap.exception.RecursoNaoEncontradoException;
import br.com.fiap.repository.UsuarioRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

//--- O acesso é controlado em duas camadas:
//--- 1ª) URL e método HTTP (ConfiguracoesDeSeguranca): decide QUEM pode chamar o endpoint, pelo papel do token.
//---     Ex.: família não abre a fila; responde 403 antes de chegar no controller.
//--- 2ª) Dono do registro (esta classe): a URL /api/pacientes/2 é a mesma para todas as famílias, então só olhando
//---     o registro dá para saber se ele é da família logada. O enunciado exige que a família só veja os seus (RN15).
//---     Operador e admin passam direto, porque atendem todas as famílias.
@Component
public class ControleDeAcesso {

    private final UsuarioRepository usuarioRepository;

    public ControleDeAcesso(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public boolean ehFamilia(Authentication authentication) {
        return temPapel(authentication, Papel.FAMILIA);
    }

    //--- quem pode atender e resolver ocorrências (usado para mostrar os links de ação no HATEOAS)
    public boolean podeAtender(Authentication authentication) {
        return temPapel(authentication, Papel.OPERADOR) || temPapel(authentication, Papel.ADMIN);
    }

    //--- o token guarda o login (sub); o id do usuário vem do banco
    public Long usuarioIdLogado(Authentication authentication) {
        String login = authentication.getName();
        Usuario usuario = usuarioRepository.findByLogin(login)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário " + login + " não encontrado."));
        return usuario.getId();
    }

    public void garantirAcesso(Authentication authentication, Long usuarioIdDono) {
        //--- operador e admin passam sem consultar o banco
        if (!ehFamilia(authentication)) {
            return;
        }

        if (!usuarioIdLogado(authentication).equals(usuarioIdDono)) {
            throw new AcessoNegadoException("Este registro pertence a outra família.");
        }
    }

    //--- o papel chega como authority com o prefixo ROLE_ (o .roles(...) do UsuarioDetailsService acrescenta)
    private boolean temPapel(Authentication authentication, Papel papel) {
        String authority = "ROLE_" + papel.name();
        return authentication.getAuthorities()
                .stream()
                .anyMatch(concedida -> concedida.getAuthority().equals(authority));
    }

}
