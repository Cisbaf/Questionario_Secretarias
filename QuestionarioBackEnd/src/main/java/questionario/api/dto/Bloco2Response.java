package questionario.api.dto;

import questionario.domain.Bloco2Prioridades;
import questionario.domain.PrioridadeItem;
import questionario.domain.enums.Impedimento;
import questionario.domain.enums.LinhaCuidado;
import questionario.domain.enums.TemaUrgencia;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * Bloco 2 como o front recebe. {@code prioridades} vem sempre com as 5 linhas (as vazias com campos nulos);
 * {@code grausLinhaCuidado} e {@code ordemTemas} listam todas as chaves, com {@code null} onde não há resposta.
 * Bloco ainda não salvo: tudo vazio, inclusive {@code version}.
 */
public record Bloco2Response(
        List<PrioridadeDto> prioridades,
        Map<LinhaCuidado, Integer> grausLinhaCuidado,
        String linhaOutraDescricao,
        String gargalos,
        String servicosParaVizinhos,
        List<Impedimento> impedimentos,
        String impedimentoOutro,
        Map<TemaUrgencia, Integer> ordemTemas,
        Long version,
        LocalDateTime atualizadoEm) {

    public static final int TOTAL_PRIORIDADES = 5;

    public static Bloco2Response vazio() {
        return montar(List.of(), Map.of(), null, null, null, List.of(), null, Map.of(), null, null);
    }

    public static Bloco2Response from(Bloco2Prioridades bloco) {
        return montar(bloco.getPrioridades(), bloco.getGraus(), bloco.getLinhaOutraDescricao(), bloco.getGargalos(),
                bloco.getServicosParaVizinhos(), List.copyOf(bloco.getImpedimentos()), bloco.getImpedimentoOutro(),
                bloco.getOrdemTemas(), bloco.getVersion(), bloco.getAtualizadoEm());
    }

    private static Bloco2Response montar(List<PrioridadeItem> itens, Map<LinhaCuidado, Integer> graus, String outraLinha,
                                         String gargalos, String vizinhos, List<Impedimento> impedimentos,
                                         String impedimentoOutro, Map<TemaUrgencia, Integer> ordem,
                                         Long version, LocalDateTime atualizadoEm) {
        List<PrioridadeDto> linhas = IntStream.rangeClosed(1, TOTAL_PRIORIDADES)
                .mapToObj(posicao -> itens.stream().filter(item -> item.getPosicao() == posicao).findFirst()
                        .map(item -> new PrioridadeDto(item.getTitulo(), item.getProblema(), item.getResolveSozinho()))
                        .orElseGet(() -> new PrioridadeDto(null, null, null)))
                .toList();
        Map<LinhaCuidado, Integer> notas = new LinkedHashMap<>();
        Arrays.stream(LinhaCuidado.values()).forEach(linha -> notas.put(linha, graus.get(linha)));
        Map<TemaUrgencia, Integer> posicoes = new LinkedHashMap<>();
        Arrays.stream(TemaUrgencia.values()).forEach(tema -> posicoes.put(tema, ordem.get(tema)));
        List<Impedimento> marcados = impedimentos.stream().sorted(Comparator.naturalOrder()).toList();
        return new Bloco2Response(linhas, notas, outraLinha, gargalos, vizinhos, marcados, impedimentoOutro,
                posicoes, version, atualizadoEm);
    }
}
