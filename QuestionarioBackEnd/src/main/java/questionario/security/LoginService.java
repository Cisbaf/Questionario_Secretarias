package questionario.security;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import questionario.exception.CredenciaisInvalidasException;
import questionario.exception.LoginBloqueadoException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;

/**
 * Confere o e-mail e a senha do admin unico. Depois de {@value #MAX_FALHAS} erros seguidos o login fica bloqueado por
 * {@value #BLOQUEIO_MINUTOS} minutos (so um admin, entao o contador e um so, em memoria).
 */
@Service
public class LoginService {

    static final int MAX_FALHAS = 5;
    static final int BLOQUEIO_MINUTOS = 15;

    private final AdminProperties admin;
    private final PasswordEncoder encoder;
    private final String senhaCodificada;

    private int falhas;
    private Instant bloqueadoAte;

    public LoginService(AdminProperties admin, PasswordEncoder encoder) {
        this.admin = admin;
        this.encoder = encoder;
        this.senhaCodificada = admin.configurado() ? encoder.encode(admin.password()) : null;
    }

    /** Devolve o e-mail do admin se estiver certo; senao lanca 401 (ou 429 se estiver bloqueado). */
    public synchronized String entrar(String email, String senha) {
        Instant agora = Instant.now();
        if (bloqueadoAte != null && agora.isBefore(bloqueadoAte)) {
            throw new LoginBloqueadoException(Duration.between(agora, bloqueadoAte).toMinutes() + 1);
        }
        boolean certo = admin.configurado()
                && iguais(admin.email().strip(), email == null ? "" : email.strip())
                && encoder.matches(senha == null ? "" : senha, senhaCodificada);
        if (!certo) {
            if (++falhas >= MAX_FALHAS) {
                bloqueadoAte = agora.plus(Duration.ofMinutes(BLOQUEIO_MINUTOS));
                falhas = 0;
            }
            throw new CredenciaisInvalidasException();
        }
        falhas = 0;
        bloqueadoAte = null;
        return admin.email().strip();
    }

    private static boolean iguais(String a, String b) {
        return MessageDigest.isEqual(a.toLowerCase().getBytes(StandardCharsets.UTF_8), b.toLowerCase().getBytes(StandardCharsets.UTF_8));
    }
}
