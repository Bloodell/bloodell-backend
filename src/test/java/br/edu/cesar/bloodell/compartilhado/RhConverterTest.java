package br.edu.cesar.bloodell.compartilhado;

import static org.assertj.core.api.Assertions.assertThat;

import br.edu.cesar.bloodell.compartilhado.dominio.Rh;
import br.edu.cesar.bloodell.compartilhado.dominio.RhConverter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class RhConverterTest {

    private final RhConverter converter = new RhConverter();

    @ParameterizedTest
    @CsvSource({"POSITIVO,+", "NEGATIVO,-"})
    void converteEntreEnumESinalDoSchema(Rh rh, String sinal) {
        assertThat(converter.convertToDatabaseColumn(rh)).isEqualTo(sinal);
        assertThat(converter.convertToEntityAttribute(sinal)).isEqualTo(rh);
    }

    @Test
    void preservaNulos() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }
}
