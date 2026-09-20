package br.edu.cesar.bloodell.config;

import br.edu.cesar.bloodell.aed.u2.AlgoritmosDaUnidadeDois;
import br.edu.cesar.bloodell.aed.u2.FefoSeletor;
import br.edu.cesar.bloodell.aed.u2.IndiceEstoqueHash;
import br.edu.cesar.bloodell.aed.u2.MatrizCompatibilidade;
import br.edu.cesar.bloodell.aed.u2.Roteirizador;
import br.edu.cesar.bloodell.alocacao.dominio.AlocacaoProvisoriaU1;
import br.edu.cesar.bloodell.alocacao.dominio.PoliticaDeAlocacao;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConfiguracaoAed {

    @Bean
    public PoliticaDeAlocacao politicaDeAlocacao() {
        return new AlocacaoProvisoriaU1();
    }

    @Bean
    public MatrizCompatibilidade matrizCompatibilidade() {
        return new AlgoritmosDaUnidadeDois.MatrizIndisponivel();
    }

    @Bean
    public FefoSeletor fefoSeletor() {
        return new AlgoritmosDaUnidadeDois.FefoIndisponivel();
    }

    @Bean
    public IndiceEstoqueHash indiceEstoqueHash() {
        return new AlgoritmosDaUnidadeDois.IndiceIndisponivel();
    }

    @Bean
    public Roteirizador roteirizador() {
        return new AlgoritmosDaUnidadeDois.RoteirizadorIndisponivel();
    }
}
