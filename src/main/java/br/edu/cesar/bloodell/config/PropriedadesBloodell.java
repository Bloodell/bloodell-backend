package br.edu.cesar.bloodell.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "bloodell")
public class PropriedadesBloodell {

    private final Cors cors = new Cors();
    private final Telemetria telemetria = new Telemetria();
    private final Demonstracao demonstracao = new Demonstracao();
    private final Seguranca seguranca = new Seguranca();

    public Cors getCors() {
        return cors;
    }

    public Telemetria getTelemetria() {
        return telemetria;
    }

    public Demonstracao getDemonstracao() {
        return demonstracao;
    }

    public Seguranca getSeguranca() {
        return seguranca;
    }

    public static class Cors {
        private String origens = "http://127.0.0.1:5173";

        public String getOrigens() {
            return origens;
        }

        public void setOrigens(String origens) {
            this.origens = origens;
        }

        public String[] origensComoVetor() {
            return origens.split("\\s*,\\s*");
        }
    }

    public static class Telemetria {
        private int capacidadeFila = 10000;
        private int quantidadeWorkers = 4;
        private int tamanhoLote = 100;
        private long timeoutComunicacaoSegundos = 60;

        public int getCapacidadeFila() {
            return capacidadeFila;
        }

        public void setCapacidadeFila(int capacidadeFila) {
            this.capacidadeFila = capacidadeFila;
        }

        public int getQuantidadeWorkers() {
            return quantidadeWorkers;
        }

        public void setQuantidadeWorkers(int quantidadeWorkers) {
            this.quantidadeWorkers = quantidadeWorkers;
        }

        public int getTamanhoLote() {
            return tamanhoLote;
        }

        public void setTamanhoLote(int tamanhoLote) {
            this.tamanhoLote = tamanhoLote;
        }

        public long getTimeoutComunicacaoSegundos() {
            return timeoutComunicacaoSegundos;
        }

        public void setTimeoutComunicacaoSegundos(long timeoutComunicacaoSegundos) {
            this.timeoutComunicacaoSegundos = timeoutComunicacaoSegundos;
        }
    }

    public static class Demonstracao {
        private boolean habilitado = true;

        public boolean isHabilitado() {
            return habilitado;
        }

        public void setHabilitado(boolean habilitado) {
            this.habilitado = habilitado;
        }
    }

    public static class Seguranca {
        private boolean autenticacaoHabilitada = false;

        public boolean isAutenticacaoHabilitada() {
            return autenticacaoHabilitada;
        }

        public void setAutenticacaoHabilitada(boolean autenticacaoHabilitada) {
            this.autenticacaoHabilitada = autenticacaoHabilitada;
        }
    }
}
