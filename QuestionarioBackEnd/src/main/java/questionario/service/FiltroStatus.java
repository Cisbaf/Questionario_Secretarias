package questionario.service;

import questionario.domain.Questionario;
import questionario.domain.enums.StatusQuestionario;

/** Filtro da lista e do consolidado do admin. */
public enum FiltroStatus {
    TODOS, ENVIADO, RASCUNHO;

    /** Vazio usa o padrao; texto desconhecido vira 400 (IllegalArgumentException). */
    public static FiltroStatus de(String texto, FiltroStatus padrao) {
        if (texto == null || texto.isBlank()) return padrao;
        try {
            return valueOf(texto.strip().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Filtro de status inválido: use TODOS, ENVIADO ou RASCUNHO");
        }
    }

    public boolean aceita(Questionario q) {
        return this == TODOS || (this == ENVIADO) == (q.getStatus() == StatusQuestionario.ENVIADO);
    }
}
