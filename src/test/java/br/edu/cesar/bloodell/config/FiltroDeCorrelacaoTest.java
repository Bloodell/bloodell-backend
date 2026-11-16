package br.edu.cesar.bloodell.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class FiltroDeCorrelacaoTest {
    @Test void preservaValorSeguroETrimado() {
        assertThat(FiltroDeCorrelacao.normalizar("  req-123.A_b  ")).isEqualTo("req-123.A_b");
    }

    @Test void substituiValorAusenteVazioOuLongoPorIdSeguro() {
        for (String entrada : new String[]{null, "", "   ", "x".repeat(41)}) {
            String id = FiltroDeCorrelacao.normalizar(entrada);
            assertThat(id).matches("[a-f0-9]{12}").hasSizeLessThanOrEqualTo(40);
        }
    }

    @Test void substituiCaracteresDeControleESeparadoresNaoPermitidos() {
        for (String entrada : new String[]{"x\r\nInjected: yes", "abc/def", "req\u0000id"}) {
            assertThat(FiltroDeCorrelacao.normalizar(entrada)).matches("[a-f0-9]{12}");
        }
    }
}
