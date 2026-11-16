package br.edu.cesar.bloodell.bolsa;

import static org.assertj.core.api.Assertions.assertThat;

import br.edu.cesar.bloodell.bolsa.dominio.Bolsa;
import br.edu.cesar.bloodell.bolsa.dominio.EstadoBolsa;
import br.edu.cesar.bloodell.bolsa.dominio.FabricaDeBolsas;
import br.edu.cesar.bloodell.compartilhado.dominio.CoordenadaGeo;
import br.edu.cesar.bloodell.compartilhado.dominio.Endereco;
import br.edu.cesar.bloodell.compartilhado.dominio.TipoComponente;
import br.edu.cesar.bloodell.compartilhado.dominio.TipoSanguineo;
import br.edu.cesar.bloodell.doacao.dominio.Doacao;
import br.edu.cesar.bloodell.unidade.dominio.Hemocentro;
import br.edu.cesar.bloodell.unidade.dominio.Unidade;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class FabricaDeBolsasTest {

    private static final OffsetDateTime COLETA = OffsetDateTime.of(2026, 9, 19, 10, 30, 0, 0, ZoneOffset.ofHours(-3));

    private final Unidade hemocentro = new Hemocentro("Hemocentro de Teste",
            new Endereco("Rua Sintetica", "1", "Centro", "Recife", "PE", "50000-000"),
            new CoordenadaGeo(-8.0578, -34.8829));

    private final Doacao doacao = new Doacao("DOA-000007", hemocentro, COLETA,
            TipoSanguineo.doTextoCompacto("AB-"), 450);

    @ParameterizedTest(name = "{0} vence {1} dias depois da coleta")
    @CsvSource({"CONCENTRADO_HEMACIAS, 42", "PLASMA, 365", "PLAQUETAS, 5"})
    @DisplayName("a validade sai da regra do componente, contada a partir da data da coleta")
    void validadePorComponente(TipoComponente componente, int dias) {
        Bolsa bolsa = FabricaDeBolsas.aPartirDaDoacao(doacao, componente, 1L);

        assertThat(bolsa.getDataColeta()).isEqualTo(LocalDate.of(2026, 9, 19));
        assertThat(bolsa.getDataValidade()).isEqualTo(LocalDate.of(2026, 9, 19).plusDays(dias));
    }

    @Test
    @DisplayName("a bolsa herda tipo, hemocentro e sequencia, e nasce RECEBIDA")
    void herdaDadosDaDoacao() {
        Bolsa bolsa = FabricaDeBolsas.aPartirDaDoacao(doacao, TipoComponente.PLASMA, 12L);

        assertThat(bolsa.getTipoSanguineo().formatado()).isEqualTo("AB-");
        assertThat(bolsa.getUnidadeAtual()).isSameAs(hemocentro);
        assertThat(bolsa.getSequenciaEntrada()).isEqualTo(12L);
        assertThat(bolsa.getStatus()).isEqualTo(EstadoBolsa.RECEBIDA);
    }

    @Test
    @DisplayName("o codigo junta o numero da doacao, a sigla do componente e a sequencia")
    void codigoRastreavel() {
        assertThat(FabricaDeBolsas.aPartirDaDoacao(doacao, TipoComponente.CONCENTRADO_HEMACIAS, 3L).getCodigo())
                .isEqualTo("BL-000007-HEM-3");
        assertThat(FabricaDeBolsas.aPartirDaDoacao(doacao, TipoComponente.PLASMA, 4L).getCodigo())
                .isEqualTo("BL-000007-PLA-4");
        assertThat(FabricaDeBolsas.aPartirDaDoacao(doacao, TipoComponente.PLAQUETAS, 5L).getCodigo())
                .isEqualTo("BL-000007-PLQ-5");
    }
}
