package br.edu.cesar.bloodell.compartilhado.apresentacao;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.OffsetDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProblemaApi(
        String type,
        String title,
        int status,
        String detail,
        String instance,
        OffsetDateTime momento,
        String correlacao,
        List<ErroDeCampo> campos) {

    public static final String BASE_TIPOS = "https://bloodell.cesar.school/erros/";

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ErroDeCampo(String campo, String mensagem) {
    }

    public static ProblemaApi de(String tipo, String titulo, int status, String detalhe,
                                 String caminho, String correlacao, List<ErroDeCampo> campos) {
        return new ProblemaApi(BASE_TIPOS + tipo, titulo, status, detalhe, caminho,
                OffsetDateTime.now(), correlacao, campos);
    }
}
