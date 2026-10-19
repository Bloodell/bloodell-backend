package br.edu.cesar.bloodell.entrega.infraestrutura;

import br.edu.cesar.bloodell.entrega.dominio.Entrega;
import br.edu.cesar.bloodell.entrega.dominio.StatusEntrega;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EntregaRepository extends JpaRepository<Entrega, Long> {

    List<Entrega> findByStatusOrderByIdDesc(StatusEntrega status);

    boolean existsByRequisicaoIdAndStatusIn(Long requisicaoId, List<StatusEntrega> status);
}
