package questionario;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.io.ByteArrayInputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** E11: acesso (publico x admin), login, lista, visao completa e Excel. Usa a cadeia de filtros do Spring Security. */
@SpringBootTest
@ActiveProfiles("test")
class AdminApiIntegrationTest {

    private static final Pattern PUBLIC_ID = Pattern.compile("\"publicId\"\\s*:\\s*\"([^\"]+)\"");
    private static final String LOGIN_CERTO = "{\"email\":\"admin@teste.local\",\"senha\":\"senha-de-teste\"}";

    @Autowired
    WebApplicationContext context;
    @Autowired
    FilterChainProxy seguranca;

    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(seguranca).build();
    }

    // ---------- apoio ----------

    private String entrar() throws Exception {
        String cookie = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(LOGIN_CERTO))
                .andExpect(status().isOk()).andReturn().getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertThat(cookie).isNotNull();
        return cookie.substring(cookie.indexOf('=') + 1, cookie.indexOf(';'));
    }

    private ResultActions comToken(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder req, String token) throws Exception {
        return mvc.perform(req.header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }

    private String iniciar() throws Exception {
        String corpo = mvc.perform(post("/api/questionarios")).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Matcher m = PUBLIC_ID.matcher(corpo);
        assertThat(m.find()).isTrue();
        return m.group(1);
    }

    private void salvarBloco(String id, int bloco, String corpo) throws Exception {
        mvc.perform(put("/api/questionarios/" + id + "/blocos/" + bloco).contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isOk());
    }

    private void preencherTudo(String id, String municipio) throws Exception {
        salvarBloco(id, 1, Blocos3a8ApiIntegrationTest.B1.replace("Nova Iguaçu", municipio));
        salvarBloco(id, 2, Blocos3a8ApiIntegrationTest.B2);
        salvarBloco(id, 3, Blocos3a8ApiIntegrationTest.B3);
        salvarBloco(id, 4, Blocos3a8ApiIntegrationTest.B4);
        salvarBloco(id, 5, Blocos3a8ApiIntegrationTest.B5);
        salvarBloco(id, 6, Blocos3a8ApiIntegrationTest.B6);
        salvarBloco(id, 7, Blocos3a8ApiIntegrationTest.B7);
        salvarBloco(id, 8, Blocos3a8ApiIntegrationTest.B8);
    }

    private void enviar(String id) throws Exception {
        mvc.perform(post("/api/questionarios/" + id + "/enviar")).andExpect(status().isOk());
    }

    private static Sheet aba(Workbook wb, String nome) {
        Sheet sheet = wb.getSheet(nome);
        assertThat(sheet).as("aba '%s' (existem: %s)", nome, abas(wb)).isNotNull();
        return sheet;
    }

    private static String abas(Workbook wb) {
        StringBuilder sb = new StringBuilder();
        for (Sheet s : wb) sb.append(s.getSheetName()).append(" | ");
        return sb.toString();
    }

    private static String texto(Sheet s, int linha, int coluna) {
        var row = s.getRow(linha);
        var cell = row == null ? null : row.getCell(coluna);
        return cell == null ? "" : cell.getStringCellValue();
    }

    // ---------- acesso ----------

    @Test
    @DisplayName("Sem login: /api/admin/** e /api/auth/validate dão 401; o questionário segue público (abre, salva e envia)")
    void matrizDeAcesso() throws Exception {
        mvc.perform(get("/api/admin/questionarios")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/questionarios/qualquer")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/questionarios/excel")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/questionarios/qualquer/excel")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/validate")).andExpect(status().isUnauthorized());

        // regressão: o município continua sem login
        mvc.perform(get("/api/levantamentos/ativo")).andExpect(status().isOk());
        String id = iniciar();
        salvarBloco(id, 1, Blocos3a8ApiIntegrationTest.B1);
        mvc.perform(get("/api/questionarios/" + id + "/blocos/1")).andExpect(status().isOk());
        mvc.perform(get("/api/questionarios/" + id + "/pendencias")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Token inválido, adulterado ou sem o prefixo Bearer não abre a área do admin")
    void tokenRuim() throws Exception {
        String token = entrar();
        comToken(get("/api/admin/questionarios"), token + "x").andExpect(status().isUnauthorized());
        comToken(get("/api/admin/questionarios"), "lixo").andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/questionarios").header(HttpHeaders.AUTHORIZATION, token)).andExpect(status().isUnauthorized());
        comToken(get("/api/admin/questionarios"), token).andExpect(status().isOk());
    }

    // ---------- login ----------

    @Test
    @DisplayName("Login certo: 200, cookie auth_token HTTP-only; /validate responde com o token; logout apaga o cookie")
    void loginCerto() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(LOGIN_CERTO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("admin@teste.local"))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.startsWith("auth_token="),
                        org.hamcrest.Matchers.containsString("HttpOnly"),
                        org.hamcrest.Matchers.containsString("SameSite=Lax"),
                        org.hamcrest.Matchers.containsString("Path=/"))));
        comToken(get("/api/auth/validate"), entrar()).andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("admin@teste.local"));
        mvc.perform(post("/api/auth/logout")).andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("Max-Age=0")));
    }

    @Test
    @DisplayName("Login errado (senha ou e-mail): 401 com a mesma mensagem; campos em branco: 400")
    void loginErrado() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@teste.local\",\"senha\":\"errada\"}"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.message").value("E-mail ou senha inválidos"));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"outro@teste.local\",\"senha\":\"senha-de-teste\"}"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.message").value("E-mail ou senha inválidos"));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"\",\"senha\":\"\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.email").exists()).andExpect(jsonPath("$.fields.senha").exists());
        mvc.perform(post("/api/auth/login")).andExpect(status().isBadRequest());
    }

    // ---------- lista e visão completa ----------

    @Test
    @DisplayName("Lista: município repetido (ignora acento e caixa) e sem município aparecem destacados; filtros por status")
    void lista() throws Exception {
        String a = iniciar(), b = iniciar(), c = iniciar();
        salvarBloco(a, 1, "{\"municipio\":\"Duque de Caxias\",\"version\":null}");
        salvarBloco(b, 1, "{\"municipio\":\"  duque  de CAXIAS \",\"version\":null}");
        String token = entrar();

        comToken(get("/api/admin/questionarios"), token).andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.publicId=='" + a + "')].municipioRepetido").value(true))
                .andExpect(jsonPath("$[?(@.publicId=='" + a + "')].municipio").value("Duque de Caxias"))
                .andExpect(jsonPath("$[?(@.publicId=='" + b + "')].municipioRepetido").value(true))
                .andExpect(jsonPath("$[?(@.publicId=='" + b + "')].municipio").value("duque  de CAXIAS"))
                .andExpect(jsonPath("$[?(@.publicId=='" + c + "')].semMunicipio").value(true))
                .andExpect(jsonPath("$[?(@.publicId=='" + c + "')].blocosSalvos").value(0))
                .andExpect(jsonPath("$[?(@.publicId=='" + a + "')].blocosSalvos").value(1))
                .andExpect(jsonPath("$[?(@.publicId=='" + a + "')].totalBlocos").value(8))
                .andExpect(jsonPath("$[?(@.publicId=='" + a + "')].status").value("RASCUNHO"));
        comToken(get("/api/admin/questionarios?status=ENVIADO"), token).andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.publicId=='" + a + "')]").isEmpty());
        comToken(get("/api/admin/questionarios?status=rascunho"), token).andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.publicId=='" + a + "')]").isNotEmpty());
        comToken(get("/api/admin/questionarios?status=INVENTADO"), token).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Lista: um questionário completo e enviado tem 8 blocos completos e nenhuma pendência")
    void listaCompleto() throws Exception {
        String id = iniciar();
        preencherTudo(id, "Belford Roxo");
        enviar(id);
        comToken(get("/api/admin/questionarios?status=ENVIADO"), entrar()).andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.publicId=='" + id + "')].blocosCompletos").value(8))
                .andExpect(jsonPath("$[?(@.publicId=='" + id + "')].pendencias").value(0))
                .andExpect(jsonPath("$[?(@.publicId=='" + id + "')].status").value("ENVIADO"))
                .andExpect(jsonPath("$[?(@.publicId=='" + id + "')].enviadoEm").isNotEmpty());
    }

    @Test
    @DisplayName("Visão completa: respostas dos 8 blocos e pendências; link inexistente dá 404")
    void detalhe() throws Exception {
        String id = iniciar();
        salvarBloco(id, 1, "{\"municipio\":\"Mesquita\",\"version\":null}");
        String token = entrar();
        comToken(get("/api/admin/questionarios/" + id), token).andExpect(status().isOk())
                .andExpect(jsonPath("$.respostas.bloco1.municipio").value("Mesquita"))
                .andExpect(jsonPath("$.respostas.bloco3.filas").isArray())
                .andExpect(jsonPath("$.pendencias").isNotEmpty());
        comToken(get("/api/admin/questionarios/nao-existe"), token).andExpect(status().isNotFound());
        comToken(get("/api/admin/questionarios/nao-existe/excel"), token).andExpect(status().isNotFound());
    }

    // ---------- Excel e PDF ----------

    /** Linha do titulo de uma tabela dentro da aba unica (o cabecalho vem logo abaixo e os dados depois dele). */
    private static int tituloDe(Sheet s, String titulo) {
        for (int i = 0; i <= s.getLastRowNum(); i++) {
            var row = s.getRow(i);
            if (row != null && row.getCell(0) != null && row.getCell(0).getCellType() == org.apache.poi.ss.usermodel.CellType.STRING
                    && titulo.equals(row.getCell(0).getStringCellValue())) return i;
        }
        throw new AssertionError("tabela '" + titulo + "' não encontrada na aba");
    }

    private static String pdfTexto(byte[] pdf) throws Exception {
        var reader = new com.lowagie.text.pdf.PdfReader(pdf);
        var extrator = new com.lowagie.text.pdf.parser.PdfTextExtractor(reader);
        StringBuilder sb = new StringBuilder();
        for (int p = 1; p <= reader.getNumberOfPages(); p++) sb.append(extrator.getTextFromPage(p)).append('\n');
        return sb.toString();
    }

    @Test
    @DisplayName("Excel de um município: tudo numa aba só, cada tabela com título e cabeçalho; números, datas e R$ como valores")
    void excelDeUmMunicipio() throws Exception {
        String id = iniciar();
        preencherTudo(id, "Nilópolis");
        byte[] bytes = comToken(get("/api/admin/questionarios/" + id + "/excel"), entrar())
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, org.hamcrest.Matchers.containsString("questionario-nilopolis-")))
                .andReturn().getResponse().getContentAsByteArray();

        try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            assertThat(wb.getNumberOfSheets()).isEqualTo(1);
            Sheet s = aba(wb, "Questionário");
            assertThat(texto(s, 0, 0)).isEqualTo("Nilópolis");
            for (String titulo : new String[]{"1 Identificação", "2.1 Prioridades", "2.2 Linhas de cuidado", "2.3 a 2.5 Respostas",
                    "2.6 Ordem dos temas", "3.1 e 3.2 Fila", "3.3 Filas", "4.1 Pedidos de leito", "4.1 Central", "4.2 Cirurgias eletivas",
                    "5.1 Unidades de urgência", "5.2 Perfil dos pacientes", "5.3 e 5.4 Indicadores", "6 Seguimento",
                    "7.1 Repasses estaduais", "7.2 Leitos privados", "7.3 Aparelhos de imagem", "8 Observações finais"}) {
                tituloDe(s, titulo);
            }
            int ident = tituloDe(s, "1 Identificação");
            assertThat(texto(s, ident + 1, 0)).isEqualTo("Município");
            assertThat(texto(s, ident + 2, 0)).isEqualTo("Nilópolis");
            assertThat(s.getRow(ident + 2).getCell(6).getLocalDateTimeCellValue().toLocalDate().toString()).isEqualTo("2026-03-20");

            int filas = tituloDe(s, "3.3 Filas");
            assertThat(texto(s, filas + 1, 2)).isEqualTo("Total aguardando");
            assertThat(texto(s, filas + 2, 0)).isEqualTo("Cardiologia");
            assertThat(s.getRow(filas + 2).getCell(2).getNumericCellValue()).isEqualTo(100.0);
            assertThat(s.getRow(filas + 2).getCell(4).getNumericCellValue()).isEqualTo(45.5);

            int leitos = tituloDe(s, "4.1 Pedidos de leito");
            assertThat(texto(s, leitos + 2, 0)).isEqualTo("Clínico adulto");
            assertThat(s.getRow(leitos + 2).getCell(1).getNumericCellValue()).isEqualTo(50.0);
            assertThat(texto(s, leitos + 3, 1)).isEqualTo("Não disponível");

            int repasses = tituloDe(s, "7.1 Repasses estaduais");
            assertThat(s.getRow(repasses + 2).getCell(4).getNumericCellValue()).isEqualTo(1234567.89);
            assertThat(s.getRow(repasses + 2).getCell(4).getCellStyle().getDataFormatString()).contains("R$");
            assertThat(texto(s, repasses + 2, 6)).isEqualTo("Sim");

            int linhas = tituloDe(s, "2.2 Linhas de cuidado");
            assertThat(texto(s, linhas + 2, 0)).startsWith("Cardiovascular e AVC");
            assertThat(s.getRow(linhas + 2).getCell(1).getNumericCellValue()).isEqualTo(3.0);

            int sistemas = tituloDe(s, "3.1 e 3.2 Fila");
            assertThat(texto(s, sistemas + 2, 0)).contains("SISREG").contains("Sistema da prefeitura");
            assertThat(texto(s, sistemas + 2, 3)).isEqualTo("09/2026");
        }
    }

    @Test
    @DisplayName("Excel consolidado: uma aba só; só enviados por padrão, com 'todos' entram rascunhos; uma linha por município")
    void excelConsolidado() throws Exception {
        String enviado = iniciar(), rascunho = iniciar();
        preencherTudo(enviado, "Consolidado Enviado");
        enviar(enviado);
        salvarBloco(rascunho, 1, "{\"municipio\":\"Consolidado Rascunho\",\"version\":null}");
        String token = entrar();

        byte[] soEnviados = comToken(get("/api/admin/questionarios/excel"), token).andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, org.hamcrest.Matchers.containsString("consolidado-enviado")))
                .andReturn().getResponse().getContentAsByteArray();
        byte[] todos = comToken(get("/api/admin/questionarios/excel?status=TODOS"), token).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();

        try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(soEnviados))) {
            assertThat(wb.getNumberOfSheets()).isEqualTo(1);
            Sheet s = aba(wb, "Questionário");
            assertThat(texto(s, 0, 0)).isEqualTo("Consolidado dos questionários");
            int indice = tituloDe(s, "Questionários incluídos");
            assertThat(texto(s, indice + 1, 0)).isEqualTo("Município");
            String tudo = coluna(s, 0).toString();
            assertThat(tudo).contains("Consolidado Enviado").doesNotContain("Consolidado Rascunho");

            int filas = tituloDe(s, "3.3 Filas");
            assertThat(texto(s, filas + 1, 0)).isEqualTo("Município");
            assertThat(texto(s, filas + 1, 1)).isEqualTo("Status");
            assertThat(texto(s, filas + 1, 2)).isEqualTo("Especialidade / exame");
            // a primeira linha de dados da tabela é do município enviado (o único de "só enviados" com fila)
            int linhaDele = -1;
            for (int i = filas + 2; i <= s.getLastRowNum() && !texto(s, i, 0).isEmpty(); i++) {
                if (texto(s, i, 0).equals("Consolidado Enviado")) linhaDele = i;
            }
            assertThat(linhaDele).isGreaterThan(0);
            assertThat(texto(s, linhaDele, 1)).isEqualTo("Enviado");
            assertThat(texto(s, linhaDele, 2)).isEqualTo("Cardiologia");
        }
        try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(todos))) {
            Sheet s = aba(wb, "Questionário");
            assertThat(coluna(s, 0).toString()).contains("Consolidado Enviado", "Consolidado Rascunho");
            int ident = tituloDe(s, "1 Identificação");
            int linha = -1;
            for (int i = ident + 2; i <= s.getLastRowNum() && !texto(s, i, 0).isEmpty(); i++) {
                if (texto(s, i, 0).equals("Consolidado Rascunho")) linha = i;
            }
            assertThat(linha).isGreaterThan(0);
            assertThat(texto(s, linha, 1)).isEqualTo("Rascunho");
        }
    }

    @Test
    @DisplayName("PDF de um município e consolidado: application/pdf, abre, e traz o título e as tabelas; sem login dá 401")
    void pdf() throws Exception {
        String id = iniciar();
        preencherTudo(id, "Japeri");
        enviar(id);
        mvc.perform(get("/api/admin/questionarios/" + id + "/pdf")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/questionarios/pdf")).andExpect(status().isUnauthorized());
        String token = entrar();

        byte[] um = comToken(get("/api/admin/questionarios/" + id + "/pdf"), token).andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "application/pdf"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, org.hamcrest.Matchers.containsString("questionario-japeri-")))
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(new String(um, 0, 5, java.nio.charset.StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
        try (var leitor = new com.lowagie.text.pdf.PdfReader(um)) {
            assertThat(leitor.getPageSize(1).getWidth()).as("PDF em paisagem").isGreaterThan(leitor.getPageSize(1).getHeight());
        }
        String texto = pdfTexto(um);
        assertThat(texto).contains("Japeri").contains("3.3 Filas").contains("Cardiologia").contains("R$ 1.234.567,89")
                .contains("Questionário enviado").contains("Clínico adulto");

        byte[] todos = comToken(get("/api/admin/questionarios/pdf?status=TODOS"), token).andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, org.hamcrest.Matchers.containsString("questionarios-consolidado-todos.pdf")))
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(pdfTexto(todos)).contains("Consolidado dos questionários").contains("Japeri");
        comToken(get("/api/admin/questionarios/pdf?status=INVENTADO"), token).andExpect(status().isBadRequest());
        comToken(get("/api/admin/questionarios/nao-existe/pdf"), token).andExpect(status().isNotFound());
    }

    private static java.util.List<String> coluna(Sheet s, int coluna) {
        java.util.List<String> valores = new java.util.ArrayList<>();
        for (int i = 1; i <= s.getLastRowNum(); i++) valores.add(texto(s, i, coluna));
        return valores;
    }
}
