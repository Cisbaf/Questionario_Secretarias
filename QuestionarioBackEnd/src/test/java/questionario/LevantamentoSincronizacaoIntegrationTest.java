package questionario;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import questionario.config.LevantamentoProperties;
import questionario.repository.LevantamentoRepository;
import questionario.service.LevantamentoService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sincronização propriedades → levantamento aberto. H2 próprio ("capasync"), porque estes testes mudam a capa; o
 * {@code @AfterEach} devolve a capa de teste para não deixar rastro.
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:capasync;MODE=MySQL;DB_CLOSE_DELAY=-1")
@ActiveProfiles("test")
class LevantamentoSincronizacaoIntegrationTest {

    @Autowired
    WebApplicationContext context;
    @Autowired
    LevantamentoService service;
    @Autowired
    LevantamentoRepository repository;

    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @AfterEach
    void restauraCapaDeTeste() {
        service.sincronizar();
    }

    @Test
    @DisplayName("Sincronizar de novo (reiniciar o servidor) não cria outro levantamento")
    void naoDuplica() {
        service.sincronizar();
        service.sincronizar();

        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Mudar a propriedade e reiniciar atualiza o levantamento aberto, sem criar outro")
    void mudarPropriedadeAtualiza() throws Exception {
        service.sincronizar(new LevantamentoProperties(
                "Outro título", "2026-02-01", "2026-02-28", null, "2026-03-15", "outro@cisbaf.org.br"));

        mvc.perform(get("/api/levantamentos/ativo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Outro título"))
                .andExpect(jsonPath("$.periodoReferenciaInicio").value("2026-02-01"))
                .andExpect(jsonPath("$.dataCensoUpa").doesNotExist())
                .andExpect(jsonPath("$.prazoDevolucao").value("2026-03-15"))
                .andExpect(jsonPath("$.enviarPara").value("outro@cisbaf.org.br"));
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Tudo em branco: título padrão e datas nulas ('a definir' na tela), sem erro")
    void tudoEmBranco() throws Exception {
        service.sincronizar(new LevantamentoProperties("", "", "", "", "", ""));

        mvc.perform(get("/api/levantamentos/ativo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value(LevantamentoProperties.TITULO_PADRAO))
                .andExpect(jsonPath("$.periodoReferenciaInicio").doesNotExist())
                .andExpect(jsonPath("$.periodoReferenciaFim").doesNotExist())
                .andExpect(jsonPath("$.dataCensoUpa").doesNotExist())
                .andExpect(jsonPath("$.prazoDevolucao").doesNotExist())
                .andExpect(jsonPath("$.enviarPara").doesNotExist());
    }

    @Test
    @DisplayName("Propriedade inválida falha antes de gravar: a capa anterior continua intacta")
    void propriedadeInvalidaNaoGravaNada() throws Exception {
        assertThatThrownBy(() -> service.sincronizar(new LevantamentoProperties(
                "Não pode gravar", "01/02/2026", null, null, null, null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("periodo-referencia-inicio");

        mvc.perform(get("/api/levantamentos/ativo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Questionário de Teste"));
    }
}
