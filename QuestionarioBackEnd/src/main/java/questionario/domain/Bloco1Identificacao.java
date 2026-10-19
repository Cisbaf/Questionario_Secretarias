package questionario.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;

/**
 * Bloco 1 do documento: identificação. Todos os campos são opcionais no rascunho (D5); o envio final (E10) exige o
 * obrigatório. {@code version} é o controle de concorrência (D6): o PUT só vale se o cliente tinha a versão atual.
 * O município é texto livre (D10).
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "bloco1_identificacao")
public class Bloco1Identificacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "questionario_id", nullable = false, unique = true, updatable = false)
    private Questionario questionario;

    @Version
    private Long version;

    @Column(length = 150)
    private String municipio;

    @Column(length = 150)
    private String secretario;

    @Column(length = 150)
    private String responsavel;

    @Column(length = 150)
    private String cargoSetor;

    @Column(length = 150)
    private String email;

    @Column(length = 30)
    private String telefone;

    private LocalDate dataPreenchimento;

    @Column(nullable = false)
    private LocalDateTime atualizadoEm;

    public Bloco1Identificacao(Questionario questionario) {
        this.questionario = Objects.requireNonNull(questionario);
    }

    /** Substitui todos os campos do bloco (o PUT sempre manda o bloco inteiro) e renova {@code atualizadoEm}. */
    public void atualizar(String municipio, String secretario, String responsavel, String cargoSetor,
                          String email, String telefone, LocalDate dataPreenchimento) {
        this.municipio = municipio;
        this.secretario = secretario;
        this.responsavel = responsavel;
        this.cargoSetor = cargoSetor;
        this.email = email;
        this.telefone = telefone;
        this.dataPreenchimento = dataPreenchimento;
        // Sempre muda, então cada salvamento incrementa a versão, mesmo que os textos sejam idênticos.
        this.atualizadoEm = LocalDateTime.now(ZoneId.of("America/Sao_Paulo"));
    }
}
