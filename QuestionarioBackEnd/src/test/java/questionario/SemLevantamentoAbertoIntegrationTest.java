package questionario;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import questionario.repository.LevantamentoRepository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Sem levantamento aberto (encerrado): a capa dá 404 e ninguém consegue iniciar questionário. H2 próprio. */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:semlevantamento;MODE=MySQL;DB_CLOSE_DELAY=-1")
@ActiveProfiles("test")
class SemLevantamentoAbertoIntegrationTest {

    @Autowired
    WebApplicationContext context;
    @Autowired
    LevantamentoRepository levantamentos;

    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        levantamentos.findAll().forEach(levantamento -> {
            levantamento.encerrar();
            levantamentos.save(levantamento);
        });
    }

    @Test
    @DisplayName("A capa responde 404 com a mensagem de que não há levantamento aberto")
    void capaSemLevantamento() throws Exception {
        mvc.perform(get("/api/levantamentos/ativo"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Nenhum levantamento aberto no momento"));
    }

    @Test
    @DisplayName("Iniciar questionário responde 409 com a mensagem da regra")
    void iniciarSemLevantamento() throws Exception {
        mvc.perform(post("/api/questionarios"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Não há levantamento aberto para receber questionários"));
    }
}
