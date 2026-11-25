package br.edu.cesar.bloodell.autenticacao.aplicacao;

import br.edu.cesar.bloodell.autenticacao.apresentacao.AutenticacaoDtos.UsuarioAtual;
import br.edu.cesar.bloodell.autenticacao.dominio.Perfil;
import br.edu.cesar.bloodell.autenticacao.dominio.Usuario;
import br.edu.cesar.bloodell.autenticacao.infraestrutura.UsuarioRepository;
import br.edu.cesar.bloodell.autenticacao.infraestrutura.VinculoDeUnidadeRepository;
import br.edu.cesar.bloodell.config.PropriedadesBloodell;
import br.edu.cesar.bloodell.bolsa.dominio.Bolsa;
import br.edu.cesar.bloodell.doacao.dominio.Doacao;
import br.edu.cesar.bloodell.entrega.dominio.Entrega;
import br.edu.cesar.bloodell.estoque.dominio.EstoqueItem;
import br.edu.cesar.bloodell.requisicao.dominio.Requisicao;
import br.edu.cesar.bloodell.unidade.dominio.Unidade;
import br.edu.cesar.bloodell.veiculo.dominio.Veiculo;
import br.edu.cesar.bloodell.alerta.dominio.Alerta;
import br.edu.cesar.bloodell.rastreabilidade.dominio.EventoRastreabilidade;
import br.edu.cesar.bloodell.telemetria.dominio.LeituraTelemetria;
import jakarta.persistence.EntityManager;
import java.util.Arrays;
import java.util.Set;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.domain.Specification;

/** Fronteira comum para APIs e chamadas entre servicos. JWT nao transporta vinculos. */
@Service
@Transactional(readOnly = true)
public class AcessoUnidade {
    private final PropriedadesBloodell propriedades;
    private final UsuarioRepository usuarios;
    private final VinculoDeUnidadeRepository vinculos;
    private final EntityManager entidades;

    public AcessoUnidade(PropriedadesBloodell propriedades, UsuarioRepository usuarios,
                        VinculoDeUnidadeRepository vinculos, EntityManager entidades) {
        this.propriedades = propriedades;
        this.usuarios = usuarios;
        this.vinculos = vinculos;
        this.entidades = entidades;
    }

    private Usuario usuario() {
        var autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacao == null || !autenticacao.isAuthenticated()
                || !(autenticacao.getPrincipal() instanceof UsuarioAtual atual)) throw negado();
        return usuarios.findById(atual.id()).filter(u -> Usuario.hashBcryptValido(u.getSenhaHash()))
                .orElseThrow(AcessoUnidade::negado);
    }

    public void exigirAdministrador() {
        if (!propriedades.getSeguranca().isAutenticacaoHabilitada()) return;
        if (usuario().getPerfil() != Perfil.ADMIN) throw negado();
    }

    public Escopo escopo() {
        // Preserva somente o modo demonstrativo explicitamente configurado.
        if (!propriedades.getSeguranca().isAutenticacaoHabilitada()) return new Escopo(true, Set.of());
        Usuario usuario = usuario();
        return new Escopo(usuario.getPerfil() == Perfil.ADMIN, vinculos.unidadesDoUsuario(usuario.getId()));
    }

    public void exigir(Long... unidades) { escopo().exigir(unidades); }

    public <T> T exigirRecurso(T recurso) {
        if (!podeVer(recurso, escopo())) throw negado();
        return recurso;
    }

    public <T> List<T> filtrar(List<T> recursos) {
        Escopo escopo = escopo();
        return recursos.stream().filter(recurso -> podeVer(recurso, escopo)).toList();
    }

    private boolean referencia(Class<?> tipo, Long id, Escopo escopo) {
        return id == null || podeVer(entidades.find(tipo, id), escopo);
    }

    private boolean podeVer(Object recurso, Escopo escopo) {
        if (recurso == null) return false;
        if (escopo.global()) return true;
        if (recurso instanceof Unidade u) return escopo.permite(u.getId());
        if (recurso instanceof Requisicao r) return escopo.permite(r.getHospital().getId());
        if (recurso instanceof Bolsa b) return escopo.permite(b.getUnidadeAtual().getId());
        if (recurso instanceof Doacao d) return escopo.permite(d.getHemocentro().getId());
        if (recurso instanceof EstoqueItem i) return escopo.permite(i.getUnidade().getId());
        if (recurso instanceof Veiculo v) return escopo.permite(v.getHemocentroBase().getId());
        if (recurso instanceof Entrega e) return e.getVeiculo() != null && escopo.permite(e.getOrigem().getId(),
                e.getRequisicao().getHospital().getId(), e.getVeiculo().getHemocentroBase().getId());
        if (recurso instanceof LeituraTelemetria l) return podeVer(l.getVeiculo(), escopo)
                && referencia(Entrega.class, l.getEntregaId(), escopo);
        if (recurso instanceof EventoRastreabilidade e) return podeVer(e.getBolsa(), escopo)
                && referencia(Requisicao.class, e.getRequisicaoId(), escopo)
                && referencia(Entrega.class, e.getEntregaId(), escopo);
        if (recurso instanceof Alerta a) return (a.getVeiculoId() != null || a.getEntregaId() != null || a.getBolsaId() != null)
                && referencia(Veiculo.class, a.getVeiculoId(), escopo)
                && referencia(Entrega.class, a.getEntregaId(), escopo)
                && referencia(Bolsa.class, a.getBolsaId(), escopo);
        throw new IllegalArgumentException("Recurso sem politica de unidade: " + recurso.getClass().getName());
    }

    public static AccessDeniedException negado() {
        return new AccessDeniedException("Acesso a unidade nao autorizado.");
    }

    public record Escopo(boolean global, Set<Long> unidades) {
        public Escopo { unidades = Set.copyOf(unidades); }
        public boolean permite(Long... ids) {
            return global || (ids.length > 0 && Arrays.stream(ids).allMatch(id -> id != null && unidades.contains(id)));
        }
        public void exigir(Long... ids) { if (!permite(ids)) throw negado(); }
        public <T> Specification<T> especificacao(String caminho) {
            return (raiz, consulta, cb) -> {
                if (global) return cb.conjunction();
                if (unidades.isEmpty()) return cb.disjunction();
                jakarta.persistence.criteria.Path<?> campo = raiz;
                for (String parte : caminho.split("\\.")) campo = campo.get(parte);
                return campo.in(unidades);
            };
        }
    }
}
