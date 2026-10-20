package questionario.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import questionario.domain.Anexo;
import questionario.domain.Questionario;
import questionario.domain.enums.DocumentoAnexavel;

import java.util.List;
import java.util.Optional;

public interface AnexoRepository extends JpaRepository<Anexo, Long> {

    List<Anexo> findByQuestionarioOrderByIdAsc(Questionario questionario);

    long countByQuestionarioAndDocumento(Questionario questionario, DocumentoAnexavel documento);

    Optional<Anexo> findByIdAndQuestionario(Long id, Questionario questionario);
}
