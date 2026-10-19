package questionario.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * O admin unico (D2): sem tabela de usuarios, e-mail e senha vem do ambiente. Sem e-mail ou senha configurados o
 * login fica desligado (sempre recusa), entao esquecer a variavel nunca abre a area do admin.
 */
@ConfigurationProperties(prefix = "app.admin")
public record AdminProperties(String email, String password, String jwtSecret, Integer jwtHoras, Boolean cookieSeguro) {

    public int horasDeSessao() {
        return jwtHoras == null || jwtHoras < 1 ? 8 : jwtHoras;
    }

    public boolean cookieSeguroLigado() {
        return Boolean.TRUE.equals(cookieSeguro);
    }

    public boolean configurado() {
        return email != null && !email.isBlank() && password != null && !password.isBlank()
                && jwtSecret != null && jwtSecret.length() >= 32;
    }
}
