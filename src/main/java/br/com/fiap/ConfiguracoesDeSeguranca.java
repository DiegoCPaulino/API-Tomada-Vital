package br.com.fiap;

import br.com.fiap.enums.Papel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableWebSecurity
public class ConfiguracoesDeSeguranca {

    //--- segredo que assina o token. Vem do application.properties: em dev é um valor de exemplo;
    //--- no perfil docker vem da variável de ambiente JWT_SECRET (sem ela a aplicação não sobe)
    @Value("${tomada-vital.jwt-secret}")
    private String secret;

    private static final String FAMILIA = Papel.FAMILIA.name();
    private static final String OPERADOR = Papel.OPERADOR.name();
    private static final String ADMIN = Papel.ADMIN.name();

    //--- Configurações para Autenticação dos Usuários
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) {
        return configuration.getAuthenticationManager();
    }

    @Bean
    //--- o UserDetailsService recebido aqui é o nosso UsuarioDetailsService, que busca o usuário no banco
    public AuthenticationProvider authenticationProvider(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService); //--- configurando o serviço responsável por buscar o usuário durante a autenticação
        provider.setPasswordEncoder(passwordEncoder); //--- configurando o responsável por conferir a senha digitada com a senha BCrypt do banco
        return provider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); //--- responsável por criptografar a senha do usuário (cadastro e carga inicial)
    }


    //--- Configurações sobre o Token JWT
    @Bean
    public SecretKey secretKey() {
        return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSha256");
    }

    @Bean
    //--- Configuração da classe responsável pela assinatura do Token JWT. Utilizada na classe GeradorTokenJWTService
    public JwtEncoder jwtEncoder(SecretKey secretKey) {
        return NimbusJwtEncoder
                .withSecretKey(secretKey)
                .algorithm(MacAlgorithm.HS256)
                .build();
    }

    @Bean
    //--- Configuração da classe responsável por decodificar e validar integridade e autenticidade do Token JWT
    public JwtDecoder jwtDecoder(SecretKey secretKey) {
        return NimbusJwtDecoder
                .withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    @Bean
    //--- Configuração das classes responsáveis pela leitura das informações do Token JWT e por recuperar as roles (papéis) do usuário autenticado
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        grantedAuthoritiesConverter.setAuthorityPrefix(""); //--- os papéis já vêm com ROLE_ no token: não acrescenta outro prefixo
        grantedAuthoritiesConverter.setAuthoritiesClaimName("roles"); //--- parâmetro onde as roles do usuário foram configuradas

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);

        return converter;
    }


    //--- Configurações para Autorização de acessos (1ª camada: URL e método HTTP, tabela do contrato).
    //--- A 2ª camada ("a família só mexe no que é seu") fica no ControleDeAcesso.
    //--- As regras são conferidas de cima para baixo e vale a primeira que bater: as mais específicas vêm antes
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationConverter jwtAuthenticationConverter) {
        return http
                .csrf(csrf -> csrf.disable()) //--- API sem sessão nem cookie (o token vai no cabeçalho): o CSRF não se aplica

                //--- o console do H2 é uma página com frames: sem isto o navegador bloqueia a tela dele
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))

                .authorizeHttpRequests(auth -> auth
                        //--- qualquer pessoa: documentação, saúde e informações da API, console do H2 (só existe no perfil dev).
                        //--- /error é a página interna de erro do Spring: sem liberar, um 404 de rota inexistente vira 401
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/actuator/health",
                                "/actuator/info",
                                "/h2-console/**",
                                "/error")
                        .permitAll()

                        //--- o login exige HTTP Basic (login e senha): é ele que devolve o token
                        .requestMatchers(HttpMethod.POST, "/autenticacao/login")
                        .authenticated()

                        //--- métricas mostram o funcionamento interno da API: só o administrador
                        .requestMatchers("/actuator/metrics", "/actuator/metrics/**")
                        .hasRole(ADMIN)

                        //--- PACIENTES
                        //--- só a família cadastra paciente: o paciente nasce ligado à família logada
                        .requestMatchers(HttpMethod.POST, "/api/pacientes")
                        .hasRole(FAMILIA)

                        //--- alterar e apagar: a família (o dono é conferido no ControleDeAcesso) e o admin. O operador só consulta
                        .requestMatchers(HttpMethod.PUT, "/api/pacientes/**")
                        .hasAnyRole(FAMILIA, ADMIN)
                        .requestMatchers(HttpMethod.DELETE, "/api/pacientes/**")
                        .hasAnyRole(FAMILIA, ADMIN)

                        //--- consultar: todos os papéis (a família só recebe os seus; o operador precisa ver todos para atender)
                        .requestMatchers(HttpMethod.GET, "/api/pacientes/**")
                        .hasAnyRole(FAMILIA, OPERADOR, ADMIN)

                        //--- EQUIPAMENTOS
                        //--- cadastrar, alterar e apagar: a família (do seu paciente) e o admin. O operador só consulta
                        .requestMatchers(HttpMethod.POST, "/api/equipamentos")
                        .hasAnyRole(FAMILIA, ADMIN)
                        .requestMatchers(HttpMethod.PUT, "/api/equipamentos/**")
                        .hasAnyRole(FAMILIA, ADMIN)
                        .requestMatchers(HttpMethod.DELETE, "/api/equipamentos/**")
                        .hasAnyRole(FAMILIA, ADMIN)

                        //--- consultar: todos os papéis
                        .requestMatchers(HttpMethod.GET, "/api/equipamentos/**")
                        .hasAnyRole(FAMILIA, OPERADOR, ADMIN)

                        //--- OCORRÊNCIAS
                        //--- a fila de atendimento é a tela da central: só operador e admin.
                        //--- Precisa vir antes do GET /api/ocorrencias/**, senão a família passaria pela regra geral
                        .requestMatchers(HttpMethod.GET, "/api/ocorrencias/fila")
                        .hasAnyRole(OPERADOR, ADMIN)

                        //--- atender e resolver uma queda: só quem trabalha na central (operador e admin)
                        .requestMatchers(HttpMethod.POST, "/api/ocorrencias/*/atendimentos", "/api/ocorrencias/*/resolver")
                        .hasAnyRole(OPERADOR, ADMIN)

                        //--- avisar uma queda: a família (do seu equipamento) e o admin
                        .requestMatchers(HttpMethod.POST, "/api/ocorrencias")
                        .hasAnyRole(FAMILIA, ADMIN)

                        //--- consultar ocorrências e atendimentos: todos os papéis (a família acompanha as suas pelo app)
                        .requestMatchers(HttpMethod.GET, "/api/ocorrencias/**")
                        .hasAnyRole(FAMILIA, OPERADOR, ADMIN)

                        //--- acesso as demais URLs somente para usuários autenticados independente da role (papel) do usuário
                        .anyRequest()
                        .authenticated())

                .httpBasic(Customizer.withDefaults()) //--- configurando tipo de autenticação. HTTP Basic (usado no login)

                .oauth2ResourceServer(resource -> resource
                        .jwt(config -> config.jwtAuthenticationConverter(jwtAuthenticationConverter))) //--- configuração do token JWT (demais requisições)

                .build();
    }

}
