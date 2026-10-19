package questionario.api.dto.bloco;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import questionario.api.dto.Pendencia;
import questionario.domain.enums.CentralRegulacao;
import questionario.domain.enums.OrigemDado;
import questionario.domain.enums.TipoLeito;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Bloco 4 do documento: regulação hospitalar (itens 4.1 e 4.2). */
public record Bloco4Dados(
        /** 4.1: sempre uma linha por tipo de leito, na ordem do documento. */
        @Size(max = 14, message = "Há linhas demais") @Valid List<LinhaLeito> leitos,
        @Size(max = 150, message = "Máximo de 150 caracteres") String leitoOutroDescricao,
        Boolean leitosNaoDisponivel,
        LocalDate dataExtracaoLeitos,
        Set<CentralRegulacao> centrais,
        @Size(max = 150, message = "Máximo de 150 caracteres") String centralOutra,
        /** 4.2: até 10 linhas. */
        @Size(max = 10, message = "Informe no máximo 10 linhas") @Valid List<Eletiva> eletivas,
        Boolean eletivasNaoDisponivel,
        LocalDate dataExtracaoEletivas,
        Long version,
        LocalDateTime atualizadoEm) implements BlocoDados<Bloco4Dados> {

    public static final int MAX_ELETIVAS = 10;

    public record LinhaLeito(
            @NotNull(message = "Informe o tipo de leito") TipoLeito tipo,
            Boolean naoDisponivel,
            @Contagem Integer solicitacoes,
            @Contagem Integer atendidas,
            @Contagem Integer canceladas,
            @Decimal BigDecimal tempoMedioHoras,
            @Contagem Integer aguardando) {

        static LinhaLeito vazia(TipoLeito tipo) {
            return new LinhaLeito(tipo, false, null, null, null, null, null);
        }

        LinhaLeito normalizada() {
            boolean semDado = Boolean.TRUE.equals(naoDisponivel);
            // "Não disponível" na linha zera os números: um dado ou o outro, nunca os dois.
            return semDado ? vazia(tipo).comNaoDisponivel()
                    : new LinhaLeito(tipo, false, solicitacoes, atendidas, canceladas, tempoMedioHoras, aguardando);
        }

        private LinhaLeito comNaoDisponivel() {
            return new LinhaLeito(tipo, true, null, null, null, null, null);
        }

        boolean preenchida() {
            return Normaliza.preenchido(solicitacoes, atendidas, canceladas, tempoMedioHoras, aguardando);
        }
    }

    public record Eletiva(
            @Size(max = 200, message = "Máximo de 200 caracteres") String especialidade,
            @Sigtap String sigtap,
            @Contagem Integer totalAguardando,
            @Decimal BigDecimal esperaMediaDias,
            @Contagem Integer maiorEsperaDias,
            OrigemDado origem,
            @Size(max = 150, message = "Máximo de 150 caracteres") String origemOutro) {

        Eletiva normalizada() {
            return new Eletiva(Normaliza.txt(especialidade), Normaliza.txt(sigtap), totalAguardando, esperaMediaDias,
                    maiorEsperaDias, origem, origem == OrigemDado.OUTRO ? Normaliza.txt(origemOutro) : null);
        }

        boolean preenchida() {
            return Normaliza.preenchido(especialidade, sigtap, totalAguardando, esperaMediaDias, maiorEsperaDias, origem);
        }
    }

    public static Bloco4Dados vazio() {
        return new Bloco4Dados(linhasVazias(), null, false, null, Set.of(), null, List.of(), false, null, null, null);
    }

    private static List<LinhaLeito> linhasVazias() {
        return Arrays.stream(TipoLeito.values()).map(LinhaLeito::vazia).toList();
    }

    @Override
    public Bloco4Dados comControle(Long novaVersao, LocalDateTime novoAtualizadoEm) {
        return new Bloco4Dados(leitos, leitoOutroDescricao, leitosNaoDisponivel, dataExtracaoLeitos, centrais,
                centralOutra, eletivas, eletivasNaoDisponivel, dataExtracaoEletivas, novaVersao, novoAtualizadoEm);
    }

    @Override
    public Bloco4Dados normalizado() {
        Set<CentralRegulacao> marcadas = Normaliza.conjunto(centrais);
        boolean tabelaSemDado = Boolean.TRUE.equals(leitosNaoDisponivel);
        List<LinhaLeito> linhas = Normaliza.linhasFixas(leitos, TipoLeito.values(), LinhaLeito::tipo, LinhaLeito::vazia)
                .stream().map(LinhaLeito::normalizada).toList();
        return new Bloco4Dados(
                tabelaSemDado ? linhasVazias() : linhas,
                Normaliza.txt(leitoOutroDescricao), tabelaSemDado, dataExtracaoLeitos,
                marcadas, marcadas.contains(CentralRegulacao.OUTRA) ? Normaliza.txt(centralOutra) : null,
                Boolean.TRUE.equals(eletivasNaoDisponivel) ? List.of() : Normaliza.lista(eletivas, Eletiva::normalizada),
                Boolean.TRUE.equals(eletivasNaoDisponivel), dataExtracaoEletivas, version, atualizadoEm);
    }

    @Override
    public void validarRegras() {
        Normaliza.semRepetidas(leitos, LinhaLeito::tipo, "leitos");
        Map<String, String> erros = new LinkedHashMap<>();
        Normaliza.naoFutura(erros, "dataExtracaoLeitos", dataExtracaoLeitos);
        Normaliza.naoFutura(erros, "dataExtracaoEletivas", dataExtracaoEletivas);
        Normaliza.lancarSeHouver(erros);
    }

    @Override
    public List<Pendencia> pendencias() {
        List<Pendencia> lista = new ArrayList<>();
        boolean semLeitos = Boolean.TRUE.equals(leitosNaoDisponivel);
        if (!semLeitos) {
            Normaliza.exigir(lista, 4, "dataExtracaoLeitos", "4.1: informe a data de extração dos dados", dataExtracaoLeitos);
            if (leitos != null) {
                for (int i = 0; i < leitos.size(); i++) {
                    LinhaLeito l = leitos.get(i);
                    if (l == null || l.tipo() == null || Boolean.TRUE.equals(l.naoDisponivel())) continue;
                    boolean outro = l.tipo() == TipoLeito.OUTRO;
                    // A linha "Outro" é opcional; começou a preencher, tem que completar.
                    if (outro && !l.preenchida() && Normaliza.vazio(leitoOutroDescricao)) continue;
                    String p = "leitos[" + i + "].";
                    String r = "4.1, " + (outro ? "linha \"Outro\"" : "linha " + (i + 1)) + ": ";
                    if (!outro && !l.preenchida()) {
                        lista.add(Pendencia.obrigatorio(4, p + "solicitacoes", r + "preencha a linha ou marque \"Não disponível\""));
                        continue;
                    }
                    if (outro) Normaliza.exigir(lista, 4, "leitoOutroDescricao", r + "descreva o tipo de leito", leitoOutroDescricao);
                    Normaliza.exigir(lista, 4, p + "solicitacoes", r + "informe as solicitações no período (ou marque \"Não disponível\" na linha)", l.solicitacoes());
                    Normaliza.exigir(lista, 4, p + "atendidas", r + "informe as atendidas", l.atendidas());
                    Normaliza.exigir(lista, 4, p + "canceladas", r + "informe as canceladas ou óbitos antes da vaga", l.canceladas());
                    Normaliza.exigir(lista, 4, p + "tempoMedioHoras", r + "informe o tempo médio até a vaga", l.tempoMedioHoras());
                    Normaliza.exigir(lista, 4, p + "aguardando", r + "informe quantos aguardam na data da extração", l.aguardando());
                }
            }
        }
        if (centrais == null || centrais.isEmpty()) {
            lista.add(Pendencia.obrigatorio(4, "centrais", "4.1: marque a central que recebe os pedidos"));
        } else if (centrais.contains(CentralRegulacao.OUTRA) && Normaliza.vazio(centralOutra)) {
            lista.add(Pendencia.obrigatorio(4, "centralOutra", "4.1: descreva a outra central"));
        }
        boolean semEletivas = Boolean.TRUE.equals(eletivasNaoDisponivel);
        boolean temEletiva = eletivas != null && eletivas.stream().anyMatch(e -> e != null && e.preenchida());
        if (!temEletiva && !semEletivas) {
            lista.add(Pendencia.obrigatorio(4, "eletivas", "4.2: preencha ao menos uma linha ou marque \"Não disponível\""));
        }
        if (!semEletivas) {
            Normaliza.exigir(lista, 4, "dataExtracaoEletivas", "4.2: informe a data de extração dos dados", dataExtracaoEletivas);
            if (eletivas != null) {
                for (int i = 0; i < eletivas.size(); i++) {
                    Eletiva e = eletivas.get(i);
                    if (e == null || !e.preenchida()) continue;
                    String p = "eletivas[" + i + "].";
                    String r = "4.2, linha " + (i + 1) + ": ";
                    Normaliza.exigir(lista, 4, p + "especialidade", r + "informe a especialidade ou o procedimento", e.especialidade());
                    // O código SIGTAP é opcional ("se houver").
                    Normaliza.exigir(lista, 4, p + "totalAguardando", r + "informe o total aguardando", e.totalAguardando());
                    Normaliza.exigir(lista, 4, p + "esperaMediaDias", r + "informe a espera média", e.esperaMediaDias());
                    Normaliza.exigir(lista, 4, p + "maiorEsperaDias", r + "informe a maior espera", e.maiorEsperaDias());
                    Normaliza.exigir(lista, 4, p + "origem", r + "informe a origem do dado", e.origem());
                }
            }
        }
        if (leitos != null) {
            for (int i = 0; i < leitos.size(); i++) {
                LinhaLeito l = leitos.get(i);
                if (l == null || l.tipo() == null) continue;
                int atendidasECanceladas = (l.atendidas() == null ? 0 : l.atendidas()) + (l.canceladas() == null ? 0 : l.canceladas());
                if (l.solicitacoes() != null && atendidasECanceladas > l.solicitacoes()) {
                    lista.add(Pendencia.coerencia(4, "leitos[" + i + "].atendidas",
                            "4.1, linha " + (i + 1) + ": atendidas mais canceladas é maior que as solicitações"));
                }
            }
        }
        if (eletivas != null) {
            for (int i = 0; i < eletivas.size(); i++) {
                Eletiva e = eletivas.get(i);
                if (e != null && Normaliza.maior(e.esperaMediaDias(), e.maiorEsperaDias())) {
                    lista.add(Pendencia.coerencia(4, "eletivas[" + i + "].esperaMediaDias",
                            "4.2, linha " + (i + 1) + ": a espera média é maior que a maior espera"));
                }
                if (e != null && e.origem() == OrigemDado.OUTRO && Normaliza.vazio(e.origemOutro())) {
                    lista.add(Pendencia.obrigatorio(4, "eletivas[" + i + "].origemOutro",
                            "4.2, linha " + (i + 1) + ": descreva a origem do dado"));
                }
            }
        }
        return lista;
    }
}
