package questionario.api.dto.bloco;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import questionario.api.dto.Pendencia;
import questionario.domain.enums.SituacaoClinica;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Bloco 6 do documento: seguimento após a urgência. Uma linha por situação clínica, na ordem do documento. */
public record Bloco6Dados(
        @Size(max = 9, message = "Há linhas demais") @Valid List<Seguimento> situacoes,
        Boolean tabelaNaoDisponivel,
        Long version,
        LocalDateTime atualizadoEm) implements BlocoDados<Bloco6Dados> {

    public record Seguimento(
            @NotNull(message = "Informe a situação clínica") SituacaoClinica situacao,
            /** Só vale para OUTRO: nas demais o texto da situação e do procedimento está escrito no documento. */
            @Size(max = 200, message = "Máximo de 200 caracteres") String situacaoOutra,
            @Size(max = 200, message = "Máximo de 200 caracteres") String procedimentoOutro,
            Boolean naoDisponivel,
            @Contagem Integer pacientesAguardando,
            @Decimal BigDecimal esperaMediaDias,
            @Size(max = 300, message = "Máximo de 300 caracteres") String ondeFeito) {

        static Seguimento vazio(SituacaoClinica situacao) {
            return new Seguimento(situacao, null, null, false, null, null, null);
        }

        Seguimento normalizado() {
            boolean outro = situacao == SituacaoClinica.OUTRO;
            String sit = outro ? Normaliza.txt(situacaoOutra) : null;
            String proc = outro ? Normaliza.txt(procedimentoOutro) : null;
            return Boolean.TRUE.equals(naoDisponivel)
                    ? new Seguimento(situacao, sit, proc, true, null, null, null)
                    : new Seguimento(situacao, sit, proc, false, pacientesAguardando, esperaMediaDias, Normaliza.txt(ondeFeito));
        }

        boolean preenchida() {
            return Normaliza.preenchido(pacientesAguardando, esperaMediaDias, ondeFeito);
        }
    }

    public static Bloco6Dados vazio() {
        return new Bloco6Dados(linhasVazias(), false, null, null);
    }

    private static List<Seguimento> linhasVazias() {
        return Arrays.stream(SituacaoClinica.values()).map(Seguimento::vazio).toList();
    }

    @Override
    public Bloco6Dados comControle(Long novaVersao, LocalDateTime novoAtualizadoEm) {
        return new Bloco6Dados(situacoes, tabelaNaoDisponivel, novaVersao, novoAtualizadoEm);
    }

    @Override
    public Bloco6Dados normalizado() {
        boolean semTabela = Boolean.TRUE.equals(tabelaNaoDisponivel);
        List<Seguimento> linhas = Normaliza.linhasFixas(situacoes, SituacaoClinica.values(), Seguimento::situacao, Seguimento::vazio)
                .stream().map(Seguimento::normalizado).toList();
        return new Bloco6Dados(semTabela ? linhasVazias() : linhas, semTabela, version, atualizadoEm);
    }

    @Override
    public void validarRegras() {
        Normaliza.semRepetidas(situacoes, Seguimento::situacao, "situacoes");
    }

    @Override
    public List<Pendencia> pendencias() {
        List<Pendencia> lista = new ArrayList<>();
        if (!Boolean.TRUE.equals(tabelaNaoDisponivel) && situacoes != null) {
            for (int i = 0; i < situacoes.size(); i++) {
                Seguimento s = situacoes.get(i);
                if (s == null || s.situacao() == null || Boolean.TRUE.equals(s.naoDisponivel())) continue;
                boolean outro = s.situacao() == SituacaoClinica.OUTRO;
                // A linha "Outro" é opcional; começou a preencher, tem que completar.
                if (outro && !s.preenchida() && Normaliza.vazio(s.situacaoOutra()) && Normaliza.vazio(s.procedimentoOutro())) continue;
                String p = "situacoes[" + i + "].";
                String r = outro ? "Bloco 6, \"Outro\": " : "Bloco 6, linha " + (i + 1) + ": ";
                if (!outro && !s.preenchida()) {
                    lista.add(Pendencia.obrigatorio(6, p + "pacientesAguardando", r + "preencha a linha ou marque \"Não disponível\""));
                    continue;
                }
                if (outro) {
                    Normaliza.exigir(lista, 6, p + "situacaoOutra", r + "descreva a situação clínica", s.situacaoOutra());
                    Normaliza.exigir(lista, 6, p + "procedimentoOutro", r + "descreva o procedimento de seguimento", s.procedimentoOutro());
                }
                Normaliza.exigir(lista, 6, p + "pacientesAguardando", r + "informe os pacientes aguardando (ou marque \"Não disponível\" na linha)", s.pacientesAguardando());
                Normaliza.exigir(lista, 6, p + "esperaMediaDias", r + "informe a espera média", s.esperaMediaDias());
                Normaliza.exigir(lista, 6, p + "ondeFeito", r + "informe onde o procedimento é feito hoje", s.ondeFeito());
            }
        }
        return lista;
    }
}
