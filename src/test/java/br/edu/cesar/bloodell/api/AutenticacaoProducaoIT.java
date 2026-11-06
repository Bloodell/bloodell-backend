package br.edu.cesar.bloodell.api;

import static org.assertj.core.api.Assertions.assertThat;
import br.edu.cesar.bloodell.config.PropriedadesBloodell;
import br.edu.cesar.bloodell.suporte.TesteDeIntegracao;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

@AutoConfigureTestRestTemplate
@ActiveProfiles({"test", "prod"})
class AutenticacaoProducaoIT extends TesteDeIntegracao {
    @Autowired private TestRestTemplate cliente;
    @Autowired private PropriedadesBloodell propriedades;

    @Test
    void perfilDeProducaoExigeJwtEmRotasProtegidas() {
        assertThat(propriedades.getSeguranca().isAutenticacaoHabilitada()).isTrue();
        assertThat(cliente.getForEntity("/api/v1/hemocentros", String.class).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
