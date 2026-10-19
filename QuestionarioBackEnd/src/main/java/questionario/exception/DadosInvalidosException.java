package questionario.exception;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Dados que passam no formato mas violam uma regra entre campos (ex.: mais de três opções marcadas). Vira HTTP 400 com
 * o mesmo formato do Bean Validation: {@code fields} com um texto por campo.
 */
public class DadosInvalidosException extends RuntimeException {

    private final Map<String, String> campos;

    public DadosInvalidosException(Map<String, String> campos) {
        super("Dados inválidos");
        this.campos = new LinkedHashMap<>(campos);
    }

    public Map<String, String> getCampos() {
        return campos;
    }
}
