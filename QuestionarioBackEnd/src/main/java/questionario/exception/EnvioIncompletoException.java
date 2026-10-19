package questionario.exception;

import java.util.LinkedHashMap;
import java.util.Map;

/** O envio final foi recusado porque faltam obrigatórios ou há números incoerentes (HTTP 409, código ENVIO_INCOMPLETO). */
public class EnvioIncompletoException extends RuntimeException {

    public static final String CODIGO = "ENVIO_INCOMPLETO";

    private final Map<String, String> pendencias;

    public EnvioIncompletoException(Map<String, String> pendencias) {
        super("Ainda há pendências no questionário. Corrija-as para enviar.");
        this.pendencias = new LinkedHashMap<>(pendencias);
    }

    public Map<String, String> getPendencias() {
        return pendencias;
    }
}
