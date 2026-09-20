package br.edu.cesar.bloodell.requisicao.infraestrutura;

import br.edu.cesar.bloodell.requisicao.dominio.Prioridade;
import br.edu.cesar.bloodell.requisicao.dominio.Requisicao;
import br.edu.cesar.bloodell.requisicao.dominio.StatusRequisicao;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RequisicaoRepository extends JpaRepository<Requisicao, Long> {

    Optional<Requisicao> findByCodigo(String codigo);

    @Query("""
            select r from Requisicao r
            where (:status is null or r.status = :status)
              and (:prioridade is null or r.prioridade = :prioridade)
              and (:hospitalId is null or r.hospital.id = :hospitalId)
            order by r.criadaEm asc
            """)
    Page<Requisicao> buscarComFiltros(@Param("status") StatusRequisicao status,
                                      @Param("prioridade") Prioridade prioridade,
                                      @Param("hospitalId") Long hospitalId,
                                      Pageable paginacao);

    @Query("select r from Requisicao r where r.status = :status order by r.criadaEm asc, r.id asc")
    List<Requisicao> listarNaOrdemDeChegada(@Param("status") StatusRequisicao status);

    long countByStatus(StatusRequisicao status);
}
