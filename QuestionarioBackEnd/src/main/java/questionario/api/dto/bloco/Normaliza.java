package questionario.api.dto.bloco;

import questionario.exception.DadosInvalidosException;

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
}
