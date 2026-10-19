package questionario;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Esqueleto (E01, atualizado na E02): o contexto sobe, uma rota pública responde sem login, o health fica UP e o CORS
 * segue a propriedade. Mesmo padrão do Contratos: {@code @SpringBootTest} + {@code webAppContextSetup}, perfil "test" (H2).
 */
@SpringBootTest
@ActiveProfiles("test")
class E01EsqueletoIntegrationTest {

    private static final String ROTA_PUBLICA = "/api/levantamentos/ativo";

    @Autowired
    WebApplicationContext context;

    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    @DisplayName("A capa responde 200 sem nenhum token (o questionário é público)")
    void rotaPublicaSemLogin() throws Exception {
        mvc.perform(get(ROTA_PUBLICA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").exists());
    }

    @Test
    @DisplayName("GET /actuator/health responde UP")
    void healthUp() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("Rota inexistente responde 404")
    void rotaInexistente() throws Exception {
        mvc.perform(get("/api/nao-existe"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("CORS: a origem configurada é aceita no preflight")
    void corsOrigemPermitida() throws Exception {
        mvc.perform(options(ROTA_PUBLICA)
                        .header(HttpHeaders.ORIGIN, "http://localhost:3002")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3002"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
    }

    @Test
    @DisplayName("CORS: uma origem desconhecida é recusada")
    void corsOrigemDesconhecida() throws Exception {
        mvc.perform(options(ROTA_PUBLICA)
                        .header(HttpHeaders.ORIGIN, "http://site-malicioso.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }
}
