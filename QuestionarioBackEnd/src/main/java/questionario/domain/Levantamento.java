package questionario.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import questionario.domain.enums.StatusLevantamento;

import java.time.LocalDate;
import java.util.Objects;

/**
 * A "rodada" do questionário: guarda os campos da capa do documento (período de referência, data do censo nas UPAs,
 * prazo de devolução e para onde enviar). Os valores vêm das propriedades {@code app.levantamento.*}; não há tela de
 * edição, porque o admin só visualiza (ver plano, D2 e D3).
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "levantamentos")
public class Levantamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String titulo;

    private LocalDate periodoReferenciaInicio;
    private LocalDate periodoReferenciaFim;
    private LocalDate dataCensoUpa;
    private LocalDate prazoDevolucao;

    @Column(length = 200)
    private String enviarPara;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusLevantamento status = StatusLevantamento.ABERTO;

    public Levantamento(String titulo, LocalDate periodoReferenciaInicio, LocalDate periodoReferenciaFim,
                        LocalDate dataCensoUpa, LocalDate prazoDevolucao, String enviarPara) {
        atualizarCapa(titulo, periodoReferenciaInicio, periodoReferenciaFim, dataCensoUpa, prazoDevolucao, enviarPara);
    }

    public void atualizarCapa(String titulo, LocalDate periodoReferenciaInicio, LocalDate periodoReferenciaFim,
                              LocalDate dataCensoUpa, LocalDate prazoDevolucao, String enviarPara) {
        this.titulo = Objects.requireNonNull(titulo);
        this.periodoReferenciaInicio = periodoReferenciaInicio;
        this.periodoReferenciaFim = periodoReferenciaFim;
        this.dataCensoUpa = dataCensoUpa;
        this.prazoDevolucao = prazoDevolucao;
        this.enviarPara = enviarPara;
    }

    public void encerrar() {
        this.status = StatusLevantamento.ENCERRADO;
    }
}
