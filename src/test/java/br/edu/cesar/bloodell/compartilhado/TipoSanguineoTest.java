package br.edu.cesar.bloodell.compartilhado;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.edu.cesar.bloodell.compartilhado.dominio.Abo;
import br.edu.cesar.bloodell.compartilhado.dominio.CoordenadaGeo;
import br.edu.cesar.bloodell.compartilhado.dominio.Rh;
import br.edu.cesar.bloodell.compartilhado.dominio.TipoSanguineo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class TipoSanguineoTest {

    @ParameterizedTest
    @ValueSource(strings = {"A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"})
    @DisplayName("aceita os oito tipos validos na forma compacta")
    void aceitaOsOitoTipos(String texto) {
        assertThat(TipoSanguineo.doTextoCompacto(texto).formatado()).isEqualTo(texto);
    }

    @ParameterizedTest
    @ValueSource(strings = {"C+", "A", "+", "AB", "O*", "XYZ"})
    @DisplayName("recusa tipo invalido")
    void recusaTipoInvalido(String texto) {
        assertThatThrownBy(() -> TipoSanguineo.doTextoCompacto(texto))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("dois tipos iguais sao iguais (equals e hashCode escritos a mao)")
    void igualdadeDeValueObject() {
        TipoSanguineo primeiro = new TipoSanguineo(Abo.O, Rh.NEGATIVO);
        TipoSanguineo segundo = TipoSanguineo.doTextoCompacto("O-");

        assertThat(primeiro).isEqualTo(segundo);
        assertThat(primeiro.hashCode()).isEqualTo(segundo.hashCode());
    }

    @Test
    @DisplayName("distancia entre coordenadas confere com a referencia conhecida")
    void distanciaEntreCoordenadas() {
        CoordenadaGeo recife = new CoordenadaGeo(-8.0578, -34.8829);
        CoordenadaGeo jaboatao = new CoordenadaGeo(-8.1128, -35.0150);

        assertThat(recife.distanciaKm(jaboatao)).isBetween(14.0, 17.0);
        assertThat(recife.distanciaKm(recife)).isZero();
    }

    @Test
    @DisplayName("coordenada fora do intervalo do planeta e recusada")
    void coordenadaInvalida() {
        assertThatThrownBy(() -> new CoordenadaGeo(-100.0, 0.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Latitude");
    }
}
