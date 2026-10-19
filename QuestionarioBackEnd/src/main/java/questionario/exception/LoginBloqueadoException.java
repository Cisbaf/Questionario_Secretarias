package questionario.exception;

/** Erros demais seguidos no login do admin (429). */
public class LoginBloqueadoException extends RuntimeException {
    public LoginBloqueadoException(long minutos) {
        super("Muitas tentativas. Tente de novo em " + minutos + " minuto(s).");
    }
}
