package br.edu.cesar.bloodell;

import br.edu.cesar.bloodell.config.PropriedadesBloodell;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(PropriedadesBloodell.class)
public class BloodellApplication {

    public static void main(String[] args) {
        SpringApplication.run(BloodellApplication.class, args);
    }
}