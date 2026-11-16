package br.edu.cesar.bloodell.autenticacao.infraestrutura;

import br.edu.cesar.bloodell.autenticacao.dominio.Usuario;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CadastroDeUsuarioRepository {
    private final JdbcTemplate jdbc;

    public CadastroDeUsuarioRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    /**
     * Usa a unicidade da V3 de forma atomica. ON CONFLICT evita excecoes SQL cujo
     * DETAIL poderia registrar os dados da linha, incluindo o hash da senha.
     */
    public Optional<Long> cadastrarSeEmailDisponivel(Usuario usuario) {
        return jdbc.query("""
                INSERT INTO usuarios (nome, email, perfil, senha_hash)
                VALUES (?, ?, ?, ?)
                ON CONFLICT (lower(btrim(email))) DO NOTHING
                RETURNING id
                """, (resultado, linha) -> resultado.getLong("id"),
                usuario.getNome(), usuario.getEmail(), usuario.getPerfil().name(), usuario.getSenhaHash())
                .stream().findFirst();
    }
}
