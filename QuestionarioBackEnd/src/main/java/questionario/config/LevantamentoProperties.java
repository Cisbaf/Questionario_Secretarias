package questionario.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * Capa do questionário ({@code app.levantamento.*}). É a fonte da verdade: ao subir, o {@code LevantamentoService}
 * grava estes valores no levantamento aberto. Datas no formato AAAA-MM-DD; vazio significa "a definir". As datas são
 * lidas como texto de propósito, para a falha na subida dizer exatamente qual propriedade está errada.
 */
@ConfigurationProperties(prefix = "app.levantamento")
public record LevantamentoProperties(
        String titulo,
        String periodoReferenciaInicio,
        String periodoReferenciaFim,
        String dataCensoUpa,
        String prazoDevolucao,
        String enviarPara) {

    public static final String TITULO_PADRAO = "Questionário às Secretarias Municipais de Saúde";

    public String tituloOuPadrao() {
        return semBranco(titulo) != null ? semBranco(titulo) : TITULO_PADRAO;
    }

    public String enviarParaOuNulo() {
        return semBranco(enviarPara);
    }

    public LocalDate inicio() {
        return data("periodo-referencia-inicio", periodoReferenciaInicio);
    }

    public LocalDate fim() {
        return data("periodo-referencia-fim", periodoReferenciaFim);
    }

    public LocalDate censoUpa() {
        return data("data-censo-upa", dataCensoUpa);
    }

    public LocalDate prazo() {
        return data("prazo-devolucao", prazoDevolucao);
    }

    /** Falha com mensagem clara se alguma data estiver ilegível ou se o período terminar antes de começar. */
    public void validar() {
        LocalDate inicio = inicio();
        LocalDate fim = fim();
        censoUpa();
        prazo();
        if (inicio != null && fim != null && fim.isBefore(inicio)) {
            throw new IllegalStateException(
                    "app.levantamento.periodo-referencia-fim (" + fim + ") não pode ser anterior ao início (" + inicio + ")");
        }
    }

    private static LocalDate data(String propriedade, String valor) {
        String texto = semBranco(valor);
        if (texto == null) {
            return null;
        }
        try {
            return LocalDate.parse(texto);
        } catch (DateTimeParseException e) {
            throw new IllegalStateException(
                    "app.levantamento." + propriedade + " inválida ('" + texto + "'): use o formato AAAA-MM-DD");
        }
    }

    private static String semBranco(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
