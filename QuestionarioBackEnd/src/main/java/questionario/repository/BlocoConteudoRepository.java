package questionario.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import questionario.domain.BlocoConteudo;
import questionario.domain.Questionario;

import java.util.List;
import java.util.Optional;

public interface BlocoConteudoRepository extends JpaRepository<BlocoConteudo, Long> {

    Optional<BlocoConteudo> findByQuestionarioAndNumero(Questionario questionario, int numero);

    List<BlocoConteudo> findByQuestionario(Questionario questionario);
}
