package br.edu.cesar.bloodell.aceitacao;

import br.edu.cesar.bloodell.suporte.TesteDeIntegracao;
import io.cucumber.java.Before;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Mesmo contexto Spring e mesmo PostgreSQL dos demais testes de integração. Cada
 * cenário começa com o banco vazio, para que um não dependa do que o outro gravou.
 */
@CucumberContextConfiguration
@AutoConfigureTestRestTemplate
public class ConfiguracaoDaAceitacao extends TesteDeIntegracao {

    @Autowired
    private JdbcTemplate jdbc;

    @Before
    public void bancoVazio() {
        limparBanco(jdbc);
    }
}
