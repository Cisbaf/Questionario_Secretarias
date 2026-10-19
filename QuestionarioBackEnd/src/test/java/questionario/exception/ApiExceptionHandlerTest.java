package questionario.exception;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Garante o formato {message, fields} que o front (lib/api.ts) espera. Usa um controller de teste só para
 * disparar cada exceção, já que a E01 ainda não tem endpoints com validação.
 */
class ApiExceptionHandlerTest {

    record Corpo(@NotBlank(message = "Informe o nome") String nome,
                 @Size(max = 5, message = "Máximo de 5 caracteres") String sigla) {
    }

    @RestController
    static class ControllerDeTeste {
        @PostMapping("/valida")
        void valida(@Valid @RequestBody Corpo corpo) {
        }

        @GetMapping("/conflito")
        void conflito() {
            throw new ConflictException("Já existe um questionário para este município");
        }

        @GetMapping("/conflito-com-codigo")
        void conflitoComCodigo() {
            throw new ConflictException(ConflictException.VERSAO_DESATUALIZADA, "Bloco alterado por outra pessoa");
        }

        @GetMapping("/dados-invalidos")
        void dadosInvalidos() {
            throw new DadosInvalidosException(Map.of("impedimentos", "Marque no máximo 3 opções"));
        }

        @GetMapping("/bloqueio-otimista")
        void bloqueioOtimista() {
            throw new ObjectOptimisticLockingFailureException(String.class, 1L);
        }

        @GetMapping("/nao-encontrado")
        void naoEncontrado() {
            throw new EntityNotFoundException("Questionário não encontrado");
        }

        @GetMapping("/argumento")
        void argumento() {
            throw new IllegalArgumentException("O tamanho máximo da página é 100");
        }

        @GetMapping("/integridade")
        void integridade() {
            throw new DataIntegrityViolationException("detalhe interno do banco que não pode vazar");
        }

        @GetMapping("/data")
        void data(@RequestParam LocalDate dia) {
        }

        @GetMapping("/obrigatorio")
        void obrigatorio(@RequestParam String codigo) {
        }
    }

    MockMvc mvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mvc = MockMvcBuilders.standaloneSetup(new ControllerDeTeste())
                .setControllerAdvice(new ApiExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    @DisplayName("Validação: 400 com 'fields' por campo")
    void validacao() throws Exception {
        mvc.perform(post("/valida").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"\",\"sigla\":\"ABCDEFG\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Dados inválidos"))
                .andExpect(jsonPath("$.fields.nome").value("Informe o nome"))
                .andExpect(jsonPath("$.fields.sigla").value("Máximo de 5 caracteres"));
    }

    @Test
    @DisplayName("Corpo malformado: 400 sem vazar detalhes do parser")
    void corpoMalformado() throws Exception {
        mvc.perform(post("/valida").contentType(MediaType.APPLICATION_JSON).content("{nome:"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Corpo da requisição ausente ou malformado"))
                .andExpect(jsonPath("$.fields").doesNotExist());
    }

    @Test
    @DisplayName("ConflictException: 409 com a mensagem da regra")
    void conflito() throws Exception {
        mvc.perform(get("/conflito"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Já existe um questionário para este município"));
    }

    @Test
    @DisplayName("ConflictException sem código: 409 sem o campo 'code'")
    void conflitoSemCodigo() throws Exception {
        mvc.perform(get("/conflito"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").doesNotExist());
    }

    @Test
    @DisplayName("ConflictException com código: 409 com 'code' para o front escolher o tratamento")
    void conflitoComCodigo() throws Exception {
        mvc.perform(get("/conflito-com-codigo"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Bloco alterado por outra pessoa"))
                .andExpect(jsonPath("$.code").value("VERSAO_DESATUALIZADA"));
    }

    @Test
    @DisplayName("DadosInvalidosException (regra entre campos): 400 com 'fields', igual ao Bean Validation")
    void dadosInvalidos() throws Exception {
        mvc.perform(get("/dados-invalidos"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Dados inválidos"))
                .andExpect(jsonPath("$.fields.impedimentos").value("Marque no máximo 3 opções"))
                .andExpect(jsonPath("$.code").doesNotExist());
    }

    @Test
    @DisplayName("Duas gravações simultâneas (lock otimista do JPA): 409 VERSAO_DESATUALIZADA, sem detalhe interno")
    void bloqueioOtimista() throws Exception {
        mvc.perform(get("/bloqueio-otimista"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VERSAO_DESATUALIZADA"))
                .andExpect(jsonPath("$.message").value(
                        "Este bloco foi alterado por outra pessoa. Recarregue para ver a versão mais recente."));
    }

    @Test
    @DisplayName("EntityNotFoundException: 404")
    void naoEncontrado() throws Exception {
        mvc.perform(get("/nao-encontrado"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Questionário não encontrado"));
    }

    @Test
    @DisplayName("IllegalArgumentException: 400 com a mensagem")
    void argumentoIlegal() throws Exception {
        mvc.perform(get("/argumento"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("O tamanho máximo da página é 100"));
    }

    @Test
    @DisplayName("Violação de integridade: 409 com mensagem genérica (o detalhe do banco não vaza)")
    void integridade() throws Exception {
        mvc.perform(get("/integridade"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Operação não pôde ser concluída por conflito de dados"));
    }

    @Test
    @DisplayName("Data inválida na query: 400 com dica do formato")
    void dataInvalida() throws Exception {
        mvc.perform(get("/data").param("dia", "amanha"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Parâmetro 'dia' inválido: use uma data no formato AAAA-MM-DD"));
    }

    @Test
    @DisplayName("Parâmetro obrigatório ausente: 400")
    void parametroAusente() throws Exception {
        mvc.perform(get("/obrigatorio"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Parâmetro obrigatório ausente: 'codigo'"));
    }
}
