package questionario.service;

import questionario.api.dto.Bloco2Request;
import questionario.domain.enums.Impedimento;
import questionario.domain.enums.LinhaCuidado;
import questionario.domain.enums.TemaUrgencia;
import questionario.exception.DadosInvalidosException;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Regras do Bloco 2 que o Bean Validation não expressa. Valem para rascunho: nada aqui exige que algo esteja
 * preenchido, só que o que foi preenchido faça sentido (o "obrigatório" fica para o envio, E10).
 */
final class Bloco2Validacao {

    static final int MAX_IMPEDIMENTOS = 3;
    static final int NOTA_MINIMA = 1;
    static final int NOTA_MAXIMA = 5;

    private Bloco2Validacao() {
    }

    static void validar(Bloco2Request request) {
        Map<String, String> erros = new LinkedHashMap<>();

        Map<LinhaCuidado, Integer> graus = request.grausLinhaCuidado();
        if (graus != null) {
            graus.forEach((linha, nota) -> {
                if (nota != null && (nota < NOTA_MINIMA || nota > NOTA_MAXIMA)) {
                    erros.put("grausLinhaCuidado." + linha.name(), "Use uma nota de 1 a 5");
                }
            });
            if (graus.get(LinhaCuidado.OUTRA) != null && vazio(request.linhaOutraDescricao())) {
                erros.put("linhaOutraDescricao", "Descreva a outra linha de cuidado");
            }
        }

        Set<Impedimento> impedimentos = marcados(request.impedimentos());
        if (impedimentos.size() > MAX_IMPEDIMENTOS) {
            erros.put("impedimentos", "Marque no máximo " + MAX_IMPEDIMENTOS + " opções");
        }
        if (impedimentos.contains(Impedimento.OUTRO) && vazio(request.impedimentoOutro())) {
            erros.put("impedimentoOutro", "Descreva o outro impedimento");
        }

        Map<TemaUrgencia, Integer> ordem = request.ordemTemas();
        if (ordem != null) {
            int total = TemaUrgencia.values().length;
            Set<Integer> usadas = new HashSet<>();
            boolean repetida = false;
            boolean foraDaFaixa = false;
            for (Integer posicao : ordem.values()) {
                if (posicao == null) continue;
                if (posicao < 1 || posicao > total) foraDaFaixa = true;
                else if (!usadas.add(posicao)) repetida = true;
            }
            if (foraDaFaixa) {
                erros.put("ordemTemas", "Use posições de 1 a " + total);
            } else if (repetida) {
                erros.put("ordemTemas", "Cada posição só pode ser usada uma vez");
            }
        }

        if (!erros.isEmpty()) {
            throw new DadosInvalidosException(erros);
        }
    }

    /** Conjunto sem nulos (um {@code [null]} no JSON não deve estourar mais adiante). */
    static Set<Impedimento> marcados(Set<Impedimento> bruto) {
        Set<Impedimento> limpo = new HashSet<>();
        if (bruto != null) {
            bruto.stream().filter(Objects::nonNull).forEach(limpo::add);
        }
        return limpo;
    }

    private static boolean vazio(String texto) {
        return texto == null || texto.isBlank();
    }
}
