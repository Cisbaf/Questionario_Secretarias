package questionario;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Bloqueio do login depois de erros seguidos. Contexto próprio: o bloqueio não pode atrapalhar os outros testes. */
@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AdminLoginBloqueioTest {

    @Autowired
    WebApplicationContext context;
    @Autowired
    FilterChainProxy seguranca;

    @Test
    @DisplayName("5 erros seguidos bloqueiam o login (429), até com a senha certa")
    void bloqueia() throws Exception {
        MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(seguranca).build();
        for (int i = 0; i < 5; i++) {
            mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"admin@teste.local\",\"senha\":\"errada" + i + "\"}")).andExpect(status().isUnauthorized());
        }
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"admin@teste.local\",\"senha\":\"senha-de-teste\"}")).andExpect(status().isTooManyRequests());
    }
}
