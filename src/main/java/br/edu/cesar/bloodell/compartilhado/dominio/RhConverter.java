package br.edu.cesar.bloodell.compartilhado.dominio;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class RhConverter implements AttributeConverter<Rh, String> {

    @Override
    public String convertToDatabaseColumn(Rh rh) {
        return rh == null ? null : rh.getSinal();
    }

    @Override
    public Rh convertToEntityAttribute(String sinal) {
        return sinal == null ? null : Rh.doTexto(sinal);
    }
}
