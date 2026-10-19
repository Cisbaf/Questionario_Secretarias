package questionario.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

/** Token JWT (HS256) do admin. Guarda so o e-mail; a validade vem de {@code app.admin.jwt-horas}. */
@Service
public class JwtService {

    private final AdminProperties admin;
    private final SecretKey chave;

    public JwtService(AdminProperties admin) {
        this.admin = admin;
        // Sem segredo valido o login esta desligado; a chave so existe para o servico poder subir.
        String segredo = admin.configurado() ? admin.jwtSecret() : "login-desligado-sem-segredo-configurado-0000";
        this.chave = Keys.hmacShaKeyFor(segredo.getBytes(StandardCharsets.UTF_8));
    }

    public String gerar(String email) {
        Instant agora = Instant.now();
        return Jwts.builder()
                .subject(email)
                .issuedAt(Date.from(agora))
                .expiration(Date.from(agora.plus(duracao())))
                .signWith(chave)
                .compact();
    }

    /** E-mail do token, ou vazio se estiver errado, adulterado ou vencido. */
    public Optional<String> emailDe(String token) {
        if (!admin.configurado()) return Optional.empty();
        try {
            Claims claims = Jwts.parser().verifyWith(chave).build().parseSignedClaims(token).getPayload();
            return Optional.ofNullable(claims.getSubject());
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public Duration duracao() {
        return Duration.ofHours(admin.horasDeSessao());
    }
}
