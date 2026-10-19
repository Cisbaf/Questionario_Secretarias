package questionario.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import questionario.domain.enums.ResolveSozinho;

/**
 * Uma das cinco linhas do item 2.1 do Bloco 2. {@code posicao} (1 a 5) é obrigatória no banco: assim a linha existe
 * mesmo vazia (um embutido com todas as colunas nulas seria lido como nulo pelo Hibernate).
 */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PrioridadeItem {

    @Column(name = "posicao", nullable = false)
    private int posicao;

    @Column(name = "titulo", length = 300)
    private String titulo;

    @Column(name = "problema", length = 500)
    private String problema;

    @Enumerated(EnumType.STRING)
    @Column(name = "resolve_sozinho", length = 20)
    private ResolveSozinho resolveSozinho;

    public PrioridadeItem(int posicao, String titulo, String problema, ResolveSozinho resolveSozinho) {
        this.posicao = posicao;
        this.titulo = titulo;
        this.problema = problema;
        this.resolveSozinho = resolveSozinho;
    }
}
