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

    private static void exigir(List<Pendencia> lista, int bloco, String campo, String mensagem, String valor) {
        if (vazio(valor)) lista.add(Pendencia.obrigatorio(bloco, campo, mensagem));
    }

    public static List<Pendencia> bloco1(Bloco1Response b) {
        List<Pendencia> lista = new ArrayList<>();
        exigir(lista, 1, "municipio", "Informe o município", b.municipio());
        exigir(lista, 1, "secretario", "Informe o(a) secretário(a) municipal de saúde", b.secretario());
        exigir(lista, 1, "responsavel", "Informe o responsável pelo preenchimento", b.responsavel());
        exigir(lista, 1, "cargoSetor", "Informe o cargo ou setor", b.cargoSetor());
        exigir(lista, 1, "email", "Informe o e-mail", b.email());
        exigir(lista, 1, "telefone", "Informe o telefone ou WhatsApp", b.telefone());
        if (b.dataPreenchimento() == null) lista.add(Pendencia.obrigatorio(1, "dataPreenchimento", "Informe a data do preenchimento"));
        return lista;
    }

    public static List<Pendencia> bloco2(Bloco2Response b) {
        List<Pendencia> lista = new ArrayList<>();
        for (int i = 0; i < b.prioridades().size(); i++) {
            PrioridadeDto p = b.prioridades().get(i);
            String r = "2.1, prioridade " + (i + 1) + ": ";
            if (vazio(p.titulo())) lista.add(Pendencia.obrigatorio(2, "prioridades[" + i + "].titulo", r + "informe a prioridade"));
            if (vazio(p.problema())) lista.add(Pendencia.obrigatorio(2, "prioridades[" + i + "].problema", r + "informe o problema a que ela responde"));
            if (p.resolveSozinho() == null) lista.add(Pendencia.obrigatorio(2, "prioridades[" + i + "].resolveSozinho", r + "informe se o município resolve sozinho"));
        }
        long semNota = java.util.Arrays.stream(LinhaCuidado.values())
                .filter(l -> l != LinhaCuidado.OUTRA)
                .filter(l -> b.grausLinhaCuidado().get(l) == null).count();
        if (semNota > 0) lista.add(Pendencia.obrigatorio(2, "grausLinhaCuidado", "2.2: faltam notas em " + semNota + " linha(s) de cuidado"));
        if (vazio(b.gargalos())) lista.add(Pendencia.obrigatorio(2, "gargalos", "2.3: informe os maiores gargalos de acesso"));
        if (vazio(b.servicosParaVizinhos())) lista.add(Pendencia.obrigatorio(2, "servicosParaVizinhos", "2.4: informe os serviços para atender os municípios vizinhos"));
        if (b.impedimentos().isEmpty()) lista.add(Pendencia.obrigatorio(2, "impedimentos", "2.5: marque ao menos um impedimento"));
        if (b.impedimentos().contains(Impedimento.OUTRO) && vazio(b.impedimentoOutro())) {
            lista.add(Pendencia.obrigatorio(2, "impedimentoOutro", "2.5: descreva o outro impedimento"));
        }
        boolean ordemCompleta = java.util.Arrays.stream(TemaUrgencia.values()).allMatch(t -> b.ordemTemas().get(t) != null);
        if (!ordemCompleta) lista.add(Pendencia.obrigatorio(2, "ordemTemas", "2.6: ordene os 6 temas e confirme a ordem"));
        return lista;
    }
}
