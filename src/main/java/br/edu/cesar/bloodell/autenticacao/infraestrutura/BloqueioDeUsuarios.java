package br.edu.cesar.bloodell.autenticacao.infraestrutura;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class BloqueioDeUsuarios {
    private final JdbcTemplate jdbc;

    public BloqueioDeUsuarios(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    /**
     * Deve ser chamado dentro da transacao, antes de consultar as contas.
     * Serializa PATCHs e coordena com INSERTs, sem bloquear SELECTs comuns.
     */
    public void bloquearEscritas() {
        jdbc.execute("LOCK TABLE usuarios IN SHARE ROW EXCLUSIVE MODE");
    }
}
