package questionario.api.dto.bloco;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import questionario.api.dto.Pendencia;
import questionario.domain.enums.SimNaoParcial;
import questionario.domain.enums.SistemaRegulacao;
import questionario.domain.enums.TipoFila;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Bloco 3 do documento: fila da regulação ambulatorial (itens 3.1 a 3.3). */
public record Bloco3Dados(
        Set<SistemaRegulacao> sistemas,
        @Size(max = 150, message = "Máximo de 150 caracteres") String sistemaProprioNome,
        @Size(max = 150, message = "Máximo de 150 caracteres") String sistemaOutroNome,
        TipoFila tipoFila,
        SimNaoParcial classificacaoRisco,
        /** Mês e ano da última revisão da fila, "AAAA-MM". */
        @Pattern(regexp = "^$|^\\d{4}-(0[1-9]|1[0-2])$", message = "Use mês e ano (AAAA-MM)") String ultimaRevisao,
        LocalDate dataExtracao,
        Boolean filasNaoDisponivel,
        @Size(max = 15, message = "Informe no máximo 15 filas") @Valid List<Fila> filas,
        Long version,
        LocalDateTime atualizadoEm) implements BlocoDados<Bloco3Dados> {

    public static final int MAX_FILAS = 15;

    /** Uma linha da tabela 3.3. */
    public record Fila(
            @Size(max = 200, message = "Máximo de 200 caracteres") String especialidade,
            @Sigtap String sigtap,
            @Contagem Integer totalAguardando,
            @Contagem Integer prioridadeAlta,
            @Decimal BigDecimal esperaMediaDias,
            @Contagem Integer maiorEsperaDias,
            @Contagem Integer aguardandoMais180) {

        Fila normalizada() {
            return new Fila(Normaliza.txt(especialidade), Normaliza.txt(sigtap), totalAguardando, prioridadeAlta,
                    esperaMediaDias, maiorEsperaDias, aguardandoMais180);
        }

        boolean preenchida() {
            return Normaliza.preenchido(especialidade, sigtap, totalAguardando, prioridadeAlta, esperaMediaDias,
                    maiorEsperaDias, aguardandoMais180);
        }
    }

    public static Bloco3Dados vazio() {
        return new Bloco3Dados(Set.of(), null, null, null, null, null, null, false, List.of(), null, null);
    }

    @Override
    public Bloco3Dados comControle(Long novaVersao, LocalDateTime novoAtualizadoEm) {
        return new Bloco3Dados(sistemas, sistemaProprioNome, sistemaOutroNome, tipoFila, classificacaoRisco,
                ultimaRevisao, dataExtracao, filasNaoDisponivel, filas, novaVersao, novoAtualizadoEm);
    }

    @Override
    public Bloco3Dados normalizado() {
        Set<SistemaRegulacao> marcados = Normaliza.conjunto(sistemas);
        return new Bloco3Dados(
                marcados,
                marcados.contains(SistemaRegulacao.PROPRIO) ? Normaliza.txt(sistemaProprioNome) : null,
                marcados.contains(SistemaRegulacao.OUTRO) ? Normaliza.txt(sistemaOutroNome) : null,
                tipoFila, classificacaoRisco, Normaliza.txt(ultimaRevisao), dataExtracao,
                Boolean.TRUE.equals(filasNaoDisponivel),
                Boolean.TRUE.equals(filasNaoDisponivel) ? List.of() : Normaliza.lista(filas, Fila::normalizada),
                version, atualizadoEm);
    }

    @Override
    public void validarRegras() {
        // Sem regras entre campos que travem o rascunho: as coerências do 3.3 só avisam (ver pendencias()).
    }

    @Override
    public List<Pendencia> pendencias() {
        List<Pendencia> lista = new ArrayList<>();
        if (sistemas == null || sistemas.isEmpty()) {
            lista.add(Pendencia.obrigatorio(3, "sistemas", "3.1: marque ao menos um sistema de regulação"));
        }
        if (sistemas != null && sistemas.contains(SistemaRegulacao.PROPRIO) && Normaliza.vazio(sistemaProprioNome)) {
            lista.add(Pendencia.obrigatorio(3, "sistemaProprioNome", "3.1: informe qual é o sistema próprio"));
        }
        if (sistemas != null && sistemas.contains(SistemaRegulacao.OUTRO) && Normaliza.vazio(sistemaOutroNome)) {
            lista.add(Pendencia.obrigatorio(3, "sistemaOutroNome", "3.1: descreva o outro sistema"));
        }
        if (tipoFila == null) lista.add(Pendencia.obrigatorio(3, "tipoFila", "3.2: informe se a fila é única, por unidade ou mista"));
        if (classificacaoRisco == null) lista.add(Pendencia.obrigatorio(3, "classificacaoRisco", "3.2: informe se a fila tem classificação de risco"));
        boolean temFila = filas != null && filas.stream().anyMatch(f -> f != null && f.preenchida());
        if (!temFila && !Boolean.TRUE.equals(filasNaoDisponivel)) {
            lista.add(Pendencia.obrigatorio(3, "filas", "3.3: preencha ao menos uma fila ou marque \"Não disponível\""));
        }
        if (filas != null) {
            for (int i = 0; i < filas.size(); i++) {
                Fila f = filas.get(i);
                if (f == null) continue;
                String linha = "3.3, linha " + (i + 1) + ": ";
                if (Normaliza.maior(f.prioridadeAlta(), f.totalAguardando())) {
                    lista.add(Pendencia.coerencia(3, "filas[" + i + "].prioridadeAlta", linha + "com prioridade alta é maior que o total aguardando"));
                }
                if (Normaliza.maior(f.aguardandoMais180(), f.totalAguardando())) {
                    lista.add(Pendencia.coerencia(3, "filas[" + i + "].aguardandoMais180", linha + "aguardando há mais de 180 dias é maior que o total aguardando"));
                }
                if (Normaliza.maior(f.esperaMediaDias(), f.maiorEsperaDias())) {
                    lista.add(Pendencia.coerencia(3, "filas[" + i + "].esperaMediaDias", linha + "a espera média é maior que a maior espera"));
                }
            }
        }
        return lista;
    }
}
