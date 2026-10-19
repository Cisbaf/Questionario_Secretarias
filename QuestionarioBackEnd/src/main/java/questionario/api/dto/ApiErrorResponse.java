package questionario.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

/**
 * Formato único de erro da API. O front (lib/api.ts) lê {@code fields} (erro por campo) e, se não houver, {@code message}.
 * O {@code code} só aparece em conflitos que o front trata de forma diferente (ver {@code ConflictException}).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiErrorResponse(String message, Map<String, String> fields, String code) {

    public ApiErrorResponse(String message) {
        this(message, null, null);
    }

    public ApiErrorResponse(String message, Map<String, String> fields) {
        this(message, fields, null);
    }
}
