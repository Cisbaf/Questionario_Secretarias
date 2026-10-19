package questionario;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import questionario.domain.Questionario;
import questionario.repository.QuestionarioRepository;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Bloco 1 (identificação): leitura, gravação com versão (D6), validação (D5) e bloqueio depois do envio (D12). */
@SpringBootTest
@ActiveProfiles("test")
class Bloco1ApiIntegrationTest {

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

    private String iniciar() throws Exception {
        String corpo = mvc.perform(post("/api/questionarios"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Matcher matcher = PUBLIC_ID.matcher(corpo);
        assertThat(matcher.find()).as("resposta com publicId: %s", corpo).isTrue();
        return matcher.group(1);
    }

    private static String bloco1(String municipio, Long version) {
        return "{\"municipio\":" + texto(municipio) + ",\"secretario\":\"Maria Souza\",\"responsavel\":\"João Lima\","
                + "\"cargoSetor\":\"Planejamento\",\"email\":\"joao@exemplo.gov.br\",\"telefone\":\"(21) 99999-0000\","
                + "\"dataPreenchimento\":\"2026-10-19\",\"version\":" + version + "}";
    }

    private static String texto(String valor) {
        return valor == null ? "null" : "\"" + valor + "\"";
    }

    private void salvar(String id, String corpo, int statusEsperado) throws Exception {
        mvc.perform(put("/api/questionarios/" + id + "/blocos/1").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().is(statusEsperado));
    }

    @Test
    @DisplayName("Bloco ainda não salvo: 200 com tudo vazio e version nula")
    void blocoNovoVemVazio() throws Exception {
        String id = iniciar();
        mvc.perform(get("/api/questionarios/" + id + "/blocos/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.municipio").doesNotExist())
                .andExpect(jsonPath("$.version").doesNotExist())
                .andExpect(jsonPath("$.atualizadoEm").doesNotExist())
                .andExpect(jsonPath("$.id").doesNotExist());
    }

    @Test
    @DisplayName("Primeiro salvamento (version nula) cria o bloco com version 0 e o GET devolve os mesmos dados")
    void primeiroSalvamentoCria() throws Exception {
        String id = iniciar();
        mvc.perform(put("/api/questionarios/" + id + "/blocos/1").contentType(MediaType.APPLICATION_JSON)
                        .content(bloco1("  Nova Iguaçu  ", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.municipio").value("Nova Iguaçu"))
                .andExpect(jsonPath("$.version").value(0))
                .andExpect(jsonPath("$.atualizadoEm").exists())
                .andExpect(jsonPath("$.id").doesNotExist());

        mvc.perform(get("/api/questionarios/" + id + "/blocos/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.municipio").value("Nova Iguaçu"))
                .andExpect(jsonPath("$.secretario").value("Maria Souza"))
                .andExpect(jsonPath("$.email").value("joao@exemplo.gov.br"))
                .andExpect(jsonPath("$.telefone").value("(21) 99999-0000"))
                .andExpect(jsonPath("$.dataPreenchimento").value("2026-10-19"))
                .andExpect(jsonPath("$.version").value(0));
    }

    @Test
    @DisplayName("Salvar de novo com a versão atual sobe a versão, mesmo sem mudar nenhum texto")
    void salvarComVersaoAtualSobeAVersao() throws Exception {
        String id = iniciar();
        salvar(id, bloco1("Magé", null), 200);
        mvc.perform(put("/api/questionarios/" + id + "/blocos/1").contentType(MediaType.APPLICATION_JSON)
                        .content(bloco1("Magé", 0L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(1));
        mvc.perform(put("/api/questionarios/" + id + "/blocos/1").contentType(MediaType.APPLICATION_JSON)
                        .content(bloco1("Magé", 1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(2));
    }

    @Test
    @DisplayName("Versão velha: 409 VERSAO_DESATUALIZADA e o servidor mantém o que já estava salvo")
    void versaoVelhaDa409() throws Exception {
        String id = iniciar();
        salvar(id, bloco1("Japeri", null), 200);
        salvar(id, bloco1("Itaguaí", 0L), 200); // outra aba salvou: agora é a versão 1

        mvc.perform(put("/api/questionarios/" + id + "/blocos/1").contentType(MediaType.APPLICATION_JSON)
                        .content(bloco1("Queimados", 0L)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VERSAO_DESATUALIZADA"))
                .andExpect(jsonPath("$.message").exists());

        mvc.perform(get("/api/questionarios/" + id + "/blocos/1"))
                .andExpect(jsonPath("$.municipio").value("Itaguaí"))
                .andExpect(jsonPath("$.version").value(1));
    }

    @Test
    @DisplayName("Já existe e o cliente manda version nula (achava que era o primeiro salvamento): 409")
    void versaoNulaComBlocoExistenteDa409() throws Exception {
        String id = iniciar();
        salvar(id, bloco1("Japeri", null), 200);
        mvc.perform(put("/api/questionarios/" + id + "/blocos/1").contentType(MediaType.APPLICATION_JSON)
                        .content(bloco1("Outro", null)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VERSAO_DESATUALIZADA"));
    }

    @Test
    @DisplayName("Não existe e o cliente manda uma versão: 409 (o bloco não é criado a partir de versão inventada)")
    void versaoSemBlocoDa409() throws Exception {
        String id = iniciar();
        mvc.perform(put("/api/questionarios/" + id + "/blocos/1").contentType(MediaType.APPLICATION_JSON)
                        .content(bloco1("Japeri", 3L)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VERSAO_DESATUALIZADA"));
        mvc.perform(get("/api/questionarios/" + id + "/blocos/1"))
                .andExpect(jsonPath("$.version").doesNotExist());
    }

    @Test
    @DisplayName("Texto só com espaços vira campo vazio")
    void espacosViramVazio() throws Exception {
        String id = iniciar();
        mvc.perform(put("/api/questionarios/" + id + "/blocos/1").contentType(MediaType.APPLICATION_JSON)
                        .content(bloco1("     ", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.municipio").doesNotExist());
    }

    @Test
    @DisplayName("Rascunho aceita tudo vazio (D5): só formato e tamanho são validados")
    void rascunhoAceitaTudoVazio() throws Exception {
        String id = iniciar();
        mvc.perform(put("/api/questionarios/" + id + "/blocos/1").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(0));
    }

    @Test
    @DisplayName("Formato inválido: 400 com 'fields' por campo e nada é gravado")
    void formatoInvalidoDa400() throws Exception {
        String id = iniciar();
        String longo = "x".repeat(151);
        mvc.perform(put("/api/questionarios/" + id + "/blocos/1").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"municipio\":\"" + longo + "\",\"email\":\"sem-arroba\",\"telefone\":\"abc\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.municipio").value("Máximo de 150 caracteres"))
                .andExpect(jsonPath("$.fields.email").value("E-mail inválido"))
                .andExpect(jsonPath("$.fields.telefone").value("Use apenas números, espaços, parênteses, + e -"));

        mvc.perform(get("/api/questionarios/" + id + "/blocos/1"))
                .andExpect(jsonPath("$.version").doesNotExist());
    }

    @Test
    @DisplayName("Data fora do formato AAAA-MM-DD: 400 sem vazar detalhe do parser")
    void dataInvalidaDa400() throws Exception {
        String id = iniciar();
        mvc.perform(put("/api/questionarios/" + id + "/blocos/1").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dataPreenchimento\":\"19/10/2026\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Corpo da requisição ausente ou malformado"));
    }

    @Test
    @DisplayName("Questionário enviado não aceita gravação (409 QUESTIONARIO_ENVIADO), mas continua legível")
    void enviadoNaoAceitaGravacao() throws Exception {
        String id = iniciar();
        salvar(id, bloco1("Mesquita", null), 200);

        Questionario questionario = repository.findByPublicId(id).orElseThrow();
        questionario.marcarComoEnviado();
        repository.save(questionario);

        mvc.perform(put("/api/questionarios/" + id + "/blocos/1").contentType(MediaType.APPLICATION_JSON)
                        .content(bloco1("Alterado depois do envio", 0L)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("QUESTIONARIO_ENVIADO"));

        mvc.perform(get("/api/questionarios/" + id + "/blocos/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.municipio").value("Mesquita"));
    }

    @Test
    @DisplayName("Link inexistente ou mal formado: 404 na leitura e na gravação")
    void linkInvalidoDa404() throws Exception {
        for (String id : new String[]{"00000000-0000-0000-0000-000000000000", "abc", "1"}) {
            mvc.perform(get("/api/questionarios/" + id + "/blocos/1")).andExpect(status().isNotFound());
            salvar(id, bloco1("Qualquer", null), 404);
        }
    }

    @Test
    @DisplayName("Cada questionário tem o seu bloco: gravar em um não aparece no outro")
    void questionariosSaoIsolados() throws Exception {
        String a = iniciar();
        String b = iniciar();
        salvar(a, bloco1("Belford Roxo", null), 200);

        mvc.perform(get("/api/questionarios/" + b + "/blocos/1"))
                .andExpect(jsonPath("$.municipio").doesNotExist())
                .andExpect(jsonPath("$.version").doesNotExist());
    }

    @Test
    @DisplayName("GET do questionário traz a situação dos 8 blocos: só o Bloco 1 passa a ter versão depois de salvo")
    void situacaoDosBlocos() throws Exception {
        String id = iniciar();
        mvc.perform(get("/api/questionarios/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.blocos.length()").value(8))
                .andExpect(jsonPath("$.blocos[0].numero").value(1))
                .andExpect(jsonPath("$.blocos[0].version").doesNotExist())
                .andExpect(jsonPath("$.blocos[7].numero").value(8));

        salvar(id, bloco1("Paracambi", null), 200);

        mvc.perform(get("/api/questionarios/" + id))
                .andExpect(jsonPath("$.blocos[0].version").value(0))
                .andExpect(jsonPath("$.blocos[0].atualizadoEm").exists())
                .andExpect(jsonPath("$.blocos[1].version").doesNotExist());
    }
}
