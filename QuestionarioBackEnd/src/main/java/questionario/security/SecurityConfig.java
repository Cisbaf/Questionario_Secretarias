package questionario.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.nio.charset.StandardCharsets;

/**
 * Uma regra so (plano, E11): {@code /api/admin/**} e {@code /api/auth/validate} exigem o admin; todo o resto segue
 * publico, para o questionario continuar abrindo e salvando sem login.
 */
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain filtros(HttpSecurity http, JwtAuthenticationFilter jwt) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable) // sem sessao e sem cookie lido pelo Spring: o token vem no cabecalho
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(a -> a
                        .requestMatchers("/api/admin/**", "/api/auth/validate").hasRole("ADMIN")
                        .anyRequest().permitAll())
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, ex) -> erro(res, HttpServletResponse.SC_UNAUTHORIZED, "Faça login para continuar"))
                        .accessDeniedHandler((req, res, ex) -> erro(res, HttpServletResponse.SC_FORBIDDEN, "Acesso negado")))
                .addFilterBefore(jwt, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private static void erro(HttpServletResponse res, int status, String mensagem) throws java.io.IOException {
        res.setStatus(status);
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding(StandardCharsets.UTF_8.name());
        res.getWriter().write("{\"message\":\"" + mensagem + "\"}");
    }
}
