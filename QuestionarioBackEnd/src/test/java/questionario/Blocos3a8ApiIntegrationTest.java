package questionario;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Blocos 3 a 8 (conteúdo em JSON): leitura, gravação com versão, validação, coerência, revisão e envio final. */
@SpringBootTest
@ActiveProfiles("test")
class Blocos3a8ApiIntegrationTest {

    private static final Pattern PUBLIC_ID = Pattern.compile("\"publicId\"\\s*:\\s*\"([^\"]+)\"");

    @Autowired
    WebApplicationContext context;

    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    private String iniciar() throws Exception {
        String corpo = mvc.perform(post("/api/questionarios")).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Matcher matcher = PUBLIC_ID.matcher(corpo);
        assertThat(matcher.find()).as("publicId em %s", corpo).isTrue();
        return matcher.group(1);
    }

    private ResultActions enviarBloco(String id, int bloco, String corpo) throws Exception {
        return mvc.perform(put("/api/questionarios/" + id + "/blocos/" + bloco)
                .contentType(MediaType.APPLICATION_JSON).content(corpo));
    }

    private ResultActions lerBloco(String id, int bloco) throws Exception {
        return mvc.perform(get("/api/questionarios/" + id + "/blocos/" + bloco));
    }

    private static String com(String campos, Long version) {
        return "{" + (campos.isEmpty() ? "" : campos + ",") + "\"version\":" + version + "}";
    }

    // ---------- corpos válidos e completos (tudo que o envio exige) ----------

    static final String B1 = "{\"municipio\":\"Nova Iguaçu\",\"secretario\":\"Maria Souza\",\"responsavel\":\"João Lima\","
            + "\"email\":\"joao@exemplo.gov.br\",\"version\":null}";

    static final String B2 = "{\"prioridades\":[{\"titulo\":\"UTI neonatal\",\"problema\":\"x\",\"resolveSozinho\":\"NAO\"}],"
            + "\"grausLinhaCuidado\":{" + notasTodas() + "},"
            + "\"impedimentos\":[\"FALTA_PROFISSIONAIS\"],"
            + "\"ordemTemas\":{\"GOVERNANCA_PACTUACAO\":1,\"ORGANIZACAO_REDE\":2,\"FINANCIAMENTO\":3,"
            + "\"TRANSPORTE_REGULACAO\":4,\"PROFISSIONAIS\":5,\"MONITORAMENTO_AVALIACAO\":6},\"version\":null}";

    private static String notasTodas() {
        String[] linhas = {"CARDIOVASCULAR_AVC", "TRAUMA_ORTOPEDIA", "ONCOLOGIA", "DIAGNOSTICO_IMAGEM", "QUEIMADOS",
                "ATENCAO_PRIMARIA_ESF", "GESTACAO_PARTO_NASCIMENTO", "SAUDE_CRIANCA_PEDIATRIA", "UTI_ADULTO",
                "CLINICA_MEDICA_RETAGUARDA", "CIRURGIAS_ELETIVAS_MEDIA", "SAUDE_MENTAL", "NEFROLOGIA_TRS",
                "OFTALMOLOGIA", "DOENCAS_CRONICAS", "REABILITACAO_PCD", "PALIATIVOS_ATENCAO_DOMICILIAR"};
        StringBuilder sb = new StringBuilder();
        for (String linha : linhas) sb.append(sb.length() == 0 ? "" : ",").append("\"").append(linha).append("\":3");
        return sb.toString();
    }

    static final String B3 = "{\"sistemas\":[\"SISREG\",\"PROPRIO\"],\"sistemaProprioNome\":\"Sistema da prefeitura\","
            + "\"tipoFila\":\"UNICA\",\"classificacaoRisco\":\"SIM\",\"ultimaRevisao\":\"2026-09\",\"dataExtracao\":\"2026-11-05\","
            + "\"filas\":[{\"especialidade\":\"Cardiologia\",\"sigtap\":\"0301010072\",\"totalAguardando\":100,"
            + "\"prioridadeAlta\":10,\"esperaMediaDias\":45.5,\"maiorEsperaDias\":200,\"aguardandoMais180\":20}],\"version\":null}";

    static final String B4 = "{\"leitos\":[{\"tipo\":\"CLINICO_ADULTO\",\"solicitacoes\":50,\"atendidas\":30,\"canceladas\":10,"
            + "\"tempoMedioHoras\":12.5,\"aguardando\":10}],\"centrais\":[\"MUNICIPAL\"],"
            + "\"eletivas\":[{\"especialidade\":\"Ortopedia\",\"totalAguardando\":30,\"esperaMediaDias\":90,\"maiorEsperaDias\":300,\"origem\":\"SER\"}],\"version\":null}";

    static final String B5 = "{\"unidades\":[{\"nome\":\"UPA Centro\",\"cnes\":\"1234567\",\"tipo\":\"UPA\",\"leitosObservacao\":10,"
            + "\"pacientes24h\":8,\"pacientes72h\":3,\"ventilacaoMecanica\":1}],"
            + "\"perfil\":[{\"necessidade\":\"LEITO_CLINICO\",\"pacientes\":5,\"diagnosticos\":\"I10, E11\"}],"
            + "\"pacientesMais24h\":20,\"tempoMedioPermanenciaHoras\":30.5,\"obitosAguardandoVaga\":1,\"altaSemProcedimento\":2,"
            + "\"registroInformatizado\":\"PARCIAL\",\"motivos\":[\"FALTA_LEITO_NA_REGIAO\"],\"version\":null}";

    static final String B6 = "{\"situacoes\":[{\"situacao\":\"FRATURA_CIRURGICA\",\"pacientesAguardando\":7,\"esperaMediaDias\":40,"
            + "\"ondeFeito\":\"Hospital da Posse\"}],\"version\":null}";

    static final String B7 = "{\"repasses\":[{\"unidade\":\"UPA Centro\",\"cnes\":\"1234567\",\"atoNumero\":\"Res. SES 123\","
            + "\"atoData\":\"2025-05-10\",\"valorMensal\":1234567.89,\"vigenciaAte\":\"2026-12-31\",\"repasseEmDia\":\"SIM\"}],"
            + "\"leitosPrivados\":[{\"hospital\":\"Hospital Filantrópico\",\"cnes\":\"7654321\",\"tipoLeito\":\"UTI_ADULTO\","
            + "\"contratadosSus\":5,\"poderiamSerContratados\":3,\"valorDiaria\":1500.5}],"
            + "\"aparelhosNaoDisponivel\":true,\"version\":null}";

    static final String B8 = "{\"observacoes\":\"Nada a acrescentar\",\"documentos\":[\"PLANO_MUNICIPAL_PAS\"],"
            + "\"assinaturaNome\":\"Maria Souza\",\"assinaturaData\":\"2026-11-20\",\"declaracao\":true,\"version\":null}";

    private void preencherTudo(String id) throws Exception {
        enviarBloco(id, 1, B1).andExpect(status().isOk());
        enviarBloco(id, 2, B2).andExpect(status().isOk());
        enviarBloco(id, 3, B3).andExpect(status().isOk());
        enviarBloco(id, 4, B4).andExpect(status().isOk());
        enviarBloco(id, 5, B5).andExpect(status().isOk());
        enviarBloco(id, 6, B6).andExpect(status().isOk());
        enviarBloco(id, 7, B7).andExpect(status().isOk());
        enviarBloco(id, 8, B8).andExpect(status().isOk());
    }

    // ---------- regras comuns aos blocos 3 a 8 ----------

    @ParameterizedTest
    @ValueSource(ints = {3, 4, 5, 6, 7, 8})
    @DisplayName("Regra de versão em cada bloco: cria com 0, sobe, versão velha 409, nula com bloco existente 409, inventada 409")
    void regraDeVersao(int bloco) throws Exception {
        String id = iniciar();
        lerBloco(id, bloco).andExpect(status().isOk()).andExpect(jsonPath("$.version").doesNotExist());

        enviarBloco(id, bloco, com("", 5L)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VERSAO_DESATUALIZADA"));
        enviarBloco(id, bloco, "{}").andExpect(status().isOk()).andExpect(jsonPath("$.version").value(0));
        enviarBloco(id, bloco, com("", 0L)).andExpect(status().isOk()).andExpect(jsonPath("$.version").value(1));

        enviarBloco(id, bloco, com("", 0L)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VERSAO_DESATUALIZADA"));
        enviarBloco(id, bloco, "{}").andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VERSAO_DESATUALIZADA"));
        lerBloco(id, bloco).andExpect(jsonPath("$.version").value(1));
    }

    @ParameterizedTest
    @ValueSource(ints = {3, 4, 5, 6, 7, 8})
    @DisplayName("Link inexistente ou mal formado: 404 na leitura e na gravação de cada bloco")
    void linkInvalido(int bloco) throws Exception {
        for (String id : new String[]{"00000000-0000-0000-0000-000000000000", "abc"}) {
            lerBloco(id, bloco).andExpect(status().isNotFound());
            enviarBloco(id, bloco, "{}").andExpect(status().isNotFound());
        }
    }

    @Test
    @DisplayName("Blocos novos vêm vazios, com as linhas fixas completas: 14 leitos, 13 necessidades, 9 situações")
    void blocosNovosVemVazios() throws Exception {
        String id = iniciar();
        lerBloco(id, 3).andExpect(jsonPath("$.filas.length()").value(0)).andExpect(jsonPath("$.sistemas.length()").value(0))
                .andExpect(jsonPath("$.filasNaoDisponivel").value(false));
        lerBloco(id, 4).andExpect(jsonPath("$.leitos.length()").value(14))
                .andExpect(jsonPath("$.leitos[0].tipo").value("CLINICO_ADULTO"))
                .andExpect(jsonPath("$.leitos[13].tipo").value("OUTRO"));
        lerBloco(id, 5).andExpect(jsonPath("$.perfil.length()").value(13))
                .andExpect(jsonPath("$.perfil[12].necessidade").value("OUTRO"));
        lerBloco(id, 6).andExpect(jsonPath("$.situacoes.length()").value(9))
                .andExpect(jsonPath("$.situacoes[0].situacao").value("FRATURA_CIRURGICA"));
        lerBloco(id, 7).andExpect(jsonPath("$.repasses.length()").value(0));
        lerBloco(id, 8).andExpect(jsonPath("$.documentos.length()").value(0)).andExpect(jsonPath("$.declaracao").value(false));
    }

    // ---------- Bloco 3 ----------

    @Test
    @DisplayName("Bloco 3: grava e devolve tudo (textos aparados, decimal, filas na ordem); 'Qual?' só vale com PROPRIO marcado")
    void bloco3Completo() throws Exception {
        String id = iniciar();
        enviarBloco(id, 3, "{\"sistemas\":[\"SER\",\"SER\"],\"sistemaProprioNome\":\"sobrou\",\"sistemaOutroNome\":\"sobrou\","
                + "\"tipoFila\":\"MISTA\",\"classificacaoRisco\":\"PARCIAL\",\"ultimaRevisao\":\"2026-09\","
                + "\"filas\":[{\"especialidade\":\"  Cardiologia  \",\"sigtap\":\"0301010072\",\"totalAguardando\":100,"
                + "\"esperaMediaDias\":45.5},{\"especialidade\":\"Ortopedia\"}],\"version\":null}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(0))
                .andExpect(jsonPath("$.sistemas.length()").value(1))
                .andExpect(jsonPath("$.sistemaProprioNome").doesNotExist())
                .andExpect(jsonPath("$.sistemaOutroNome").doesNotExist())
                .andExpect(jsonPath("$.tipoFila").value("MISTA"))
                .andExpect(jsonPath("$.ultimaRevisao").value("2026-09"))
                .andExpect(jsonPath("$.filas[0].especialidade").value("Cardiologia"))
                .andExpect(jsonPath("$.filas[0].esperaMediaDias").value(45.5))
                .andExpect(jsonPath("$.filas[1].especialidade").value("Ortopedia"))
                .andExpect(jsonPath("$.filas[1].totalAguardando").doesNotExist());
        lerBloco(id, 3).andExpect(jsonPath("$.filas.length()").value(2)).andExpect(jsonPath("$.filas[0].totalAguardando").value(100));
    }

    @Test
    @DisplayName("Bloco 3: 15 filas passam e 16 dão 400; SIGTAP, mês/ano e números inválidos dão 400 com o campo certo")
    void bloco3Limites() throws Exception {
        String id = iniciar();
        String linha = "{\"especialidade\":\"x\"},";
        enviarBloco(id, 3, "{\"filas\":[" + linha.repeat(14) + "{\"especialidade\":\"x\"}],\"version\":null}").andExpect(status().isOk());
        enviarBloco(id, 3, "{\"filas\":[" + linha.repeat(15) + "{\"especialidade\":\"x\"}],\"version\":0}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.filas").value("Informe no máximo 15 filas"));

        enviarBloco(id, 3, "{\"ultimaRevisao\":\"13/2026\",\"filas\":[{\"sigtap\":\"123\",\"totalAguardando\":-1,"
                + "\"esperaMediaDias\":1.234}],\"version\":0}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.ultimaRevisao").value("Use mês e ano (AAAA-MM)"))
                .andExpect(jsonPath("$.fields['filas[0].sigtap']").value("O código SIGTAP tem 10 números"))
                .andExpect(jsonPath("$.fields['filas[0].totalAguardando']").exists())
                .andExpect(jsonPath("$.fields['filas[0].esperaMediaDias']").exists());
        lerBloco(id, 3).andExpect(jsonPath("$.version").value(0)).andExpect(jsonPath("$.filas.length()").value(15));
    }

    @Test
    @DisplayName("Bloco 3: enum desconhecido dá 400; 'Não disponível' esvazia a tabela")
    void bloco3EnumENaoDisponivel() throws Exception {
        String id = iniciar();
        enviarBloco(id, 3, "{\"tipoFila\":\"INVENTADA\",\"version\":null}").andExpect(status().isBadRequest());
        enviarBloco(id, 3, "{\"filasNaoDisponivel\":true,\"filas\":[{\"especialidade\":\"vai sumir\"}],\"version\":null}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filasNaoDisponivel").value(true))
                .andExpect(jsonPath("$.filas.length()").value(0));
    }

    // ---------- Bloco 4 ----------

    @Test
    @DisplayName("Bloco 4: só a linha enviada é gravada, mas o GET traz as 14 na ordem do documento; 'Outro' tem descrição")
    void bloco4LinhasFixas() throws Exception {
        String id = iniciar();
        enviarBloco(id, 4, "{\"leitos\":[{\"tipo\":\"UTI_ADULTO\",\"solicitacoes\":50,\"atendidas\":30,\"canceladas\":5,"
                + "\"tempoMedioHoras\":12.5,\"aguardando\":15},{\"tipo\":\"OUTRO\",\"solicitacoes\":2}],"
                + "\"leitoOutroDescricao\":\"Queimados\",\"centrais\":[\"CRUR_SAMU\",\"OUTRA\"],\"centralOutra\":\"Central X\","
                + "\"dataExtracaoLeitos\":\"2026-11-05\",\"version\":null}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.leitos.length()").value(14))
                .andExpect(jsonPath("$.leitos[0].tipo").value("CLINICO_ADULTO"))
                .andExpect(jsonPath("$.leitos[0].solicitacoes").doesNotExist())
                .andExpect(jsonPath("$.leitos[3].tipo").value("UTI_ADULTO"))
                .andExpect(jsonPath("$.leitos[3].solicitacoes").value(50))
                .andExpect(jsonPath("$.leitos[3].tempoMedioHoras").value(12.5))
                .andExpect(jsonPath("$.leitos[13].solicitacoes").value(2))
                .andExpect(jsonPath("$.leitoOutroDescricao").value("Queimados"))
                .andExpect(jsonPath("$.centralOutra").value("Central X"))
                .andExpect(jsonPath("$.dataExtracaoLeitos").value("2026-11-05"));
    }

    @Test
    @DisplayName("Bloco 4: tipo de leito repetido dá 400; 'Não disponível' na linha zera os números dela")
    void bloco4RepetidaENaoDisponivel() throws Exception {
        String id = iniciar();
        enviarBloco(id, 4, "{\"leitos\":[{\"tipo\":\"UTI_ADULTO\"},{\"tipo\":\"UTI_ADULTO\"}],\"version\":null}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.leitos").value("Há uma linha repetida"));
        enviarBloco(id, 4, "{\"leitos\":[{\"tipo\":\"UTI_ADULTO\",\"naoDisponivel\":true,\"solicitacoes\":9}],\"version\":null}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.leitos[3].naoDisponivel").value(true))
                .andExpect(jsonPath("$.leitos[3].solicitacoes").doesNotExist());
    }

    @Test
    @DisplayName("Bloco 4: 10 eletivas passam e 11 dão 400; origem 'Outro' guarda o texto, as demais descartam")
    void bloco4Eletivas() throws Exception {
        String id = iniciar();
        String linha = "{\"especialidade\":\"x\"},";
        enviarBloco(id, 4, "{\"eletivas\":[" + linha.repeat(9) + "{\"especialidade\":\"x\"}],\"version\":null}").andExpect(status().isOk());
        enviarBloco(id, 4, "{\"eletivas\":[" + linha.repeat(10) + "{\"especialidade\":\"x\"}],\"version\":0}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.eletivas").value("Informe no máximo 10 linhas"));
        enviarBloco(id, 4, "{\"eletivas\":[{\"origem\":\"OUTRO\",\"origemOutro\":\"Planilha\"},"
                + "{\"origem\":\"SER\",\"origemOutro\":\"sobrou\"}],\"version\":0}")
                .andExpect(jsonPath("$.eletivas[0].origemOutro").value("Planilha"))
                .andExpect(jsonPath("$.eletivas[1].origemOutro").doesNotExist());
    }

    // ---------- Bloco 5 ----------

    @Test
    @DisplayName("Bloco 5: grava unidades, perfil (13 linhas), indicadores e motivos; CNES inválido dá 400")
    void bloco5() throws Exception {
        String id = iniciar();
        enviarBloco(id, 5, B5.replace("\"version\":null", "\"motivoOutro\":\"sobrou\",\"version\":null"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unidades[0].cnes").value("1234567"))
                .andExpect(jsonPath("$.unidades[0].tipo").value("UPA"))
                .andExpect(jsonPath("$.perfil.length()").value(13))
                .andExpect(jsonPath("$.perfil[0].necessidade").value("LEITO_CLINICO"))
                .andExpect(jsonPath("$.perfil[0].diagnosticos").value("I10, E11"))
                .andExpect(jsonPath("$.tempoMedioPermanenciaHoras").value(30.5))
                .andExpect(jsonPath("$.registroInformatizado").value("PARCIAL"))
                .andExpect(jsonPath("$.motivoOutro").doesNotExist());

        enviarBloco(id, 5, "{\"unidades\":[{\"cnes\":\"123\"}],\"version\":0}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields['unidades[0].cnes']").value("O CNES tem 7 números"));
    }

    @Test
    @DisplayName("Bloco 5: 'Não disponível' nos indicadores zera os 5 campos; necessidade repetida dá 400")
    void bloco5NaoDisponivel() throws Exception {
        String id = iniciar();
        enviarBloco(id, 5, "{\"indicadoresNaoDisponivel\":true,\"pacientesMais24h\":10,\"registroInformatizado\":\"SIM\",\"version\":null}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.indicadoresNaoDisponivel").value(true))
                .andExpect(jsonPath("$.pacientesMais24h").doesNotExist())
                .andExpect(jsonPath("$.registroInformatizado").doesNotExist());
        enviarBloco(id, 5, "{\"perfil\":[{\"necessidade\":\"HEMODIALISE\"},{\"necessidade\":\"HEMODIALISE\"}],\"version\":0}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.perfil").exists());
    }

    // ---------- Bloco 6 ----------

    @Test
    @DisplayName("Bloco 6: 9 situações na ordem do documento; só 'Outro' guarda situação e procedimento")
    void bloco6() throws Exception {
        String id = iniciar();
        enviarBloco(id, 6, "{\"situacoes\":[{\"situacao\":\"AVC\",\"situacaoOutra\":\"sobrou\",\"pacientesAguardando\":4,"
                + "\"esperaMediaDias\":12.5,\"ondeFeito\":\"  Hospital X \"},"
                + "{\"situacao\":\"OUTRO\",\"situacaoOutra\":\"Queimadura\",\"procedimentoOutro\":\"Enxerto\",\"pacientesAguardando\":1}],\"version\":null}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacoes.length()").value(9))
                .andExpect(jsonPath("$.situacoes[2].situacao").value("AVC"))
                .andExpect(jsonPath("$.situacoes[2].situacaoOutra").doesNotExist())
                .andExpect(jsonPath("$.situacoes[2].ondeFeito").value("Hospital X"))
                .andExpect(jsonPath("$.situacoes[8].situacaoOutra").value("Queimadura"))
                .andExpect(jsonPath("$.situacoes[8].procedimentoOutro").value("Enxerto"));
        enviarBloco(id, 6, "{\"situacoes\":[{\"situacao\":\"AVC\"},{\"situacao\":\"AVC\"}],\"version\":0}")
                .andExpect(status().isBadRequest());
    }

    // ---------- Bloco 7 ----------

    @Test
    @DisplayName("Bloco 7: dinheiro com centavos vai e volta sem perder precisão; 3 casas decimais e negativo dão 400")
    void bloco7Dinheiro() throws Exception {
        String id = iniciar();
        enviarBloco(id, 7, B7).andExpect(status().isOk())
                .andExpect(jsonPath("$.repasses[0].valorMensal").value(1234567.89))
                .andExpect(jsonPath("$.repasses[0].atoData").value("2025-05-10"))
                .andExpect(jsonPath("$.leitosPrivados[0].valorDiaria").value(1500.5))
                .andExpect(jsonPath("$.leitosPrivados[0].tipoLeito").value("UTI_ADULTO"))
                .andExpect(jsonPath("$.aparelhosNaoDisponivel").value(true))
                .andExpect(jsonPath("$.aparelhos.length()").value(0));
        enviarBloco(id, 7, "{\"repasses\":[{\"valorMensal\":10.123},{\"valorMensal\":-1}],\"version\":0}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields['repasses[0].valorMensal']").exists())
                .andExpect(jsonPath("$.fields['repasses[1].valorMensal']").exists());
    }

    @Test
    @DisplayName("Bloco 7: 20 linhas passam e 21 dão 400 em cada tabela")
    void bloco7Limites() throws Exception {
        String id = iniciar();
        String linha = "{\"observacao\":\"x\"},";
        String vinte = linha.repeat(19) + "{\"observacao\":\"x\"}";
        String vinteUm = linha.repeat(20) + "{\"observacao\":\"x\"}";
        enviarBloco(id, 7, "{\"repasses\":[" + vinte + "],\"version\":null}").andExpect(status().isOk());
        for (String tabela : new String[]{"repasses", "leitosPrivados", "aparelhos"}) {
            enviarBloco(id, 7, "{\"" + tabela + "\":[" + vinteUm + "],\"version\":0}")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fields." + tabela).value("Informe no máximo 20 linhas"));
        }
    }

    // ---------- Bloco 8 ----------

    @Test
    @DisplayName("Bloco 8: observações, documentos e declaração")
    void bloco8() throws Exception {
        String id = iniciar();
        enviarBloco(id, 8, B8).andExpect(status().isOk())
                .andExpect(jsonPath("$.observacoes").value("Nada a acrescentar"))
                .andExpect(jsonPath("$.documentos[0]").value("PLANO_MUNICIPAL_PAS"))
                .andExpect(jsonPath("$.assinaturaData").value("2026-11-20"))
                .andExpect(jsonPath("$.declaracao").value(true));
        enviarBloco(id, 8, "{\"observacoes\":\"" + "x".repeat(5001) + "\",\"version\":0}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.observacoes").value("Máximo de 5000 caracteres"));
    }

    // ---------- situação, revisão, pendências e envio ----------

    @Test
    @DisplayName("GET do questionário: cada bloco salvo aparece com versão; os outros seguem sem")
    void situacaoDosBlocos() throws Exception {
        String id = iniciar();
        enviarBloco(id, 5, "{}").andExpect(status().isOk());
        enviarBloco(id, 8, "{}").andExpect(status().isOk());
        mvc.perform(get("/api/questionarios/" + id))
                .andExpect(jsonPath("$.blocos[4].numero").value(5)).andExpect(jsonPath("$.blocos[4].version").value(0))
                .andExpect(jsonPath("$.blocos[7].version").value(0))
                .andExpect(jsonPath("$.blocos[2].version").doesNotExist())
                .andExpect(jsonPath("$.blocos[3].version").doesNotExist());
    }

    @Test
    @DisplayName("Pendências: questionário novo tem pendências em todos os blocos")
    void pendenciasDoQuestionarioNovo() throws Exception {
        String id = iniciar();
        mvc.perform(get("/api/questionarios/" + id + "/pendencias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.bloco==1)]").isNotEmpty())
                .andExpect(jsonPath("$[?(@.bloco==2)]").isNotEmpty())
                .andExpect(jsonPath("$[?(@.bloco==3)]").isNotEmpty())
                .andExpect(jsonPath("$[?(@.bloco==4)]").isNotEmpty())
                .andExpect(jsonPath("$[?(@.bloco==5)]").isNotEmpty())
                .andExpect(jsonPath("$[?(@.bloco==6)]").isNotEmpty())
                .andExpect(jsonPath("$[?(@.bloco==7)]").isNotEmpty())
                .andExpect(jsonPath("$[?(@.bloco==8)]").isNotEmpty());
    }

    @Test
    @DisplayName("Tudo preenchido: nenhuma pendência; a revisão traz os 8 blocos")
    void tudoPreenchidoNaoTemPendencias() throws Exception {
        String id = iniciar();
        preencherTudo(id);
        mvc.perform(get("/api/questionarios/" + id + "/pendencias")).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/questionarios/" + id + "/respostas")).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RASCUNHO"))
                .andExpect(jsonPath("$.bloco1.municipio").value("Nova Iguaçu"))
                .andExpect(jsonPath("$.bloco2.prioridades[0].titulo").value("UTI neonatal"))
                .andExpect(jsonPath("$.bloco3.filas[0].especialidade").value("Cardiologia"))
                .andExpect(jsonPath("$.bloco4.leitos.length()").value(14))
                .andExpect(jsonPath("$.bloco5.unidades[0].nome").value("UPA Centro"))
                .andExpect(jsonPath("$.bloco6.situacoes.length()").value(9))
                .andExpect(jsonPath("$.bloco7.repasses[0].valorMensal").value(1234567.89))
                .andExpect(jsonPath("$.bloco8.declaracao").value(true));
    }

    @Test
    @DisplayName("Coerência não trava o rascunho (200) mas aparece como pendência COERENCIA e bloqueia o envio")
    void coerenciaBloqueiaSoOEnvio() throws Exception {
        String id = iniciar();
        preencherTudo(id);
        // 3.3: prioridade alta (50) e 180 dias (20) maiores que o total (10); 4.1: atendidas+canceladas > solicitações;
        // 5.1: 72 h maior que 24 h; 7.3: em uso maior que a quantidade
        enviarBloco(id, 3, B3.replace("\"totalAguardando\":100,\"prioridadeAlta\":10", "\"totalAguardando\":10,\"prioridadeAlta\":50")
                .replace("\"version\":null", "\"version\":0")).andExpect(status().isOk());
        enviarBloco(id, 5, B5.replace("\"pacientes24h\":8,\"pacientes72h\":3", "\"pacientes24h\":3,\"pacientes72h\":8")
                .replace("\"version\":null", "\"version\":0")).andExpect(status().isOk());
        enviarBloco(id, 4, B4.replace("\"atendidas\":30,\"canceladas\":10", "\"atendidas\":45,\"canceladas\":10")
                .replace("\"version\":null", "\"version\":0")).andExpect(status().isOk());
        enviarBloco(id, 7, B7.replace("\"aparelhosNaoDisponivel\":true",
                "\"aparelhos\":[{\"estabelecimento\":\"X\",\"aparelho\":\"TOMOGRAFO\",\"quantidadeCnes\":1,\"emUsoDeFato\":2}]")
                .replace("\"version\":null", "\"version\":0")).andExpect(status().isOk());

        mvc.perform(get("/api/questionarios/" + id + "/pendencias"))
                // 3.3: prioridade alta e 180 dias maiores que o total (2), 4.1, 5.1 e 7.3
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$[0].tipo").value("COERENCIA"))
                .andExpect(jsonPath("$[4].tipo").value("COERENCIA"))
                .andExpect(jsonPath("$[4].bloco").value(7))
                .andExpect(jsonPath("$[0].bloco").value(3))
                .andExpect(jsonPath("$[0].campo").value("filas[0].prioridadeAlta"));
        mvc.perform(post("/api/questionarios/" + id + "/enviar")).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ENVIO_INCOMPLETO"));
    }

    @Test
    @DisplayName("Enviar com pendências: 409 ENVIO_INCOMPLETO com o campo de cada uma, e o questionário continua RASCUNHO")
    void envioIncompleto() throws Exception {
        String id = iniciar();
        enviarBloco(id, 1, B1).andExpect(status().isOk());
        mvc.perform(post("/api/questionarios/" + id + "/enviar"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ENVIO_INCOMPLETO"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.fields['bloco2.prioridades']").exists())
                .andExpect(jsonPath("$.fields['bloco3.sistemas']").exists())
                .andExpect(jsonPath("$.fields['bloco8.declaracao']").exists())
                .andExpect(jsonPath("$.fields['bloco1.municipio']").doesNotExist());
        mvc.perform(get("/api/questionarios/" + id)).andExpect(jsonPath("$.status").value("RASCUNHO"));
    }

    @Test
    @DisplayName("Enviar completo: 200 ENVIADO com data; depois nenhum bloco aceita gravação (409), enviar de novo dá 409, e a leitura segue")
    void envioCompleto() throws Exception {
        String id = iniciar();
        preencherTudo(id);
        mvc.perform(post("/api/questionarios/" + id + "/enviar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ENVIADO"))
                .andExpect(jsonPath("$.enviadoEm").exists());

        for (int bloco = 1; bloco <= 8; bloco++) {
            enviarBloco(id, bloco, com("", 0L)).andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("QUESTIONARIO_ENVIADO"));
        }
        mvc.perform(post("/api/questionarios/" + id + "/enviar")).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("QUESTIONARIO_ENVIADO"));
        mvc.perform(get("/api/questionarios/" + id + "/respostas")).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ENVIADO"))
                .andExpect(jsonPath("$.bloco3.tipoFila").value("UNICA"));
    }

    @Test
    @DisplayName("Revisão, pendências e envio com link inexistente: 404")
    void linkInvalidoNaRevisaoEEnvio() throws Exception {
        mvc.perform(get("/api/questionarios/abc/respostas")).andExpect(status().isNotFound());
        mvc.perform(get("/api/questionarios/abc/pendencias")).andExpect(status().isNotFound());
        mvc.perform(post("/api/questionarios/abc/enviar")).andExpect(status().isNotFound());
    }
}
