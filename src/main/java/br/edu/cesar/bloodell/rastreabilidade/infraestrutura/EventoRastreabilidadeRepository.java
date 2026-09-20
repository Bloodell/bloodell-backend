package br.edu.cesar.bloodell.rastreabilidade.infraestrutura;

import br.edu.cesar.bloodell.rastreabilidade.dominio.EventoRastreabilidade;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventoRastreabilidadeRepository extends JpaRepository<EventoRastreabilidade, Long> {

    List<EventoRastreabilidade> findByBolsaIdOrderByOcorridoEmAscIdAsc(Long bolsaId);

    @Query("select e from EventoRastreabilidade e order by e.ocorridoEm asc, e.id asc")
    List<EventoRastreabilidade> listarNaOrdemCronologica();

    @Query("""
            select e from EventoRastreabilidade e
            where e.bolsa.id = :bolsaId
            order by e.ocorridoEm desc, e.id desc
            """)
    List<EventoRastreabilidade> listarDoMaisRecente(@Param("bolsaId") Long bolsaId);
}
