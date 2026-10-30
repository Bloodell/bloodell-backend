package br.edu.cesar.bloodell.autenticacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import br.edu.cesar.bloodell.autenticacao.infraestrutura.ConfiguracaoJwt;
import br.edu.cesar.bloodell.autenticacao.infraestrutura.PropriedadesJwt;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class ConfiguracaoJwtTest {
    private final ConfiguracaoJwt configuracao = new ConfiguracaoJwt();

    @Test
    void rejeitaSegredoAusenteInvalidoOuCurtoSemVazarValor() {
        for (String segredo : new String[] {null, "", "segredo-invalido!", "Y3VydG8="}) {
            var propriedades = new PropriedadesJwt();
            propriedades.setSegredoBase64(segredo);
            assertThatThrownBy(() -> configuracao.chaveJwt(propriedades)).isInstanceOf(IllegalStateException.class);
        }
    }

    @Test
    void aceitaChaveForteERejeitaValidadeForaDosLimites() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        var propriedades = new PropriedadesJwt();
        propriedades.setSegredoBase64(Base64.getEncoder().encodeToString(bytes));
        assertThat(configuracao.chaveJwt(propriedades).getEncoded()).hasSize(32);
        for (Duration validade : new Duration[] {Duration.ZERO, Duration.ofDays(2), Duration.ofMillis(1)}) {
            propriedades.setValidade(validade);
            assertThatThrownBy(() -> configuracao.chaveJwt(propriedades)).isInstanceOf(IllegalStateException.class);
        }
    }
}
