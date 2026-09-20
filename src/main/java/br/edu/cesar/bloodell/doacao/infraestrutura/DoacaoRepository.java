package br.edu.cesar.bloodell.doacao.infraestrutura;

import br.edu.cesar.bloodell.doacao.dominio.Doacao;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DoacaoRepository extends JpaRepository<Doacao, Long> {

    Optional<Doacao> findByCodigo(String codigo);

    boolean existsByCodigo(String codigo);
}
