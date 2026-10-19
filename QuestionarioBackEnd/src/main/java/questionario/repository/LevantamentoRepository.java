package questionario.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import questionario.domain.Levantamento;
import questionario.domain.enums.StatusLevantamento;

import java.util.Optional;

public interface LevantamentoRepository extends JpaRepository<Levantamento, Long> {

    Optional<Levantamento> findFirstByStatusOrderByIdDesc(StatusLevantamento status);
}
