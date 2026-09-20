package br.edu.cesar.bloodell.bolsa.dominio;

import br.edu.cesar.bloodell.compartilhado.dominio.TipoComponente;
import br.edu.cesar.bloodell.doacao.dominio.Doacao;
import java.time.LocalDate;

public final class FabricaDeBolsas {

    private FabricaDeBolsas() {
    }

    public static Bolsa aPartirDaDoacao(Doacao doacao, TipoComponente componente, long sequencia) {
        LocalDate coleta = doacao.getDataHora().toLocalDate();
        LocalDate validade = coleta.plusDays(componente.getValidadeEmDias());
        String codigo = montarCodigo(doacao.getCodigo(), componente, sequencia);
        return new Bolsa(codigo, doacao.getTipoSanguineo(), componente, coleta, validade,
                doacao.getHemocentro(), doacao.getId(), sequencia);
    }

    private static String montarCodigo(String codigoDoacao, TipoComponente componente, long sequencia) {
        String sufixo = switch (componente) {
            case CONCENTRADO_HEMACIAS -> "HEM";
            case PLASMA -> "PLA";
            case PLAQUETAS -> "PLQ";
        };
        String numero = codigoDoacao.replace("DOA-", "");
        return "BL-" + numero + "-" + sufixo + "-" + sequencia;
    }
}
