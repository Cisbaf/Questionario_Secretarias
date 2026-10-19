package questionario.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.MapKeyEnumerated;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import questionario.domain.enums.Impedimento;
import questionario.domain.enums.LinhaCuidado;
import questionario.domain.enums.TemaUrgencia;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Bloco 2 do documento: prioridades de atenção à saúde (itens 2.1 a 2.6). Tudo é opcional no rascunho (D5); o envio
 * final (E10) confere o obrigatório. Só ficam gravadas as notas e posições respondidas: uma linha de cuidado sem nota
 * simplesmente não tem registro. {@code version} é o controle de concorrência (D6).
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "bloco2_prioridades")
public class Bloco2Prioridades {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "questionario_id", nullable = false, unique = true, updatable = false)
    private Questionario questionario;

    @Version
    private Long version;

    /** 2.1: as cinco linhas, sempre com posições 1 a 5. */
    @ElementCollection
    @CollectionTable(name = "bloco2_prioridade", joinColumns = @JoinColumn(name = "bloco_id"))
    @OrderBy("posicao ASC")
    private List<PrioridadeItem> prioridades = new ArrayList<>();

    /** 2.2: nota de 1 a 5 por linha de cuidado (só as respondidas). */
    @ElementCollection
    @CollectionTable(name = "bloco2_grau_linha", joinColumns = @JoinColumn(name = "bloco_id"))
    @MapKeyEnumerated(EnumType.STRING)
    @MapKeyColumn(name = "linha", length = 60)
    @Column(name = "grau", nullable = false)
    private Map<LinhaCuidado, Integer> graus = new HashMap<>();

    /** 2.2: descrição da linha "Outra". */
    @Column(length = 150)
    private String linhaOutraDescricao;

    /** 2.3 */
    @Column(length = 5000)
    private String gargalos;

    /** 2.4 */
    @Column(length = 5000)
    private String servicosParaVizinhos;

    /** 2.5: até três. */
    @ElementCollection
    @CollectionTable(name = "bloco2_impedimento", joinColumns = @JoinColumn(name = "bloco_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "impedimento", length = 60)
    private Set<Impedimento> impedimentos = new HashSet<>();

    /** 2.5: descrição do impedimento "Outro" (só vale com OUTRO marcado). */
    @Column(length = 150)
    private String impedimentoOutro;

    /** 2.6: posição (1 = mais urgente) de cada tema; vazio se a ordem ainda não foi definida. */
    @ElementCollection
    @CollectionTable(name = "bloco2_ordem_tema", joinColumns = @JoinColumn(name = "bloco_id"))
    @MapKeyEnumerated(EnumType.STRING)
    @MapKeyColumn(name = "tema", length = 60)
    @Column(name = "posicao", nullable = false)
    private Map<TemaUrgencia, Integer> ordemTemas = new HashMap<>();

    @Column(nullable = false)
    private LocalDateTime atualizadoEm;

    public Bloco2Prioridades(Questionario questionario) {
        this.questionario = Objects.requireNonNull(questionario);
    }

    /** Substitui o bloco inteiro (o PUT sempre manda tudo) e renova {@code atualizadoEm}, que faz a versão subir. */
    public void atualizar(List<PrioridadeItem> prioridades, Map<LinhaCuidado, Integer> graus, String linhaOutraDescricao,
                          String gargalos, String servicosParaVizinhos, Set<Impedimento> impedimentos,
                          String impedimentoOutro, Map<TemaUrgencia, Integer> ordemTemas) {
        this.prioridades.clear();
        this.prioridades.addAll(prioridades);
        this.graus.clear();
        this.graus.putAll(graus);
        this.linhaOutraDescricao = linhaOutraDescricao;
        this.gargalos = gargalos;
        this.servicosParaVizinhos = servicosParaVizinhos;
        this.impedimentos.clear();
        this.impedimentos.addAll(impedimentos);
        this.impedimentoOutro = impedimentoOutro;
        this.ordemTemas.clear();
        this.ordemTemas.putAll(ordemTemas);
        this.atualizadoEm = LocalDateTime.now(ZoneId.of("America/Sao_Paulo"));
    }
}
