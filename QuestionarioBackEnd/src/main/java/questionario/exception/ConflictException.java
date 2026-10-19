package questionario.exception;

/**
 * Regra de negócio violada por um estado que já existe (vira HTTP 409). O {@code codigo} (opcional) deixa o front
 * distinguir os conflitos: "versão desatualizada" oferece recarregar/sobrescrever; "enviado" só trava a edição.
 */
public class ConflictException extends RuntimeException {

    /** Outra pessoa (ou outra aba) salvou o bloco depois da versão que o cliente tinha. */
    public static final String VERSAO_DESATUALIZADA = "VERSAO_DESATUALIZADA";
    /** O questionário já foi enviado e não aceita mais alterações. */
    public static final String QUESTIONARIO_ENVIADO = "QUESTIONARIO_ENVIADO";

    private final String codigo;

    public ConflictException(String mensagem) {
        this(null, mensagem);
    }

    public ConflictException(String codigo, String mensagem) {
        super(mensagem);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
