package br.edu.cesar.bloodell.unidade.dominio;

import br.edu.cesar.bloodell.compartilhado.dominio.CoordenadaGeo;
import br.edu.cesar.bloodell.compartilhado.dominio.Endereco;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("HOSPITAL")
public class Hospital extends Unidade {

    protected Hospital() {
        super();
    }

    public Hospital(String nome, Endereco endereco, CoordenadaGeo localizacao) {
        super(nome, endereco, localizacao);
    }

    @Override
    public String tipo() {
        return "HOSPITAL";
    }
}
