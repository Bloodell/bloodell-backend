package br.edu.cesar.bloodell.unidade.dominio;

import br.edu.cesar.bloodell.compartilhado.dominio.CoordenadaGeo;
import br.edu.cesar.bloodell.compartilhado.dominio.Endereco;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("HEMOCENTRO")
public class Hemocentro extends Unidade {

    protected Hemocentro() {
        super();
    }

    public Hemocentro(String nome, Endereco endereco, CoordenadaGeo localizacao) {
        super(nome, endereco, localizacao);
    }

    @Override
    public String tipo() {
        return "HEMOCENTRO";
    }
}
