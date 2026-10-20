package questionario.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import questionario.domain.enums.DocumentoAnexavel;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;

/**
 * Metadados de um arquivo anexado ao item 8.2. Os bytes ficam em {@link AnexoConteudo}, numa tabela à parte, para a
 * lista de anexos nunca puxar os arquivos do banco.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "anexos")
public class Anexo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "questionario_id", nullable = false, updatable = false)
    private Questionario questionario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 40)
    private DocumentoAnexavel documento;

    @Column(nullable = false, updatable = false, length = 150)
    private String nomeOriginal;

    @Column(nullable = false, updatable = false, length = 100)
    private String tipoConteudo;

    @Column(nullable = false, updatable = false)
    private long tamanho;

    @Column(nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    public Anexo(Questionario questionario, DocumentoAnexavel documento, String nomeOriginal, String tipoConteudo, long tamanho) {
        this.questionario = Objects.requireNonNull(questionario);
        this.documento = Objects.requireNonNull(documento);
        this.nomeOriginal = Objects.requireNonNull(nomeOriginal);
        this.tipoConteudo = Objects.requireNonNull(tipoConteudo);
        this.tamanho = tamanho;
        this.criadoEm = LocalDateTime.now(ZoneId.of("America/Sao_Paulo"));
    }
}
