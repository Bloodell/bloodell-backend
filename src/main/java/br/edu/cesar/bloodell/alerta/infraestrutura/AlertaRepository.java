package br.edu.cesar.bloodell.alerta.infraestrutura;

import br.edu.cesar.bloodell.alerta.dominio.Alerta;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AlertaRepository extends JpaRepository<Alerta, Long> {

    @Query("select a from Alerta a where a.resolvidoEm is null order by a.criadoEm desc")
    List<Alerta> listarAbertos();

    boolean existsByTipoAndVeiculoIdAndResolvidoEmIsNull(String tipo, Long veiculoId);
}
