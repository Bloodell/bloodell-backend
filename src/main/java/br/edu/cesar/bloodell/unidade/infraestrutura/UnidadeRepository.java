package br.edu.cesar.bloodell.unidade.infraestrutura;

import br.edu.cesar.bloodell.unidade.dominio.Unidade;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UnidadeRepository extends JpaRepository<Unidade, Long> {

    Optional<Unidade> findByNomeIgnoreCase(String nome);

    @Query("select u from Unidade u where type(u) = br.edu.cesar.bloodell.unidade.dominio.Hospital order by u.nome")
    List<Unidade> listarHospitais();

    @Query("select u from Unidade u where type(u) = br.edu.cesar.bloodell.unidade.dominio.Hemocentro order by u.nome")
    List<Unidade> listarHemocentros();
}
