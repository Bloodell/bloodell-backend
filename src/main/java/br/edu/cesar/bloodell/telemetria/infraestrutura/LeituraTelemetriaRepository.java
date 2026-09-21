package br.edu.cesar.bloodell.telemetria.infraestrutura;

import br.edu.cesar.bloodell.telemetria.dominio.LeituraTelemetria;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LeituraTelemetriaRepository extends JpaRepository<LeituraTelemetria, Long> {

    @Query("""
            select l from LeituraTelemetria l
            where l.veiculo.id = :veiculoId
              and (:de is null or l.registradaEm >= :de)
              and (:ate is null or l.registradaEm <= :ate)
            order by l.registradaEm desc
            """)
    List<LeituraTelemetria> listarPorVeiculoNoPeriodo(@Param("veiculoId") Long veiculoId,
                                                      @Param("de") OffsetDateTime de,
                                                      @Param("ate") OffsetDateTime ate);

    long countByVeiculoId(Long veiculoId);

    Optional<LeituraTelemetria> findTopByVeiculoIdOrderByRegistradaEmDesc(Long veiculoId);
}
