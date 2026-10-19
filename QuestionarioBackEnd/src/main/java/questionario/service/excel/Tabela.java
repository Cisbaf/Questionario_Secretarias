package questionario.service.excel;

import java.util.ArrayList;
import java.util.List;

/**
 * Uma tabela do documento (2.1, 3.3, 4.1...) de um questionario: o nome da aba, os cabecalhos e as linhas. Valores:
 * texto, {@link Integer}, {@link java.math.BigDecimal}, {@link Reais}, {@link java.time.LocalDate} ou nulo.
 */
record Tabela(String aba, List<String> colunas, List<List<Object>> linhas) {

    static Tabela nova(String aba, String... colunas) {
        return new Tabela(aba, List.of(colunas), new ArrayList<>());
    }

    Tabela linha(Object... valores) {
        linhas.add(java.util.Arrays.asList(valores));
        return this;
    }
}
