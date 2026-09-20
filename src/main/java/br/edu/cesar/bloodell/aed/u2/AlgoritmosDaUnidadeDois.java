package br.edu.cesar.bloodell.aed.u2;

import br.edu.cesar.bloodell.aed.contratos.BolsaAed;
import br.edu.cesar.bloodell.aed.u1.ListaEstoque;

public final class AlgoritmosDaUnidadeDois {

    public static final String MENSAGEM =
            "algoritmo da Unidade 2 -- ainda nao implementado (restricao academica R4 do blueprint)";

    private AlgoritmosDaUnidadeDois() {
    }

    public static final class MatrizIndisponivel implements MatrizCompatibilidade {
        @Override
        public boolean podeReceber(String tipoReceptor, String tipoDoador) {
            throw new UnsupportedOperationException(MENSAGEM);
        }
    }

    public static final class FefoIndisponivel implements FefoSeletor {
        @Override
        public BolsaAed[] ordenarPorValidade(BolsaAed[] elegiveis) {
            throw new UnsupportedOperationException(MENSAGEM);
        }
    }

    public static final class IndiceIndisponivel implements IndiceEstoqueHash {
        @Override
        public void inserir(String chave, ListaEstoque bolsas) {
            throw new UnsupportedOperationException(MENSAGEM);
        }

        @Override
        public ListaEstoque buscar(String chave) {
            throw new UnsupportedOperationException(MENSAGEM);
        }

        @Override
        public int colisoes() {
            throw new UnsupportedOperationException(MENSAGEM);
        }
    }

    public static final class RoteirizadorIndisponivel implements Roteirizador {
        @Override
        public int[] calcularOrdemDeVisita(double latitudeOrigem, double longitudeOrigem,
                                           double[] latitudesDestinos, double[] longitudesDestinos) {
            throw new UnsupportedOperationException(MENSAGEM);
        }
    }
}
