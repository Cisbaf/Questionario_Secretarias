package questionario.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import questionario.domain.AnexoConteudo;

public interface AnexoConteudoRepository extends JpaRepository<AnexoConteudo, Long> {
}
