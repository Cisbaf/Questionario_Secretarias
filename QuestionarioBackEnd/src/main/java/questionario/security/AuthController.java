package questionario.security;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Map;

/**
 * Login do admin unico. O token vai num cookie HTTP-only {@code auth_token}: o navegador so fala com o Next, que
 * repassa o cookie ao Spring como {@code Authorization: Bearer} (ver proxy-fetch.ts).
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    public static final String COOKIE = "auth_token";

    private final LoginService login;
    private final JwtService jwt;
    private final AdminProperties admin;

    public record LoginRequest(
            @NotBlank(message = "Informe o e-mail") @Size(max = 150, message = "Máximo de 150 caracteres") String email,
            @NotBlank(message = "Informe a senha") @Size(max = 200, message = "Máximo de 200 caracteres") String senha) {
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> entrar(@Valid @RequestBody LoginRequest request) {
        String email = login.entrar(request.email(), request.senha());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie(jwt.gerar(email), jwt.duracao()).toString())
                .body(Map.of("email", email));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> sair() {
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO).toString())
                .build();
    }

    /** O front chama ao abrir /admin: 200 se o cookie ainda vale, 401 se nao. */
    @GetMapping("/validate")
    public Map<String, String> validar(Authentication autenticacao) {
        return Map.of("email", String.valueOf(autenticacao.getPrincipal()));
    }

    private ResponseCookie cookie(String valor, Duration duracao) {
        return ResponseCookie.from(COOKIE, valor)
                .httpOnly(true)
                .secure(admin.cookieSeguroLigado())
                .sameSite("Lax")
                .path("/")
                .maxAge(duracao)
                .build();
    }
}
