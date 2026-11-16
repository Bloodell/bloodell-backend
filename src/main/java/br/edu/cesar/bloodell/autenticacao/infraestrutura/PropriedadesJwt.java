package br.edu.cesar.bloodell.autenticacao.infraestrutura;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("bloodell.seguranca.jwt")
public class PropriedadesJwt {
    private String segredoBase64;
    private String emissor = "bloodell";
    private String audiencia = "bloodell-api";
    private Duration validade = Duration.ofMinutes(15);

    public String getSegredoBase64() { return segredoBase64; }
    public void setSegredoBase64(String valor) { segredoBase64 = valor; }
    public String getEmissor() { return emissor; }
    public void setEmissor(String valor) { emissor = valor; }
    public String getAudiencia() { return audiencia; }
    public void setAudiencia(String valor) { audiencia = valor; }
    public Duration getValidade() { return validade; }
    public void setValidade(Duration valor) { validade = valor; }
}
