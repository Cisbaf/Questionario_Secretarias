package questionario.exception;

/** E-mail ou senha do admin errados (401). A mensagem e igual para os dois, para nao dizer qual errou. */
public class CredenciaisInvalidasException extends RuntimeException {
    public CredenciaisInvalidasException() {
        super("E-mail ou senha inválidos");
    }
}
