package br.edu.cesar.bloodell.alocacao.dominio;

import br.edu.cesar.bloodell.alocacao.apresentacao.AlocacaoDtos.PassoDaDecisao;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DiarioDaDecisao {

    private final List<PassoDaDecisao> passos = new ArrayList<>();

    public void anotar(String etapa, String detalhe, String resultado) {
        passos.add(new PassoDaDecisao(passos.size() + 1, etapa, detalhe, resultado));
    }

    public List<PassoDaDecisao> getPassos() {
        return Collections.unmodifiableList(passos);
    }
}
