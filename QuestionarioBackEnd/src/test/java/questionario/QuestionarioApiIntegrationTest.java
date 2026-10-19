package questionario;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import questionario.domain.Questionario;
import questionario.repository.QuestionarioRepository;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Iniciar e abrir um questionário pelo link público (publicId), sem login. */
@SpringBootTest
@ActiveProfiles("test")
class QuestionarioApiIntegrationTest {

    private static final Pattern PUBLIC_ID = Pattern.compile("\"publicId\"\\s*:\\s*\"([^\"]+)\"");

    @Autowired
    WebApplicationContext context;
    @Autowired
    QuestionarioRepository repository;

    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    /** Inicia um questionário pela API e devolve o publicId. */
    private String iniciar() throws Exception {
        String corpo = mvc.perform(post("/api/questionarios"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Matcher matcher = PUBLIC_ID.matcher(corpo);
        assertThat(matcher.find()).as("resposta com publicId: %s", corpo).isTrue();
        return matcher.group(1);
    }

    @Test
    @DisplayName("Iniciar cria um rascunho com publicId UUID e sem expor o id numérico")
    void iniciarCriaRascunho() throws Exception {
        mvc.perform(post("/api/questionarios"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("RASCUNHO"))
                .andExpect(jsonPath("$.criadoEm").exists())
                .andExpect(jsonPath("$.enviadoEm").doesNotExist())
                .andExpect(jsonPath("$.id").doesNotExist());

        String publicId = iniciar();
        assertThat(UUID.fromString(publicId)).hasToString(publicId);
        assertThat(repository.findByPublicId(publicId)).isPresent();
    }

    @Test
    @DisplayName("Dois inícios geram dois questionários com links diferentes (o município é texto livre, não há bloqueio)")
    void doisInicios() throws Exception {
        String primeiro = iniciar();
        String segundo = iniciar();

        assertThat(primeiro).isNotEqualTo(segundo);
    }

    @Test
    @DisplayName("Abrir pelo publicId devolve o questionário")
    void buscarPeloPublicId() throws Exception {
        String publicId = iniciar();

        mvc.perform(get("/api/questionarios/" + publicId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicId").value(publicId))
                .andExpect(jsonPath("$.status").value("RASCUNHO"))
                .andExpect(jsonPath("$.criadoEm").exists())
                .andExpect(jsonPath("$.id").doesNotExist());
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "1", "00000000-0000-0000-0000-000000000000"})
    @DisplayName("Link inexistente, lixo ou que não é UUID dá sempre 404, sem pista do motivo")
    void linkInvalido(String publicId) throws Exception {
        mvc.perform(get("/api/questionarios/{id}", publicId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Questionário não encontrado"));
    }

    @Test
    @DisplayName("O id numérico interno não abre o questionário (não dá para enumerar trocando o número)")
    void idNumericoNaoAbre() throws Exception {
        String publicId = iniciar();
        Questionario salvo = repository.findByPublicId(publicId).orElseThrow();

        mvc.perform(get("/api/questionarios/" + salvo.getId()))
                .andExpect(status().isNotFound());
    }
}
