package br.com.fiap;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

//--- TEMPORARIO: libera tudo ate a Tarefa 2 configurar o JWT
@Configuration
@EnableWebSecurity
public class ConfiguracoesDeSeguranca {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); //--- responsável por criptografar a senha do usuário (a carga inicial já usa)
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        return http
                .csrf(csrf -> csrf.disable())

                //--- o console do H2 é uma página com frames: sem isto o navegador bloqueia a tela dele
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))

                //--- por enquanto qualquer URL é liberada, sem login
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())

                .build();
    }

}
