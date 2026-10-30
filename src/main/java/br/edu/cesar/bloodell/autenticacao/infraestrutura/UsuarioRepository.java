package br.edu.cesar.bloodell.autenticacao.infraestrutura;

import br.edu.cesar.bloodell.autenticacao.dominio.Usuario;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    @Query("select u from Usuario u where lower(trim(u.email)) = :email")
    Optional<Usuario> buscarPorEmail(@Param("email") String email);
}
