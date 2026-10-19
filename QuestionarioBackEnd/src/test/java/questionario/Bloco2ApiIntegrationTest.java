package questionario;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
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

/**
 * Bloco 2 (prioridades): leitura, gravação com versão (D6), as regras do documento (até 3 impedimentos, ordem sem
 * repetição, nota de 1 a 5, "Outra/Outro" com descrição) e bloqueio depois do envio (D12).
 */
@SpringBootTest
@ActiveProfiles("test")
class Bloco2ApiIntegrationTest {

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

    private static final String PRIORIDADES = "\"prioridades\":["
            + "{\"titulo\":\"UTI neonatal\",\"problema\":\"Mortalidade infantil\",\"resolveSozinho\":\"NAO\"},"
            + "{\"titulo\":\"  Imagem  \",\"problema\":\"Fila de tomografia\",\"resolveSozinho\":\"EM_PARTE\"},"
            + "{\"titulo\":\"Saúde mental\",\"problema\":null,\"resolveSozinho\":\"SIM\"}]";

    /** Bloco 2 completo e válido; {@code extra} substitui/acrescenta campos já escritos em JSON (ex.: impedimentos). */
    private static String bloco2(Long version) {
        return "{" + PRIORIDADES + ","
                + "\"grausLinhaCuidado\":{\"CARDIOVASCULAR_AVC\":5,\"ONCOLOGIA\":3,\"OUTRA\":4},"
                + "\"linhaOutraDescricao\":\"Hemodiálise pediátrica\","
                + "\"gargalos\":\"Ressonância e cirurgia cardíaca\","
                + "\"servicosParaVizinhos\":\"Centro de imagem com horário ocioso\","
                + "\"impedimentos\":[\"FINANCIAMENTO_TETO_MAC\",\"FALTA_PROFISSIONAIS\",\"OUTRO\"],"
                + "\"impedimentoOutro\":\"Terreno para obra\","
                + "\"ordemTemas\":{\"FINANCIAMENTO\":1,\"GOVERNANCA_PACTUACAO\":2,\"ORGANIZACAO_REDE\":3,"
                + "\"TRANSPORTE_REGULACAO\":4,\"PROFISSIONAIS\":5,\"MONITORAMENTO_AVALIACAO\":6},"
                + "\"version\":" + version + "}";
    }

    private static String bloco2Com(String campos, Long version) {
        return "{" + campos + ",\"version\":" + version + "}";
    }

    private ResultActions enviar(String id, String corpo) throws Exception {
        return mvc.perform(put("/api/questionarios/" + id + "/blocos/2").contentType(MediaType.APPLICATION_JSON)
                .content(corpo));
    }

    private void salvar(String id, String corpo, int statusEsperado) throws Exception {
        enviar(id, corpo).andExpect(status().is(statusEsperado));
    }

    @Test
    @DisplayName("Bloco ainda não salvo: 200, 5 linhas de prioridade vazias e version nula")
    void blocoNovoVemVazio() throws Exception {
        String id = iniciar();
        mvc.perform(get("/api/questionarios/" + id + "/blocos/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prioridades.length()").value(5))
                .andExpect(jsonPath("$.prioridades[0].titulo").doesNotExist())
                .andExpect(jsonPath("$.prioridades[4].resolveSozinho").doesNotExist())
                .andExpect(jsonPath("$.impedimentos.length()").value(0))
                .andExpect(jsonPath("$.gargalos").doesNotExist())
                .andExpect(jsonPath("$.version").doesNotExist())
                .andExpect(jsonPath("$.atualizadoEm").doesNotExist())
                .andExpect(jsonPath("$.id").doesNotExist());
    }

    @Test
    @DisplayName("Primeiro salvamento cria o bloco com version 0 e o GET devolve tudo que foi gravado")
    void primeiroSalvamentoCria() throws Exception {
        String id = iniciar();
        enviar(id, bloco2(null))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(0))
                .andExpect(jsonPath("$.atualizadoEm").exists())
                .andExpect(jsonPath("$.id").doesNotExist());

        mvc.perform(get("/api/questionarios/" + id + "/blocos/2"))
                .andExpect(status().isOk())
                // 2.1: sempre 5 linhas, na ordem enviada; as 2 últimas vêm vazias; o texto é aparado
                .andExpect(jsonPath("$.prioridades.length()").value(5))
                .andExpect(jsonPath("$.prioridades[0].titulo").value("UTI neonatal"))
                .andExpect(jsonPath("$.prioridades[0].resolveSozinho").value("NAO"))
                .andExpect(jsonPath("$.prioridades[1].titulo").value("Imagem"))
                .andExpect(jsonPath("$.prioridades[1].resolveSozinho").value("EM_PARTE"))
                .andExpect(jsonPath("$.prioridades[2].problema").doesNotExist())
                .andExpect(jsonPath("$.prioridades[3].titulo").doesNotExist())
                .andExpect(jsonPath("$.prioridades[4].titulo").doesNotExist())
                // 2.2
                .andExpect(jsonPath("$.grausLinhaCuidado.CARDIOVASCULAR_AVC").value(5))
                .andExpect(jsonPath("$.grausLinhaCuidado.ONCOLOGIA").value(3))
                .andExpect(jsonPath("$.grausLinhaCuidado.OUTRA").value(4))
                .andExpect(jsonPath("$.grausLinhaCuidado.QUEIMADOS").doesNotExist())
                .andExpect(jsonPath("$.linhaOutraDescricao").value("Hemodiálise pediátrica"))
                // 2.3 e 2.4
                .andExpect(jsonPath("$.gargalos").value("Ressonância e cirurgia cardíaca"))
                .andExpect(jsonPath("$.servicosParaVizinhos").value("Centro de imagem com horário ocioso"))
                // 2.5: na ordem do documento, não na ordem enviada
                .andExpect(jsonPath("$.impedimentos.length()").value(3))
                .andExpect(jsonPath("$.impedimentos[0]").value("FINANCIAMENTO_TETO_MAC"))
                .andExpect(jsonPath("$.impedimentos[1]").value("FALTA_PROFISSIONAIS"))
                .andExpect(jsonPath("$.impedimentos[2]").value("OUTRO"))
                .andExpect(jsonPath("$.impedimentoOutro").value("Terreno para obra"))
                // 2.6
                .andExpect(jsonPath("$.ordemTemas.FINANCIAMENTO").value(1))
                .andExpect(jsonPath("$.ordemTemas.GOVERNANCA_PACTUACAO").value(2))
                .andExpect(jsonPath("$.ordemTemas.MONITORAMENTO_AVALIACAO").value(6))
                .andExpect(jsonPath("$.version").value(0));
    }

    @Test
    @DisplayName("Salvar de novo substitui tudo: nota removida, impedimento trocado e linha de prioridade esvaziada somem")
    void salvarSubstituiTudo() throws Exception {
        String id = iniciar();
        salvar(id, bloco2(null), 200);

        enviar(id, bloco2Com("\"prioridades\":[{\"titulo\":\"Só esta\"}],"
                + "\"grausLinhaCuidado\":{\"ONCOLOGIA\":1,\"QUEIMADOS\":null},"
                + "\"impedimentos\":[\"TRANSPORTE_DISTANCIA\"],"
                + "\"ordemTemas\":{}", 0L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.prioridades.length()").value(5))
                .andExpect(jsonPath("$.prioridades[0].titulo").value("Só esta"))
                .andExpect(jsonPath("$.prioridades[1].titulo").doesNotExist())
                .andExpect(jsonPath("$.grausLinhaCuidado.ONCOLOGIA").value(1))
                .andExpect(jsonPath("$.grausLinhaCuidado.CARDIOVASCULAR_AVC").doesNotExist())
                .andExpect(jsonPath("$.grausLinhaCuidado.OUTRA").doesNotExist())
                .andExpect(jsonPath("$.linhaOutraDescricao").doesNotExist())
                .andExpect(jsonPath("$.gargalos").doesNotExist())
                .andExpect(jsonPath("$.impedimentos.length()").value(1))
                .andExpect(jsonPath("$.impedimentos[0]").value("TRANSPORTE_DISTANCIA"))
                .andExpect(jsonPath("$.ordemTemas.FINANCIAMENTO").doesNotExist());

        mvc.perform(get("/api/questionarios/" + id + "/blocos/2"))
                .andExpect(jsonPath("$.prioridades[1].titulo").doesNotExist())
                .andExpect(jsonPath("$.grausLinhaCuidado.CARDIOVASCULAR_AVC").doesNotExist())
                .andExpect(jsonPath("$.impedimentos.length()").value(1))
                .andExpect(jsonPath("$.version").value(1));
    }

    @Test
    @DisplayName("Salvar com a versão atual sobe a versão, mesmo sem mudar nada")
    void salvarComVersaoAtualSobeAVersao() throws Exception {
        String id = iniciar();
        salvar(id, bloco2(null), 200);
        enviar(id, bloco2(0L)).andExpect(status().isOk()).andExpect(jsonPath("$.version").value(1));
        enviar(id, bloco2(1L)).andExpect(status().isOk()).andExpect(jsonPath("$.version").value(2));
    }

    @Test
    @DisplayName("Versão velha: 409 VERSAO_DESATUALIZADA e o servidor mantém o que já estava salvo")
    void versaoVelhaDa409() throws Exception {
        String id = iniciar();
        salvar(id, bloco2(null), 200);
        salvar(id, bloco2Com("\"gargalos\":\"da outra aba\"", 0L), 200); // agora é a versão 1

        enviar(id, bloco2Com("\"gargalos\":\"da aba velha\"", 0L))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VERSAO_DESATUALIZADA"))
                .andExpect(jsonPath("$.message").exists());

        mvc.perform(get("/api/questionarios/" + id + "/blocos/2"))
                .andExpect(jsonPath("$.gargalos").value("da outra aba"))
                .andExpect(jsonPath("$.version").value(1));
    }

    @Test
    @DisplayName("Version nula com bloco existente, ou versão inventada sem bloco: 409")
    void versaoIncoerenteDa409() throws Exception {
        String id = iniciar();
        enviar(id, bloco2(3L)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VERSAO_DESATUALIZADA"));
        mvc.perform(get("/api/questionarios/" + id + "/blocos/2")).andExpect(jsonPath("$.version").doesNotExist());

        salvar(id, bloco2(null), 200);
        enviar(id, bloco2(null)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VERSAO_DESATUALIZADA"));
    }

    @Test
    @DisplayName("Rascunho aceita tudo vazio (D5)")
    void rascunhoAceitaTudoVazio() throws Exception {
        String id = iniciar();
        mvc.perform(put("/api/questionarios/" + id + "/blocos/2").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(0))
                .andExpect(jsonPath("$.prioridades.length()").value(5));
    }

    @Test
    @DisplayName("2.5: até 3 impedimentos passam; 4 dão 400 e nada é gravado")
    void limiteDeImpedimentos() throws Exception {
        String id = iniciar();
        enviar(id, bloco2Com("\"impedimentos\":[\"FINANCIAMENTO_TETO_MAC\",\"FALTA_PROFISSIONAIS\","
                + "\"REGULACAO_FRAGMENTADA\"]", null))
                .andExpect(status().isOk());

        enviar(id, bloco2Com("\"impedimentos\":[\"FINANCIAMENTO_TETO_MAC\",\"FALTA_PROFISSIONAIS\","
                + "\"REGULACAO_FRAGMENTADA\",\"TRANSPORTE_DISTANCIA\"]", 0L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Dados inválidos"))
                .andExpect(jsonPath("$.fields.impedimentos").value("Marque no máximo 3 opções"));

        mvc.perform(get("/api/questionarios/" + id + "/blocos/2"))
                .andExpect(jsonPath("$.impedimentos.length()").value(3))
                .andExpect(jsonPath("$.version").value(0));
    }

    @Test
    @DisplayName("2.5: a mesma opção repetida no JSON conta uma vez só")
    void impedimentoRepetidoContaUmaVez() throws Exception {
        String id = iniciar();
        enviar(id, bloco2Com("\"impedimentos\":[\"FALTA_PROFISSIONAIS\",\"FALTA_PROFISSIONAIS\","
                + "\"FALTA_PROFISSIONAIS\",\"FALTA_PROFISSIONAIS\"]", null))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.impedimentos.length()").value(1));
    }

    @Test
    @DisplayName("2.5: 'Outro' marcado sem descrição dá 400; sem a marca, a descrição é descartada")
    void impedimentoOutro() throws Exception {
        String id = iniciar();
        enviar(id, bloco2Com("\"impedimentos\":[\"OUTRO\"],\"impedimentoOutro\":\"   \"", null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.impedimentoOutro").value("Descreva o outro impedimento"));

        enviar(id, bloco2Com("\"impedimentos\":[\"FALTA_PROFISSIONAIS\"],\"impedimentoOutro\":\"sobrou\"", null))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.impedimentoOutro").doesNotExist());
    }

    @Test
    @DisplayName("2.5: opção que não existe no documento dá 400")
    void impedimentoDesconhecido() throws Exception {
        String id = iniciar();
        enviar(id, bloco2Com("\"impedimentos\":[\"INVENTADO\"]", null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Corpo da requisição ausente ou malformado"));
    }

    @Test
    @DisplayName("2.2: nota fora de 1 a 5 dá 400 apontando a linha; 'Outra' com nota exige descrição")
    void notasDasLinhasDeCuidado() throws Exception {
        String id = iniciar();
        enviar(id, bloco2Com("\"grausLinhaCuidado\":{\"ONCOLOGIA\":0,\"QUEIMADOS\":6,\"UTI_ADULTO\":5}", null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields['grausLinhaCuidado.ONCOLOGIA']").value("Use uma nota de 1 a 5"))
                .andExpect(jsonPath("$.fields['grausLinhaCuidado.QUEIMADOS']").value("Use uma nota de 1 a 5"))
                .andExpect(jsonPath("$.fields['grausLinhaCuidado.UTI_ADULTO']").doesNotExist());

        enviar(id, bloco2Com("\"grausLinhaCuidado\":{\"OUTRA\":3}", null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.linhaOutraDescricao").value("Descreva a outra linha de cuidado"));

        // descrição sozinha (sem nota) é rascunho válido
        enviar(id, bloco2Com("\"linhaOutraDescricao\":\"Hemodiálise\"", null)).andExpect(status().isOk());
    }

    @Test
    @DisplayName("2.2: linha de cuidado que não existe no documento dá 400")
    void linhaDesconhecida() throws Exception {
        String id = iniciar();
        enviar(id, bloco2Com("\"grausLinhaCuidado\":{\"INVENTADA\":3}", null)).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("2.6: ordem completa válida passa; posição repetida ou fora de 1 a 6 dá 400")
    void ordemDosTemas() throws Exception {
        String id = iniciar();
        salvar(id, bloco2Com("\"ordemTemas\":{\"FINANCIAMENTO\":1,\"GOVERNANCA_PACTUACAO\":2,\"ORGANIZACAO_REDE\":3,"
                + "\"TRANSPORTE_REGULACAO\":4,\"PROFISSIONAIS\":5,\"MONITORAMENTO_AVALIACAO\":6}", null), 200);

        enviar(id, bloco2Com("\"ordemTemas\":{\"FINANCIAMENTO\":1,\"GOVERNANCA_PACTUACAO\":1}", 0L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.ordemTemas").value("Cada posição só pode ser usada uma vez"));

        enviar(id, bloco2Com("\"ordemTemas\":{\"FINANCIAMENTO\":7}", 0L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.ordemTemas").value("Use posições de 1 a 6"));
        enviar(id, bloco2Com("\"ordemTemas\":{\"FINANCIAMENTO\":0}", 0L)).andExpect(status().isBadRequest());

        mvc.perform(get("/api/questionarios/" + id + "/blocos/2"))
                .andExpect(jsonPath("$.ordemTemas.FINANCIAMENTO").value(1))
                .andExpect(jsonPath("$.version").value(0));
    }

    @Test
    @DisplayName("2.6: ordem parcial é aceita no rascunho (D5)")
    void ordemParcialNoRascunho() throws Exception {
        String id = iniciar();
        enviar(id, bloco2Com("\"ordemTemas\":{\"FINANCIAMENTO\":1,\"PROFISSIONAIS\":2}", null))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ordemTemas.PROFISSIONAIS").value(2))
                .andExpect(jsonPath("$.ordemTemas.GOVERNANCA_PACTUACAO").doesNotExist());
    }

    @Test
    @DisplayName("2.1: mais de 5 linhas e textos longos dão 400 com o campo certo; resolveSozinho desconhecido dá 400")
    void formatoDasPrioridades() throws Exception {
        String id = iniciar();
        String seis = "{\"titulo\":\"x\"},".repeat(6);
        enviar(id, bloco2Com("\"prioridades\":[" + seis.substring(0, seis.length() - 1) + "]", null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.prioridades").value("Informe no máximo 5 prioridades"));

        enviar(id, bloco2Com("\"prioridades\":[{\"titulo\":\"" + "x".repeat(301) + "\",\"problema\":\""
                + "y".repeat(501) + "\"}],\"gargalos\":\"" + "z".repeat(5001) + "\"", null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields['prioridades[0].titulo']").value("Máximo de 300 caracteres"))
                .andExpect(jsonPath("$.fields['prioridades[0].problema']").value("Máximo de 500 caracteres"))
                .andExpect(jsonPath("$.fields.gargalos").value("Máximo de 5000 caracteres"));

        enviar(id, bloco2Com("\"prioridades\":[{\"resolveSozinho\":\"TALVEZ\"}]", null))
                .andExpect(status().isBadRequest());

        mvc.perform(get("/api/questionarios/" + id + "/blocos/2")).andExpect(jsonPath("$.version").doesNotExist());
    }

    @Test
    @DisplayName("Questionário enviado não aceita gravação (409 QUESTIONARIO_ENVIADO), mas continua legível")
    void enviadoNaoAceitaGravacao() throws Exception {
        String id = iniciar();
        salvar(id, bloco2(null), 200);

        Questionario questionario = repository.findByPublicId(id).orElseThrow();
        questionario.marcarComoEnviado();
        repository.save(questionario);

        enviar(id, bloco2Com("\"gargalos\":\"depois do envio\"", 0L))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("QUESTIONARIO_ENVIADO"));

        mvc.perform(get("/api/questionarios/" + id + "/blocos/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gargalos").value("Ressonância e cirurgia cardíaca"));
    }

    @Test
    @DisplayName("Link inexistente ou mal formado: 404 na leitura e na gravação")
    void linkInvalidoDa404() throws Exception {
        for (String id : new String[]{"00000000-0000-0000-0000-000000000000", "abc", "1"}) {
            mvc.perform(get("/api/questionarios/" + id + "/blocos/2")).andExpect(status().isNotFound());
            salvar(id, bloco2(null), 404);
        }
    }

    @Test
    @DisplayName("Cada questionário tem o seu bloco, e o Bloco 2 é independente do Bloco 1")
    void questionariosEBlocosSaoIsolados() throws Exception {
        String a = iniciar();
        String b = iniciar();
        salvar(a, bloco2(null), 200);

        mvc.perform(get("/api/questionarios/" + b + "/blocos/2"))
                .andExpect(jsonPath("$.gargalos").doesNotExist())
                .andExpect(jsonPath("$.version").doesNotExist());
        mvc.perform(get("/api/questionarios/" + a + "/blocos/1"))
                .andExpect(jsonPath("$.version").doesNotExist());
    }

    @Test
    @DisplayName("GET do questionário: o Bloco 2 passa a ter versão depois de salvo; os outros blocos seguem sem")
    void situacaoDosBlocos() throws Exception {
        String id = iniciar();
        mvc.perform(get("/api/questionarios/" + id)).andExpect(jsonPath("$.blocos[1].version").doesNotExist());

        salvar(id, bloco2(null), 200);

        mvc.perform(get("/api/questionarios/" + id))
                .andExpect(jsonPath("$.blocos.length()").value(8))
                .andExpect(jsonPath("$.blocos[1].numero").value(2))
                .andExpect(jsonPath("$.blocos[1].version").value(0))
                .andExpect(jsonPath("$.blocos[1].atualizadoEm").exists())
                .andExpect(jsonPath("$.blocos[0].version").doesNotExist())
                .andExpect(jsonPath("$.blocos[2].version").doesNotExist());
    }
}
