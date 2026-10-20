package questionario.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Bytes de um {@link Anexo} (mesmo id). Só é lida no download. */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "anexo_conteudo")
public class AnexoConteudo {

    @Id
    private Long anexoId;

    /**
     * {@code longblob} explícito: sem isso o Hibernate cria {@code tinyblob} (255 bytes) no MySQL e qualquer arquivo
     * de verdade estoura com "Data too long". O H2 dos testes não tem esse limite e não acusaria o erro.
     */
    @Lob
    @jakarta.persistence.Column(nullable = false, columnDefinition = "longblob")
    private byte[] dados;

    public AnexoConteudo(Long anexoId, byte[] dados) {
        this.anexoId = anexoId;
        this.dados = dados;
    }
}
