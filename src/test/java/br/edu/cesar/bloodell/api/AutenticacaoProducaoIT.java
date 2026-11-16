package br.edu.cesar.bloodell.api;

import static org.assertj.core.api.Assertions.assertThat;
import br.edu.cesar.bloodell.config.PropriedadesBloodell;
import br.edu.cesar.bloodell.suporte.TesteDeIntegracao;
import java.security.SecureRandom;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@AutoConfigureTestRestTemplate
@ActiveProfiles({"test", "prod"})
class AutenticacaoProducaoIT extends TesteDeIntegracao {
    private static final String SEGREDO_JWT = Base64.getEncoder().encodeToString(new SecureRandom().generateSeed(32));

    @Autowired private TestRestTemplate cliente;
    @Autowired private PropriedadesBloodell propriedades;

    @DynamicPropertySource
    static void configurarSegredoJwt(DynamicPropertyRegistry registro) {
        registro.add("bloodell.seguranca.jwt.segredo-base64", () -> SEGREDO_JWT);
    }

    @Test
    void perfilDeProducaoExigeJwtEmRotasProtegidas() {
        assertThat(propriedades.getSeguranca().isAutenticacaoHabilitada()).isTrue();
        assertThat(cliente.getForEntity("/api/v1/hemocentros", String.class).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
