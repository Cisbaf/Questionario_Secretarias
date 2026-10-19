package questionario.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/** Le {@code Authorization: Bearer ...} (o BFF poe o token do cookie) e autentica o admin. Sem token, segue anonimo. */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwt;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String cabecalho = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (cabecalho != null && cabecalho.startsWith("Bearer ")) {
            jwt.emailDe(cabecalho.substring(7)).ifPresent(email -> SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(email, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))));
        }
        chain.doFilter(request, response);
    }
}
