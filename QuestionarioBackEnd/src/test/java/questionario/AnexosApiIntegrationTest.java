package questionario;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** E12: anexos do 8.2 (limites, formatos, bloqueio depois do envio, download, ligação com o bloco 8 e com a revisão). */
@SpringBootTest
@ActiveProfiles("test")
class AnexosApiIntegrationTest {

    private static final Pattern PUBLIC_ID = Pattern.compile("\"publicId\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern ID = Pattern.compile("\"id\"\\s*:\\s*(\\d+)");
    private static final byte[] PDF = "%PDF-1.4\nconteudo de teste\n%%EOF".getBytes(StandardCharsets.ISO_8859_1);

    @Autowired
    WebApplicationContext context;
    @Autowired
    FilterChainProxy seguranca;
    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(seguranca).build();
    }

    private String iniciar() throws Exception {
        String corpo = mvc.perform(post("/api/questionarios")).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Matcher m = PUBLIC_ID.matcher(corpo);
        assertThat(m.find()).isTrue();
        return m.group(1);
    }

    private ResultActions enviarArquivo(String q, String documento, String nome, byte[] bytes) throws Exception {
        return mvc.perform(multipart("/api/questionarios/" + q + "/anexos")
                .file(new MockMultipartFile("arquivo", nome, "application/octet-stream", bytes))
                .param("documento", documento));
    }

    private long idDe(ResultActions r) throws Exception {
        Matcher m = ID.matcher(r.andReturn().getResponse().getContentAsString());
        assertThat(m.find()).isTrue();
        return Long.parseLong(m.group(1));
    }

    @Test
    @DisplayName("A coluna dos bytes do anexo é longblob (no MySQL o padrão seria tinyblob, de 255 bytes)")
    void colunaDosBytesComporta10Mb() throws Exception {
        var coluna = questionario.domain.AnexoConteudo.class.getDeclaredField("dados").getAnnotation(jakarta.persistence.Column.class);
        assertThat(coluna.columnDefinition()).isEqualToIgnoringCase("longblob");
    }

    @Test
    @DisplayName("Arquivo de vários MB sobe e desce com os mesmos bytes")
    void arquivoGrande() throws Exception {
        String q = iniciar();
        byte[] meio = new byte[3 * 1024 * 1024];
        new java.util.Random(7).nextBytes(meio);
        System.arraycopy(PDF, 0, meio, 0, 5);
        long id = idDe(enviarArquivo(q, "PLANO_MUNICIPAL_PAS", "grande.pdf", meio).andExpect(status().isCreated()));
        byte[] volta = mvc.perform(get("/api/questionarios/" + q + "/anexos/" + id + "/arquivo")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(volta).isEqualTo(meio);
    }

    @Test
    @DisplayName("Envia, lista, baixa (mesmos bytes, attachment, nosniff) e remove")
    void ciclo() throws Exception {
        String q = iniciar();
        ResultActions criado = enviarArquivo(q, "PLANO_MUNICIPAL_PAS", "Plano 2026.pdf", PDF)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.documento").value("PLANO_MUNICIPAL_PAS"))
                .andExpect(jsonPath("$.nome").value("Plano 2026.pdf"))
                .andExpect(jsonPath("$.tamanho").value(PDF.length));
        long id = idDe(criado);

        mvc.perform(get("/api/questionarios/" + q + "/anexos")).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].id").value(id));
        byte[] baixado = mvc.perform(get("/api/questionarios/" + q + "/anexos/" + id + "/arquivo")).andExpect(status().isOk())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, org.hamcrest.Matchers.startsWith("attachment")))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "application/pdf"))
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(baixado).isEqualTo(PDF);

        mvc.perform(delete("/api/questionarios/" + q + "/anexos/" + id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/questionarios/" + q + "/anexos")).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/questionarios/" + q + "/anexos/" + id + "/arquivo")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("No máximo 3 arquivos por documento; outro documento tem seu próprio limite; remover libera vaga")
    void limitePorDocumento() throws Exception {
        String q = iniciar();
        long primeiro = 0;
        for (int i = 1; i <= 3; i++) {
            ResultActions r = enviarArquivo(q, "RELATORIO_FILA_REGULACAO", "fila" + i + ".pdf", PDF).andExpect(status().isCreated());
            if (i == 1) primeiro = idDe(r);
        }
        enviarArquivo(q, "RELATORIO_FILA_REGULACAO", "fila4.pdf", PDF).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.arquivo").value(org.hamcrest.Matchers.containsString("máximo")));
        enviarArquivo(q, "RELATORIO_PERMANENCIA_UPA", "upa.pdf", PDF).andExpect(status().isCreated());
        mvc.perform(delete("/api/questionarios/" + q + "/anexos/" + primeiro)).andExpect(status().isNoContent());
        enviarArquivo(q, "RELATORIO_FILA_REGULACAO", "fila5.pdf", PDF).andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Recusa: arquivo vazio, acima de 10 MB, formato fora da lista, conteúdo que não bate com a extensão, documento inventado")
    void recusas() throws Exception {
        String q = iniciar();
        enviarArquivo(q, "PLANO_MUNICIPAL_PAS", "vazio.pdf", new byte[0]).andExpect(status().isBadRequest());
        enviarArquivo(q, "PLANO_MUNICIPAL_PAS", "grande.pdf", grande()).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.arquivo").value(org.hamcrest.Matchers.containsString("10 MB")));
        enviarArquivo(q, "PLANO_MUNICIPAL_PAS", "programa.exe", PDF).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.arquivo").value(org.hamcrest.Matchers.containsString("Formato")));
        enviarArquivo(q, "PLANO_MUNICIPAL_PAS", "falso.pdf", "isto nao e um pdf".getBytes(StandardCharsets.UTF_8))
                .andExpect(status().isBadRequest());
        enviarArquivo(q, "PLANO_MUNICIPAL_PAS", "binario.csv", new byte[]{'a', ',', 0, 'b'}).andExpect(status().isBadRequest());
        enviarArquivo(q, "NAO_EXISTE", "a.pdf", PDF).andExpect(status().isBadRequest());
        enviarArquivo(q, "PLANO_MUNICIPAL_PAS", "sem-extensao", PDF).andExpect(status().isBadRequest());
        mvc.perform(post("/api/questionarios/" + q + "/anexos").contentType(MediaType.MULTIPART_FORM_DATA)
                .param("documento", "PLANO_MUNICIPAL_PAS")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/questionarios/" + q + "/anexos")).andExpect(jsonPath("$.length()").value(0));
    }

    private static byte[] grande() {
        byte[] b = new byte[10 * 1024 * 1024 + 1];
        System.arraycopy(PDF, 0, b, 0, PDF.length);
        return b;
    }

    @Test
    @DisplayName("Aceita CSV, Excel/Word (zip), imagem e DOC/XLS antigos pelos primeiros bytes; o nome é limpo de pastas")
    void formatos() throws Exception {
        String q = iniciar();
        enviarArquivo(q, "ESTUDOS_DEMANDA_REPRIMIDA", "dados.csv", "a;b\n1;2\n".getBytes(StandardCharsets.UTF_8)).andExpect(status().isCreated());
        enviarArquivo(q, "ESTUDOS_DEMANDA_REPRIMIDA", "planilha.xlsx", new byte[]{'P', 'K', 3, 4, 1, 2}).andExpect(status().isCreated());
        enviarArquivo(q, "ESTUDOS_DEMANDA_REPRIMIDA", "foto.PNG", new byte[]{(byte) 0x89, 'P', 'N', 'G', 1}).andExpect(status().isCreated());
        enviarArquivo(q, "CONTRATOS_HOSPITAIS_PRIVADOS", "antigo.xls", new byte[]{(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0, 1}).andExpect(status().isCreated());
        enviarArquivo(q, "CONTRATOS_HOSPITAIS_PRIVADOS", "foto.jpg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 1}).andExpect(status().isCreated());
        enviarArquivo(q, "CONTRATOS_HOSPITAIS_PRIVADOS", "..\\..\\etc/passwd.pdf", PDF).andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("passwd.pdf"));
    }

    @Test
    @DisplayName("Anexo de outro questionário não é visto nem baixado nem apagado por este link")
    void isolamento() throws Exception {
        String a = iniciar(), b = iniciar();
        long id = idDe(enviarArquivo(a, "PLANO_MUNICIPAL_PAS", "a.pdf", PDF).andExpect(status().isCreated()));
        mvc.perform(get("/api/questionarios/" + b + "/anexos")).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/questionarios/" + b + "/anexos/" + id + "/arquivo")).andExpect(status().isNotFound());
        mvc.perform(delete("/api/questionarios/" + b + "/anexos/" + id)).andExpect(status().isNotFound());
        mvc.perform(get("/api/questionarios/" + a + "/anexos/" + id + "/arquivo")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Documento com arquivo anexado continua marcado no 8.2 mesmo se o bloco for salvo sem ele; aparece na revisão")
    void ligadoAoBloco8() throws Exception {
        String q = iniciar();
        enviarArquivo(q, "RELATORIO_FILA_REGULACAO", "fila.pdf", PDF).andExpect(status().isCreated());
        String b8 = "{\"observacoes\":\"x\",\"documentos\":[\"PLANO_MUNICIPAL_PAS\"],\"assinaturaNome\":\"M\",\"assinaturaData\":\"2026-03-20\",\"declaracao\":true,\"version\":null}";
        mvc.perform(put("/api/questionarios/" + q + "/blocos/8").contentType(MediaType.APPLICATION_JSON).content(b8))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentos.length()").value(2))
                .andExpect(jsonPath("$.documentos", org.hamcrest.Matchers.hasItems("PLANO_MUNICIPAL_PAS", "RELATORIO_FILA_REGULACAO")));
        mvc.perform(get("/api/questionarios/" + q + "/respostas")).andExpect(status().isOk())
                .andExpect(jsonPath("$.anexos.length()").value(1))
                .andExpect(jsonPath("$.anexos[0].nome").value("fila.pdf"));
    }

    @Test
    @DisplayName("Planilha de coleta (Bloco 1): só planilha, um arquivo só (o novo troca o antigo), fora do 8.2 e do limite de 3")
    void planilhaDeColeta() throws Exception {
        String q = iniciar();
        byte[] xlsx = new byte[]{'P', 'K', 3, 4, 9, 9};
        enviarArquivo(q, "PLANILHA_COLETA", "relatorio.pdf", PDF).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.arquivo").value(org.hamcrest.Matchers.containsString("planilha")));
        enviarArquivo(q, "PLANILHA_COLETA", "dados.csv", "a;b".getBytes(StandardCharsets.UTF_8)).andExpect(status().isBadRequest());
        enviarArquivo(q, "PLANILHA_COLETA", "falsa.xlsx", "texto".getBytes(StandardCharsets.UTF_8)).andExpect(status().isBadRequest());
        long primeira = idDe(enviarArquivo(q, "PLANILHA_COLETA", "Planilha Japeri.xlsx", xlsx).andExpect(status().isCreated()));
        long segunda = idDe(enviarArquivo(q, "PLANILHA_COLETA", "Planilha Japeri v2.xlsx", xlsx).andExpect(status().isCreated()));
        mvc.perform(get("/api/questionarios/" + q + "/anexos")).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(segunda)).andExpect(jsonPath("$[0].documento").value("PLANILHA_COLETA"));
        mvc.perform(get("/api/questionarios/" + q + "/anexos/" + primeira + "/arquivo")).andExpect(status().isNotFound());
        mvc.perform(get("/api/questionarios/" + q + "/anexos/" + segunda + "/arquivo")).andExpect(status().isOk());
        // não conta nos 3 do 8.2 nem marca nada no bloco 8
        for (int i = 1; i <= 3; i++) enviarArquivo(q, "PLANO_MUNICIPAL_PAS", "p" + i + ".pdf", PDF).andExpect(status().isCreated());
        String b8 = "{\"observacoes\":\"x\",\"documentos\":[],\"assinaturaNome\":\"M\",\"assinaturaData\":\"2026-03-20\",\"declaracao\":true,\"version\":null}";
        mvc.perform(put("/api/questionarios/" + q + "/blocos/8").contentType(MediaType.APPLICATION_JSON).content(b8))
                .andExpect(status().isOk()).andExpect(jsonPath("$.documentos.length()").value(1))
                .andExpect(jsonPath("$.documentos[0]").value("PLANO_MUNICIPAL_PAS"));
        // o cliente não consegue "marcar" a planilha como documento do 8.2
        String b8b = b8.replace("\"documentos\":[]", "\"documentos\":[\"PLANILHA_COLETA\",\"PLANO_MUNICIPAL_PAS\"]").replace("\"version\":null", "\"version\":0");
        mvc.perform(put("/api/questionarios/" + q + "/blocos/8").contentType(MediaType.APPLICATION_JSON).content(b8b))
                .andExpect(status().isOk()).andExpect(jsonPath("$.documentos.length()").value(1));
    }

    @Test
    @DisplayName("Depois do envio não se anexa nem se remove (409), mas ainda se baixa")
    void bloqueadoDepoisDoEnvio() throws Exception {
        String q = iniciar();
        long id = idDe(enviarArquivo(q, "PLANO_MUNICIPAL_PAS", "plano.pdf", PDF).andExpect(status().isCreated()));
        mvc.perform(put("/api/questionarios/" + q + "/blocos/1").contentType(MediaType.APPLICATION_JSON)
                .content(Blocos3a8ApiIntegrationTest.B1)).andExpect(status().isOk());
        for (int b = 2; b <= 8; b++) {
            String corpo = new String[]{null, null, Blocos3a8ApiIntegrationTest.B2, Blocos3a8ApiIntegrationTest.B3, Blocos3a8ApiIntegrationTest.B4,
                    Blocos3a8ApiIntegrationTest.B5, Blocos3a8ApiIntegrationTest.B6, Blocos3a8ApiIntegrationTest.B7, Blocos3a8ApiIntegrationTest.B8}[b];
            mvc.perform(put("/api/questionarios/" + q + "/blocos/" + b).contentType(MediaType.APPLICATION_JSON).content(corpo)).andExpect(status().isOk());
        }
        mvc.perform(post("/api/questionarios/" + q + "/enviar")).andExpect(status().isOk());
        enviarArquivo(q, "RELATORIO_FILA_REGULACAO", "novo.pdf", PDF).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("QUESTIONARIO_ENVIADO"));
        mvc.perform(delete("/api/questionarios/" + q + "/anexos/" + id)).andExpect(status().isConflict());
        mvc.perform(get("/api/questionarios/" + q + "/anexos/" + id + "/arquivo")).andExpect(status().isOk());
    }
}
