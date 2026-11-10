package br.com.fiap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching //--- liga o Spring Cache (requisito 4 do enunciado): sem esta anotação o @Cacheable é ignorado
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
