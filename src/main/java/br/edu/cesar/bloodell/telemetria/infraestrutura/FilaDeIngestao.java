package br.edu.cesar.bloodell.telemetria.infraestrutura;

import br.edu.cesar.bloodell.config.PropriedadesBloodell;
import br.edu.cesar.bloodell.telemetria.apresentacao.TelemetriaDtos.LeituraEntrada;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

@Component
public class FilaDeIngestao {

    public record LeituraEmTransito(LeituraEntrada leitura, String correlacao) {
    }

    private final BlockingQueue<LeituraEmTransito> fila;
    private final int capacidade;

    public FilaDeIngestao(PropriedadesBloodell propriedades) {
        this.capacidade = propriedades.getTelemetria().getCapacidadeFila();
        this.fila = new ArrayBlockingQueue<>(capacidade);
    }

    public boolean oferecer(LeituraEmTransito item) {
        return fila.offer(item);
    }

    public int drenarLote(List<LeituraEmTransito> destino, int tamanhoMaximo, long esperaMs)
            throws InterruptedException {
        LeituraEmTransito primeiro = fila.poll(esperaMs, TimeUnit.MILLISECONDS);
        if (primeiro == null) {
            return 0;
        }
        destino.add(primeiro);
        return 1 + fila.drainTo(destino, tamanhoMaximo - 1);
    }

    public int tamanhoAtual() {
        return fila.size();
    }

    public int getCapacidade() {
        return capacidade;
    }
}
