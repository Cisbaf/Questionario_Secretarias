package questionario.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Corpo do PUT do Bloco 1. No rascunho tudo é opcional; aqui só valem formato e tamanho (D5). {@code version} é a
 * versão que o cliente tinha ({@code null} no primeiro salvamento).
 */
public record Bloco1Request(
        @Size(max = 150, message = "Máximo de 150 caracteres") String municipio,
        @Size(max = 150, message = "Máximo de 150 caracteres") String secretario,
        @Size(max = 150, message = "Máximo de 150 caracteres") String responsavel,
        @Size(max = 150, message = "Máximo de 150 caracteres") String cargoSetor,
        @Size(max = 150, message = "Máximo de 150 caracteres")
        @Email(regexp = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$", message = "E-mail inválido") String email,
        @Size(max = 30, message = "Máximo de 30 caracteres")
        @Pattern(regexp = "^[0-9 ()+\\-]*$", message = "Use apenas números, espaços, parênteses, + e -") String telefone,
        LocalDate dataPreenchimento,
        Long version) {
}
