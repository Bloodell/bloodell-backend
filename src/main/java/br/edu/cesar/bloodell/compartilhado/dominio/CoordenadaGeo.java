package br.edu.cesar.bloodell.compartilhado.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Objects;

@Embeddable
public class CoordenadaGeo {

    private static final double RAIO_TERRA_KM = 6371.0088;

    @Column(name = "latitude", nullable = false)
    private Double latitude;

    @Column(name = "longitude", nullable = false)
    private Double longitude;

    protected CoordenadaGeo() {
    }

    public CoordenadaGeo(double latitude, double longitude) {
        if (latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException("Latitude fora do intervalo valido: " + latitude);
        }
        if (longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Longitude fora do intervalo valido: " + longitude);
        }
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public double distanciaKm(CoordenadaGeo outra) {
        Objects.requireNonNull(outra, "Coordenada de destino nao informada.");
        double deltaLat = Math.toRadians(outra.latitude - this.latitude);
        double deltaLon = Math.toRadians(outra.longitude - this.longitude);
        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(Math.toRadians(this.latitude)) * Math.cos(Math.toRadians(outra.latitude))
                * Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
        return 2 * RAIO_TERRA_KM * Math.asin(Math.min(1.0, Math.sqrt(a)));
    }

    @Override
    public boolean equals(Object outro) {
        if (this == outro) {
            return true;
        }
        if (!(outro instanceof CoordenadaGeo)) {
            return false;
        }
        CoordenadaGeo coordenada = (CoordenadaGeo) outro;
        return Objects.equals(latitude, coordenada.latitude)
                && Objects.equals(longitude, coordenada.longitude);
    }

    @Override
    public int hashCode() {
        return Objects.hash(latitude, longitude);
    }

    @Override
    public String toString() {
        return "(" + latitude + ", " + longitude + ")";
    }
}
