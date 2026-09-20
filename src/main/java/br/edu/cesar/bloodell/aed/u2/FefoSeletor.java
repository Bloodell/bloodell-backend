package br.edu.cesar.bloodell.aed.u2;

import br.edu.cesar.bloodell.aed.contratos.BolsaAed;

public interface FefoSeletor {

    BolsaAed[] ordenarPorValidade(BolsaAed[] elegiveis);
}
