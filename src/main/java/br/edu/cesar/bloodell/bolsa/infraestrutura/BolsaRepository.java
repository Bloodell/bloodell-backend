package br.edu.cesar.bloodell.bolsa.infraestrutura;

import br.edu.cesar.bloodell.bolsa.dominio.Bolsa;
import br.edu.cesar.bloodell.bolsa.dominio.EstadoBolsa;
import br.edu.cesar.bloodell.compartilhado.dominio.TipoComponente;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BolsaRepository extends JpaRepository<Bolsa, Long> {

    Optional<Bolsa> findByCodigo(String codigo);

    boolean existsByCodigo(String codigo);

    @Query("""
            select b from Bolsa b
            where (:status is null or b.status = :status)
              and (:componente is null or b.componente = :componente)
              and (:unidadeId is null or b.unidadeAtual.id = :unidadeId)
            order by b.sequenciaEntrada asc
            """)
    Page<Bolsa> buscarComFiltros(@Param("status") EstadoBolsa status,
                                 @Param("componente") TipoComponente componente,
                                 @Param("unidadeId") Long unidadeId,
                                 Pageable paginacao);

    @Query("""
            select b from Bolsa b
            where b.unidadeAtual.id = :unidadeId
            order by b.sequenciaEntrada asc
            """)
    List<Bolsa> listarPorUnidadeNaOrdemDeChegada(@Param("unidadeId") Long unidadeId);

    @Query("select b from Bolsa b order by b.sequenciaEntrada asc")
    List<Bolsa> listarTodasNaOrdemDeChegada();

    @Query("select b from Bolsa b where b.status = :status and b.dataValidade < :data")
    List<Bolsa> listarVencidas(@Param("status") EstadoBolsa status, @Param("data") LocalDate data);

    @Query(value = "select nextval('seq_entrada_bolsa')", nativeQuery = true)
    Long proximaSequenciaDeEntrada();

    @Query("""
            select count(b) from Bolsa b
            where b.unidadeAtual.id = :unidadeId
              and b.componente = :componente
              and b.status = :status
            """)
    long contarDisponiveis(@Param("unidadeId") Long unidadeId,
                           @Param("componente") TipoComponente componente,
                           @Param("status") EstadoBolsa status);
}
