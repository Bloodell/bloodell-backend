package br.edu.cesar.bloodell.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ConfiguracaoWeb implements WebMvcConfigurer {

    private final PropriedadesBloodell propriedades;

    public ConfiguracaoWeb(PropriedadesBloodell propriedades) {
        this.propriedades = propriedades;
    }

    @Override
    public void addCorsMappings(CorsRegistry registro) {
        registro.addMapping("/api/v1/**")
                .allowedOrigins(propriedades.getCors().origensComoVetor())
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders(FiltroDeCorrelacao.CABECALHO)
                .maxAge(3600);
    }
}
