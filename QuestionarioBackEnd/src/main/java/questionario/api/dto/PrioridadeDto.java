package questionario.api.dto;

import jakarta.validation.constraints.Size;
import questionario.domain.enums.ResolveSozinho;

/** Uma linha do item 2.1 do Bloco 2 (a posição é a ordem na lista). */
public record PrioridadeDto(
        @Size(max = 300, message = "Máximo de 300 caracteres") String titulo,
        @Size(max = 500, message = "Máximo de 500 caracteres") String problema,
        ResolveSozinho resolveSozinho) {
}
