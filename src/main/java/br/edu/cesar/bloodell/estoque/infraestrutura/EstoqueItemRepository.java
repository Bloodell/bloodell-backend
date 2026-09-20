package br.edu.cesar.bloodell.estoque.infraestrutura;

import br.edu.cesar.bloodell.compartilhado.dominio.Abo;
import br.edu.cesar.bloodell.compartilhado.dominio.Rh;
import br.edu.cesar.bloodell.compartilhado.dominio.TipoComponente;
import br.edu.cesar.bloodell.estoque.dominio.EstoqueItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EstoqueItemRepository extends JpaRepository<EstoqueItem, Long> {

    @Query("""
            select e from EstoqueItem e
            where (:unidadeId is null or e.unidade.id = :unidadeId)
              and (:componente is null or e.componente = :componente)
            order by e.unidade.nome, e.componente
            """)
    List<EstoqueItem> listarComFiltros(@Param("unidadeId") Long unidadeId,
                                       @Param("componente") TipoComponente componente);

    Optional<EstoqueItem> findByUnidadeIdAndComponenteAndTipoSanguineo_AboAndTipoSanguineo_Rh(
            Long unidadeId, TipoComponente componente, Abo abo, Rh rh);
}
