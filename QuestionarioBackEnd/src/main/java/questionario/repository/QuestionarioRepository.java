package questionario.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import questionario.domain.Questionario;

import java.util.Optional;

public interface QuestionarioRepository extends JpaRepository<Questionario, Long> {

    Optional<Questionario> findByPublicId(String publicId);
}
