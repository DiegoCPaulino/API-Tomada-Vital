package br.com.fiap.controller;

import br.com.fiap.dto.TokenResponse;
import br.com.fiap.dto.UsuarioCadastroRequest;
import br.com.fiap.dto.UsuarioResponse;
import br.com.fiap.service.GeradorTokenJWTService;
import br.com.fiap.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/autenticacao")
@Tag(name = "Autenticação", description = "Cadastro da família pelo app e login de quem usa a API: a família do paciente, " +
        "o operador da central da concessionária e o administrador. O login devolve o Token JWT exigido em todos os outros " +
        "endpoints; o papel que vai dentro dele define o que cada um pode fazer.") //--- agrupando os endpoints por API
public class AutenticacaoResource {

    private final GeradorTokenJWTService geradorTokenService;
    private final UsuarioService usuarioService;

    public AutenticacaoResource(GeradorTokenJWTService geradorTokenService, UsuarioService usuarioService) {
        this.geradorTokenService = geradorTokenService;
        this.usuarioService = usuarioService;
    }

    //--- sem security no @Operation: o cadastro é público (quem ainda não tem conta não tem login nem token)
    @Operation(
            summary = "Cadastrar família.",
            description = "Cadastro público, feito pela família no app antes de cadastrar o paciente e os aparelhos. " +
                    "O usuário é criado sempre com o papel FAMILIA: ninguém se cadastra como operador ou administrador.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Usuário cadastrado com sucesso.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = UsuarioResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Dados inválidos ou login já cadastrado.",
                    content = @Content
            )
    })
    @PostMapping(path = "/cadastro", consumes = MediaType.APPLICATION_JSON_VALUE)
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Dados do usuário a ser cadastrado.",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = UsuarioCadastroRequest.class)))
    public ResponseEntity<UsuarioResponse> cadastrar(@Valid @RequestBody UsuarioCadastroRequest novoUsuario) {
        UsuarioResponse usuarioResponse = usuarioService.cadastrar(novoUsuario);
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioResponse);
    }

    @Operation(
            summary = "Autenticar usuário.",
            description = "Autentica via HTTP Basic (login e senha) e retorna um Token JWT válido por 1 hora, com o papel do " +
                    "usuário (FAMILIA, OPERADOR ou ADMIN). O app e a tela da central mandam esse token no cabeçalho " +
                    "Authorization: Bearer em todas as outras chamadas. A validade de 1 hora evita pedir login à família " +
                    "a toda hora durante uma queda de energia.",
            security = @SecurityRequirement(name = "basicAuth")) //--- Autenticação HTTP Basic
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Usuário autenticado com sucesso.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = TokenResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Credenciais inválidas.",
                    content = @Content
            )
    })
    @PostMapping("/login")
    //--- quando o método roda, o Spring Security já conferiu login e senha (HTTP Basic) e entrega o usuário autenticado
    public ResponseEntity<TokenResponse> login(Authentication authentication) {
        TokenResponse tokenResponse = geradorTokenService.gerar(authentication);
        return ResponseEntity.ok(tokenResponse);
    }

}
