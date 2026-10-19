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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** A capa que o município vê, vinda das propriedades do perfil "test". */
@SpringBootTest
@ActiveProfiles("test")
class LevantamentoApiIntegrationTest {

    @Autowired
    WebApplicationContext context;

    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    @DisplayName("Ao subir, o levantamento aberto reflete as propriedades (título, quatro datas e para onde enviar)")
    void capaVemDasPropriedades() throws Exception {
        mvc.perform(get("/api/levantamentos/ativo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Questionário de Teste"))
                .andExpect(jsonPath("$.periodoReferenciaInicio").value("2026-01-01"))
                .andExpect(jsonPath("$.periodoReferenciaFim").value("2026-06-30"))
                .andExpect(jsonPath("$.dataCensoUpa").value("2026-11-05"))
                .andExpect(jsonPath("$.prazoDevolucao").value("2026-11-30"))
                .andExpect(jsonPath("$.enviarPara").value("planejamento@cisbaf.org.br"));
    }

    @Test
    @DisplayName("O id interno do levantamento não sai na API")
    void idInternoNaoSai() throws Exception {
        mvc.perform(get("/api/levantamentos/ativo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").doesNotExist())
                .andExpect(jsonPath("$.status").doesNotExist());
    }
}
