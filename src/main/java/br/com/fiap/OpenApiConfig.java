package br.com.fiap;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

//--- Configuração do documento OpenAPI (Swagger) com os dois jeitos de se autenticar da API:
//--- basicAuth (login e senha, só no POST /autenticacao/login) e bearerAuth (o Token JWT, nos demais endpoints)
@Configuration
@SecurityScheme(
        name = "basicAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "basic") //--- Autenticação por HTTP Basic
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer", //--- Utilização de Token JWT
        bearerFormat = "JWT")
public class OpenApiConfig {

    @Bean
    public OpenAPI apiInfo() {
        return new OpenAPI()
                .info(new Info()
                        .title("API Tomada Vital")
                        .description("Pacientes que dependem de um aparelho elétrico para viver correm risco quando falta luz em casa. "
                                + "Esta API registra a queda de energia avisada pelo dispositivo na tomada e monta a fila de atendimento "
                                + "por tempo de bateria: quem tem menos bateria é atendido primeiro."));
    }

}
