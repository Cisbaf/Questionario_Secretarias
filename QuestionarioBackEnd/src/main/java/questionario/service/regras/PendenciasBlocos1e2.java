package questionario.service.regras;

import questionario.api.dto.Bloco1Response;
import questionario.api.dto.Bloco2Response;
import questionario.api.dto.Pendencia;
import questionario.api.dto.PrioridadeDto;
import questionario.domain.enums.Impedimento;
import questionario.domain.enums.LinhaCuidado;
import questionario.domain.enums.TemaUrgencia;

import java.util.ArrayList;
import java.util.List;

/** Obrigatórios do envio final para os Blocos 1 e 2 (os demais blocos trazem as suas em {@code BlocoDados.pendencias}). */
public final class PendenciasBlocos1e2 {

    private PendenciasBlocos1e2() {
    }

    private static boolean vazio(String texto) {
        return texto == null || texto.isBlank();
    }

    public static List<Pendencia> bloco1(Bloco1Response b) {
        List<Pendencia> lista = new ArrayList<>();
        if (vazio(b.municipio())) lista.add(Pendencia.obrigatorio(1, "municipio", "Informe o município"));
        if (vazio(b.secretario())) lista.add(Pendencia.obrigatorio(1, "secretario", "Informe o(a) secretário(a) municipal de saúde"));
        if (vazio(b.responsavel())) lista.add(Pendencia.obrigatorio(1, "responsavel", "Informe o responsável pelo preenchimento"));
        if (vazio(b.email()) && vazio(b.telefone())) lista.add(Pendencia.obrigatorio(1, "email", "Informe um e-mail ou um telefone para contato"));
        return lista;
    }

    public static List<Pendencia> bloco2(Bloco2Response b) {
        List<Pendencia> lista = new ArrayList<>();
        boolean temPrioridade = b.prioridades().stream().anyMatch((PrioridadeDto p) -> !vazio(p.titulo()));
        if (!temPrioridade) lista.add(Pendencia.obrigatorio(2, "prioridades", "2.1: informe ao menos uma prioridade"));
        for (int i = 0; i < b.prioridades().size(); i++) {
            PrioridadeDto p = b.prioridades().get(i);
            if (vazio(p.titulo()) && (!vazio(p.problema()) || p.resolveSozinho() != null)) {
                lista.add(Pendencia.obrigatorio(2, "prioridades[" + i + "].titulo", "2.1: a prioridade " + (i + 1) + " tem campos preenchidos, mas falta o nome"));
            }
        }
        long semNota = java.util.Arrays.stream(LinhaCuidado.values())
                .filter(l -> l != LinhaCuidado.OUTRA)
                .filter(l -> b.grausLinhaCuidado().get(l) == null).count();
        if (semNota > 0) lista.add(Pendencia.obrigatorio(2, "grausLinhaCuidado", "2.2: faltam notas em " + semNota + " linha(s) de cuidado"));
        if (b.impedimentos().isEmpty()) lista.add(Pendencia.obrigatorio(2, "impedimentos", "2.5: marque ao menos um impedimento"));
        if (b.impedimentos().contains(Impedimento.OUTRO) && vazio(b.impedimentoOutro())) {
            lista.add(Pendencia.obrigatorio(2, "impedimentoOutro", "2.5: descreva o outro impedimento"));
        }
        boolean ordemCompleta = java.util.Arrays.stream(TemaUrgencia.values()).allMatch(t -> b.ordemTemas().get(t) != null);
        if (!ordemCompleta) lista.add(Pendencia.obrigatorio(2, "ordemTemas", "2.6: ordene os 6 temas e confirme a ordem"));
        return lista;
    }
}
