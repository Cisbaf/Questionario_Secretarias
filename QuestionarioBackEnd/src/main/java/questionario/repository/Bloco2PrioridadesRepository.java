package questionario.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import questionario.domain.Bloco2Prioridades;
import questionario.domain.Questionario;

import java.util.Optional;

public interface Bloco2PrioridadesRepository extends JpaRepository<Bloco2Prioridades, Long> {

    Optional<Bloco2Prioridades> findByQuestionario(Questionario questionario);
}
