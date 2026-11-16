package br.edu.cesar.bloodell.bolsa.aplicacao;

import br.edu.cesar.bloodell.autenticacao.aplicacao.AcessoUnidade;
import br.edu.cesar.bloodell.bolsa.apresentacao.BolsaDtos.BolsaResposta;
import br.edu.cesar.bloodell.bolsa.apresentacao.BolsaDtos.EventoResposta;
import br.edu.cesar.bloodell.bolsa.dominio.Bolsa;
import br.edu.cesar.bloodell.bolsa.dominio.EstadoBolsa;
import br.edu.cesar.bloodell.bolsa.infraestrutura.BolsaRepository;
import br.edu.cesar.bloodell.compartilhado.dominio.TipoComponente;
import br.edu.cesar.bloodell.compartilhado.excecao.RecursoNaoEncontradoException;
import br.edu.cesar.bloodell.rastreabilidade.dominio.EventoRastreabilidade;
import br.edu.cesar.bloodell.rastreabilidade.infraestrutura.EventoRastreabilidadeRepository;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BolsaService {

    private final AcessoUnidade acesso;

    private final BolsaRepository bolsas;
    private final EventoRastreabilidadeRepository eventos;

    public BolsaService(BolsaRepository bolsas, EventoRastreabilidadeRepository eventos, AcessoUnidade acesso) {
        this.acesso = acesso;
        this.bolsas = bolsas;
        this.eventos = eventos;
    }

    @Transactional(readOnly = true)
    public Page<BolsaResposta> listar(EstadoBolsa status, TipoComponente componente,
                                      Long unidadeId, Pageable paginacao) {
        LocalDate hoje = LocalDate.now();
        var escopo = acesso.escopo();
        if (unidadeId != null) escopo.exigir(unidadeId);
        org.springframework.data.jpa.domain.Specification<Bolsa> filtro = escopo.especificacao("unidadeAtual.id");
        filtro = filtro.and((r, q, cb) -> {
            var condicoes = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (status != null) condicoes.add(cb.equal(r.get("status"), status));
            if (componente != null) condicoes.add(cb.equal(r.get("componente"), componente));
            if (unidadeId != null) condicoes.add(cb.equal(r.get("unidadeAtual").get("id"), unidadeId));
            q.orderBy(cb.asc(r.get("sequenciaEntrada")));
            return cb.and(condicoes.toArray(jakarta.persistence.criteria.Predicate[]::new));
        });
        return bolsas.findAll(filtro, paginacao)
                .map(bolsa -> paraResposta(bolsa, hoje));
    }

    @Transactional(readOnly = true)
    public BolsaResposta detalhar(Long id) {
        Bolsa bolsa = bolsas.findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Bolsa", id));
        return paraResposta(acesso.exigirRecurso(bolsa), LocalDate.now());
    }

    @Transactional(readOnly = true)
    public List<EventoResposta> rastreabilidade(Long bolsaId) {
        acesso.exigirRecurso(bolsas.findById(bolsaId)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Bolsa", bolsaId)));
        List<EventoRastreabilidade> registrados = acesso.filtrar(eventos.findByBolsaIdOrderByOcorridoEmAscIdAsc(bolsaId));
        return registrados.stream()
                .map(evento -> new EventoResposta(
                    evento.getId(),
                    evento.getStatusAnterior() == null ? null : evento.getStatusAnterior().name(),
                    evento.getStatusNovo().name(),
                    evento.getDescricao(),
                    evento.getOcorridoEm()))
                .toList();
    }

    public static BolsaResposta paraResposta(Bolsa bolsa, LocalDate hoje) {
        int dias = (int) ChronoUnit.DAYS.between(hoje, bolsa.getDataValidade());
        return new BolsaResposta(
                bolsa.getId(),
                bolsa.getCodigo(),
                bolsa.getTipoSanguineo().formatado(),
                bolsa.getComponente().name(),
                bolsa.getDataColeta().toString(),
                bolsa.getDataValidade().toString(),
                bolsa.getStatus().name(),
                bolsa.getUnidadeAtual().getId(),
                bolsa.getUnidadeAtual().getNome(),
                bolsa.getSequenciaEntrada(),
                bolsa.estaDentroDaValidade(hoje),
                dias);
    }
}
