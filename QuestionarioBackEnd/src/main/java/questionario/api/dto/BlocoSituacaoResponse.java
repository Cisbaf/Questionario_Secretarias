package questionario.api.dto;

import java.time.LocalDateTime;

/**
 * Situação de um bloco dentro do questionário (para o Stepper). {@code version == null} significa que o bloco ainda
 * não foi salvo no servidor.
 */
public record BlocoSituacaoResponse(int numero, Long version, LocalDateTime atualizadoEm) {

    public static BlocoSituacaoResponse naoSalvo(int numero) {
        return new BlocoSituacaoResponse(numero, null, null);
    }
}
