package questionario.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import questionario.domain.Bloco1Identificacao;
import questionario.domain.Questionario;

import java.util.Optional;

public interface Bloco1IdentificacaoRepository extends JpaRepository<Bloco1Identificacao, Long> {

    Optional<Bloco1Identificacao> findByQuestionario(Questionario questionario);
}
