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
import questionario.domain.enums.StatusQuestionario;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;
import java.util.UUID;

/**
 * Um questionário em preenchimento. Como a página é pública, quem tem o {@code publicId} (UUID) tem acesso: o
 * {@code id} numérico é só a chave interna e nunca sai na API. O município não é uma coluna daqui: é um texto livre
 * digitado no Bloco 1.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "questionarios")
public class Questionario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false, length = 36)
    private String publicId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "levantamento_id", nullable = false, updatable = false)
    private Levantamento levantamento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusQuestionario status = StatusQuestionario.RASCUNHO;

    @Column(nullable = false)
    private LocalDateTime criadoEm;

    private LocalDateTime enviadoEm;

    public Questionario(Levantamento levantamento) {
        this.levantamento = Objects.requireNonNull(levantamento);
        this.publicId = UUID.randomUUID().toString();
        this.criadoEm = LocalDateTime.now(ZoneId.of("America/Sao_Paulo"));
    }

    /** Envio definitivo (D12): a partir daqui os blocos não aceitam mais alteração. A tela de envio entra na E10. */
    public void marcarComoEnviado() {
        this.status = StatusQuestionario.ENVIADO;
        this.enviadoEm = LocalDateTime.now(ZoneId.of("America/Sao_Paulo"));
    }
}
