package br.edu.cesar.bloodell.rastreabilidade.aplicacao;

import br.edu.cesar.bloodell.bolsa.dominio.Bolsa;
import br.edu.cesar.bloodell.bolsa.dominio.EstadoBolsa;
import br.edu.cesar.bloodell.rastreabilidade.dominio.EventoRastreabilidade;
import br.edu.cesar.bloodell.rastreabilidade.infraestrutura.EventoRastreabilidadeRepository;
import org.springframework.stereotype.Component;

@Component
public class RegistradorDeEventos {

    private final EventoRastreabilidadeRepository eventos;

    public RegistradorDeEventos(EventoRastreabilidadeRepository eventos) {
        this.eventos = eventos;
    }

    public EventoRastreabilidade registrar(Bolsa bolsa, EstadoBolsa anterior, EstadoBolsa novo,
                                           Long requisicaoId, String descricao) {
        return eventos.save(new EventoRastreabilidade(bolsa, anterior, novo, requisicaoId, descricao));
    }
}
