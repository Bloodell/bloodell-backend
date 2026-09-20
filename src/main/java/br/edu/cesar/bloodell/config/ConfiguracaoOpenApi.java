package br.edu.cesar.bloodell.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConfiguracaoOpenApi {

    @Bean
    public OpenAPI documentacaoBloodell() {
        return new OpenAPI().info(new Info()
                .title("BLOODELL API")
                .version("v1")
                .description("""
                        Gestao, distribuicao e monitoramento de hemocomponentes.

                        AVISO ACADEMICO: projeto integrador da CESAR School. Todos os dados
                        sao sinteticos. Nao ha dado real de doador ou paciente. As regras de
                        compatibilidade ABO/Rh sao didaticas e nao constituem orientacao
                        clinica nem substituem protocolo de hemoterapia.

                        Versionamento: mudanca que quebra contrato exige /api/v2 ou
                        depreciacao anunciada. Ver docs/versionamento-api.md.
                        """)
                .contact(new Contact().name("Squad BLOODELL - ADS 3o semestre"))
                .license(new License().name("Uso academico")));
    }
}
