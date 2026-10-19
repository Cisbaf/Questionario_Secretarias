package questionario.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS por propriedade ({@code cors.allowed-origins}, separada por vírgula). No uso normal o navegador só fala com o
 * Next (rota /api, que repassa ao Spring), então o CORS é rede de segurança para chamadas diretas em desenvolvimento.
 * Na E11 o Spring Security passa a controlar o CORS e esta classe deve ser removida.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final String[] origins;

    public CorsConfig(@Value("${cors.allowed-origins}") String[] origins) {
        this.origins = origins;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns(origins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
