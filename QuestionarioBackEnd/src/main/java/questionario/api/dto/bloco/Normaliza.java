package questionario.api.dto.bloco;

import questionario.exception.DadosInvalidosException;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.UnaryOperator;

/** Utilitários comuns aos DTOs dos blocos 3 a 8. */
final class Normaliza {

    private Normaliza() {
    }

    /** Tira espaços das pontas; vazio vira nulo (campo não preenchido). */
    static String txt(String valor) {
        if (valor == null) return null;
        String limpo = valor.strip();
        return limpo.isEmpty() ? null : limpo;
    }

    static <E> Set<E> conjunto(Set<E> bruto) {
        Set<E> limpo = new HashSet<>();
        if (bruto != null) bruto.stream().filter(Objects::nonNull).forEach(limpo::add);
        return limpo;
    }

    static <T> List<T> lista(List<T> bruta, UnaryOperator<T> normaliza) {
        List<T> limpa = new ArrayList<>();
        if (bruta != null) {
            for (T item : bruta) {
                if (item != null) limpa.add(normaliza.apply(item));
            }
        }
        return limpa;
    }

    static boolean preenchido(Object... valores) {
        for (Object valor : valores) {
            if (valor == null) continue;
            if (valor instanceof String s && s.isBlank()) continue;
            return true;
        }
        return false;
    }

    /** Falta a resposta: nulo, texto em branco ou conjunto vazio. */
    static boolean falta(Object valor) {
        return valor == null || (valor instanceof String s && s.isBlank())
                || (valor instanceof java.util.Collection<?> c && c.isEmpty());
    }

    /** Regra "todo campo é obrigatório": acrescenta a pendência se o valor faltar. */
    static void exigir(List<questionario.api.dto.Pendencia> lista, int bloco, String campo, String mensagem, Object valor) {
        if (falta(valor)) lista.add(questionario.api.dto.Pendencia.obrigatorio(bloco, campo, mensagem));
    }

    static boolean vazio(String valor) {
        return valor == null || valor.isBlank();
    }

    /** Linhas fixas: garante uma linha por chave do enum, na ordem do enum, sem repetir. */
    static <E extends Enum<E>, L> List<L> linhasFixas(List<L> recebidas, E[] chaves, Function<L, E> chaveDe,
                                                        Function<E, L> vazia) {
        Map<E, L> porChave = new LinkedHashMap<>();
        if (recebidas != null) {
            for (L linha : recebidas) {
                if (linha != null && chaveDe.apply(linha) != null) porChave.putIfAbsent(chaveDe.apply(linha), linha);
            }
        }
        List<L> completas = new ArrayList<>();
        for (E chave : chaves) {
            completas.add(porChave.getOrDefault(chave, vazia.apply(chave)));
        }
        return completas;
    }

    /** 400 se a mesma chave aparece em duas linhas fixas (o front nunca manda isso). */
    static <E, L> void semRepetidas(List<L> linhas, Function<L, E> chaveDe, String campo) {
        if (linhas == null) return;
        Set<E> vistas = new HashSet<>();
        for (L linha : linhas) {
            E chave = linha == null ? null : chaveDe.apply(linha);
            if (chave != null && !vistas.add(chave)) {
                throw new DadosInvalidosException(Map.of(campo, "Há uma linha repetida"));
            }
        }
    }

    static boolean maior(Number a, Number b) {
        return a != null && b != null && a.doubleValue() > b.doubleValue();
    }

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    /** Data de hoje no fuso do consórcio (o servidor pode rodar em outro fuso). */
    static LocalDate hoje() {
        return LocalDate.now(FUSO);
    }

    /** Registra erro se a data estiver no futuro. */
    static void naoFutura(Map<String, String> erros, String campo, LocalDate data) {
        if (data != null && data.isAfter(hoje())) erros.putIfAbsent(campo, "A data não pode ser futura");
    }

    /** Mês e ano "AAAA-MM" não pode ser posterior ao mês atual. */
    static void mesNaoFuturo(Map<String, String> erros, String campo, String anoMes) {
        if (anoMes != null && anoMes.matches("\\d{4}-(0[1-9]|1[0-2])") && YearMonth.parse(anoMes).isAfter(YearMonth.from(hoje()))) {
            erros.putIfAbsent(campo, "O mês não pode ser futuro");
        }
    }

    static void lancarSeHouver(Map<String, String> erros) {
        if (!erros.isEmpty()) throw new DadosInvalidosException(erros);
    }
}
