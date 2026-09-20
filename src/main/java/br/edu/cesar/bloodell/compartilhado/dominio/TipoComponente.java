package br.edu.cesar.bloodell.compartilhado.dominio;

public enum TipoComponente {

    CONCENTRADO_HEMACIAS("Concentrado de hemacias", 42, new FaixaTemperatura(2.0, 6.0)),
    PLASMA("Plasma fresco congelado", 365, new FaixaTemperatura(-30.0, -18.0)),
    PLAQUETAS("Concentrado de plaquetas", 5, new FaixaTemperatura(20.0, 24.0));

    private final String descricao;
    private final int validadeEmDias;
    private final FaixaTemperatura faixaConservacao;

    TipoComponente(String descricao, int validadeEmDias, FaixaTemperatura faixaConservacao) {
        this.descricao = descricao;
        this.validadeEmDias = validadeEmDias;
        this.faixaConservacao = faixaConservacao;
    }

    public String getDescricao() {
        return descricao;
    }

    public int getValidadeEmDias() {
        return validadeEmDias;
    }

    public FaixaTemperatura getFaixaConservacao() {
        return faixaConservacao;
    }
}
