package br.edu.cesar.bloodell.aceitacao;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

/**
 * Executa os cenários Gherkin de {@code src/test/resources/aceitacao} pela API real,
 * com PostgreSQL no Testcontainers. O sufixo IT põe a suíte no failsafe, junto dos
 * demais testes de integração: cenário vermelho falha o {@code mvnw verify} e a CI.
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("aceitacao")
@ConfigurationParameter(key = "cucumber.glue", value = "br.edu.cesar.bloodell.aceitacao")
@ConfigurationParameter(key = "cucumber.plugin",
        value = "pretty, html:target/cucumber/aceitacao.html, json:target/cucumber/aceitacao.json")
@ConfigurationParameter(key = "cucumber.publish.quiet", value = "true")
class AceitacaoIT {
}
