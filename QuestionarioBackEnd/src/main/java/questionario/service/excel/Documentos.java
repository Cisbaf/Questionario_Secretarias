package questionario.service.excel;

import questionario.service.AdminService;
import questionario.service.AdminService.Lido;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * O que vai para o Excel e para o PDF: um titulo e uma lista de tabelas (o nome da aba vira o titulo da tabela).
 * Um municipio: as tabelas do documento como estao. Consolidado: a lista dos questionarios e, para cada tabela, as
 * linhas de todos os municipios com "Municipio" e "Status" na frente.
 */
record Documentos(String titulo, String subtitulo, List<Tabela> tabelas, String nomeArquivo) {

    static Documentos porMunicipio(Lido lido) {
        String municipio = lido.respostas().bloco1().municipio();
        boolean enviado = enviado(lido);
        String quando = lido.questionario().getEnviadoEm() != null
                ? "Enviado em " + Formato.dataHora(lido.questionario().getEnviadoEm())
                : "Em preenchimento (iniciado em " + Formato.dataHora(lido.questionario().getCriadoEm()) + ")";
        return new Documentos(
                municipio == null || municipio.isBlank() ? "(sem município)" : municipio.strip(),
                (enviado ? "Questionário enviado" : "Questionário em preenchimento") + " · " + quando,
                TabelasDoQuestionario.de(lido.respostas()),
                "questionario-" + slug(municipio, lido.questionario().getPublicId()));
    }

    static Documentos consolidado(List<Lido> lidos, Set<String> repetidos, String filtro) {
        List<Tabela> tabelas = new ArrayList<>();

        Tabela indice = Tabela.nova("Questionários incluídos", "Município", "Status", "Enviado em", "Pendências",
                "Município repetido", "Código do questionário");
        for (Lido l : lidos) {
            boolean repetido = repetidos.contains(AdminService.chave(l.respostas().bloco1().municipio()));
            indice.linha(l.respostas().bloco1().municipio(), status(l), l.questionario().getEnviadoEm(),
                    l.pendencias().size(), repetido ? "Sim" : "", l.questionario().getPublicId());
        }
        tabelas.add(indice);

        Map<String, Tabela> porAba = new LinkedHashMap<>();
        for (Lido lido : lidos) {
            for (Tabela t : TabelasDoQuestionario.de(lido.respostas())) {
                Tabela destino = porAba.computeIfAbsent(t.aba(), aba -> {
                    List<String> colunas = new ArrayList<>(List.of("Município", "Status"));
                    colunas.addAll(t.colunas());
                    return new Tabela(aba, colunas, new ArrayList<>());
                });
                for (List<Object> linha : t.linhas()) {
                    List<Object> nova = new ArrayList<>();
                    nova.add(lido.respostas().bloco1().municipio());
                    nova.add(status(lido));
                    nova.addAll(linha);
                    destino.linhas().add(nova);
                }
            }
        }
        tabelas.addAll(porAba.values());
        String quais = switch (filtro) {
            case "ENVIADO" -> "só questionários enviados";
            case "RASCUNHO" -> "só questionários em preenchimento";
            default -> "todos os questionários";
        };
        return new Documentos("Consolidado dos questionários", lidos.size() + " questionário(s): " + quais,
                tabelas, "questionarios-consolidado-" + filtro.toLowerCase());
    }

    private static boolean enviado(Lido l) {
        return l.questionario().getStatus().name().equals("ENVIADO");
    }

    private static String status(Lido l) {
        return enviado(l) ? "Enviado" : "Rascunho";
    }

    /** Nome de arquivo seguro: sem acento, so letras, numeros e hifen; termina com 8 caracteres do codigo para dois municipios iguais nao baterem no mesmo nome. */
    static String slug(String municipio, String publicId) {
        String chave = AdminService.chave(municipio).replace(' ', '-');
        String curto = publicId.substring(0, 8);
        return chave.isEmpty() ? "sem-municipio-" + curto : chave + "-" + curto;
    }
}
