package br.edu.cesar.bloodell.veiculo.infraestrutura;

import br.edu.cesar.bloodell.veiculo.dominio.StatusVeiculo;
import br.edu.cesar.bloodell.veiculo.dominio.Veiculo;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VeiculoRepository extends JpaRepository<Veiculo, Long> {

    Optional<Veiculo> findByPlaca(String placa);

    List<Veiculo> findByStatusOrderByPlaca(StatusVeiculo status);

    List<Veiculo> findAllByOrderByPlaca();
}
