package questionario.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;

/**
 * Conteúdo de um dos Blocos 3 a 8, guardado como JSON em uma linha só por (questionário, bloco). Escolha de projeto: as
 * tabelas desses blocos são muitas, largas e mudam de forma com o documento; em JSON, acrescentar uma opção não exige
 * {@code ALTER TABLE} (o banco é {@code ddl-auto=update}, sem Flyway). {@code version} é o controle de concorrência (D6).
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "bloco_conteudo", uniqueConstraints = @UniqueConstraint(columnNames = {"questionario_id", "numero"}))
public class BlocoConteudo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "questionario_id", nullable = false, updatable = false)
    private Questionario questionario;

    @Column(nullable = false, updatable = false)
    private int numero;

    @Version
    private Long version;

    @Column(nullable = false, length = 1_000_000)
    private String conteudo;

    @Column(nullable = false)
    private LocalDateTime atualizadoEm;

    public BlocoConteudo(Questionario questionario, int numero) {
        this.questionario = Objects.requireNonNull(questionario);
        this.numero = numero;
    }

    /** Troca o JSON inteiro e renova {@code atualizadoEm}, o que faz a versão subir mesmo com conteúdo igual. */
    public void atualizar(String conteudo) {
        this.conteudo = Objects.requireNonNull(conteudo);
        this.atualizadoEm = LocalDateTime.now(ZoneId.of("America/Sao_Paulo"));
    }
}
