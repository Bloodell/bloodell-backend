package br.edu.cesar.bloodell.autenticacao.infraestrutura;

import java.util.Set;
import java.util.HashSet;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class VinculoDeUnidadeRepository {
    private final JdbcTemplate jdbc;

    public VinculoDeUnidadeRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public Set<Long> unidadesDoUsuario(Long usuarioId) {
        return new HashSet<>(jdbc.queryForList(
                "SELECT unidade_id FROM usuario_unidades WHERE usuario_id = ?", Long.class, usuarioId));
    }

    /** Chamado na transacao administrativa, depois do bloqueio de escritas. */
    public void substituir(Long usuarioId, Set<Long> unidades) {
        jdbc.update("DELETE FROM usuario_unidades WHERE usuario_id = ?", usuarioId);
        for (Long unidade : unidades) {
            jdbc.update("INSERT INTO usuario_unidades(usuario_id, unidade_id) VALUES (?, ?)", usuarioId, unidade);
        }
    }
}
