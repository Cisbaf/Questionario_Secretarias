package questionario.service.excel;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Texto dos valores no PDF (e nas datas dos titulos), no padrao brasileiro. */
final class Formato {

    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DecimalFormatSymbols BR = DecimalFormatSymbols.getInstance(Locale.forLanguageTag("pt-BR"));

    private Formato() {
    }

    static String dataHora(LocalDateTime d) {
        return d == null ? "" : d.format(DATA_HORA);
    }

    static String valor(Object v) {
        return switch (v) {
            case null -> "";
            case Integer i -> new DecimalFormat("#,##0", BR).format(i);
            case BigDecimal d -> new DecimalFormat("#,##0.##", BR).format(d);
            case Reais r -> "R$ " + new DecimalFormat("#,##0.00", BR).format(r.valor());
            case LocalDate d -> d.format(DATA);
            case LocalDateTime d -> d.format(DATA_HORA);
            default -> v.toString();
        };
    }
}
