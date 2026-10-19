package questionario.api.dto.bloco;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import questionario.api.dto.Pendencia;
import questionario.domain.enums.MotivoSemVaga;
import questionario.domain.enums.NecessidadePaciente;
import questionario.domain.enums.SimNaoParcial;
import questionario.domain.enums.TipoUnidadeUrgencia;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Bloco 5 do documento: pacientes nas UPAs e prontos-socorros sem vaga (itens 5.1 a 5.4). Só dados agregados: nenhum
 * campo de identificação de paciente (LGPD).
 */
public record Bloco5Dados(
        /** 5.1: unidades de urgência (lista variável). */
        @Size(max = 30, message = "Informe no máximo 30 unidades") @Valid List<Unidade> unidades,
        Boolean unidadesNaoDisponivel,
        LocalDate dataExtracaoUnidades,
        /** 5.2: uma linha por necessidade, na ordem do documento. */
        @Size(max = 13, message = "Há linhas demais") @Valid List<Perfil> perfil,
        @Size(max = 150, message = "Máximo de 150 caracteres") String perfilOutroDescricao,
        Boolean perfilNaoDisponivel,
        /** 5.3: indicadores do período. */
        @Contagem Integer pacientesMais24h,
        @Decimal BigDecimal tempoMedioPermanenciaHoras,
        @Contagem Integer obitosAguardandoVaga,
        @Contagem Integer altaSemProcedimento,
        SimNaoParcial registroInformatizado,
        Boolean indicadoresNaoDisponivel,
        /** 5.4 */
        Set<MotivoSemVaga> motivos,
        @Size(max = 300, message = "Máximo de 300 caracteres") String motivoOutro,
        Long version,
        LocalDateTime atualizadoEm) implements BlocoDados<Bloco5Dados> {

    public record Unidade(
            @Size(max = 200, message = "Máximo de 200 caracteres") String nome,
            @Cnes String cnes,
            TipoUnidadeUrgencia tipo,
            @Contagem Integer leitosObservacao,
            @Contagem Integer pacientes24h,
            @Contagem Integer pacientes72h,
            @Contagem Integer ventilacaoMecanica) {

        Unidade normalizada() {
            return new Unidade(Normaliza.txt(nome), Normaliza.txt(cnes), tipo, leitosObservacao, pacientes24h,
                    pacientes72h, ventilacaoMecanica);
        }

        boolean preenchida() {
            return Normaliza.preenchido(nome, cnes, tipo, leitosObservacao, pacientes24h, pacientes72h, ventilacaoMecanica);
        }
    }

    public record Perfil(
            @NotNull(message = "Informe a necessidade") NecessidadePaciente necessidade,
            Boolean naoDisponivel,
            @Contagem Integer pacientes,
            /** Principais diagnósticos (CID-10), texto livre: só o formato é conferido pelo front. */
            @Size(max = 300, message = "Máximo de 300 caracteres") String diagnosticos) {

        static Perfil vazio(NecessidadePaciente necessidade) {
            return new Perfil(necessidade, false, null, null);
        }

        Perfil normalizado() {
            return Boolean.TRUE.equals(naoDisponivel)
                    ? new Perfil(necessidade, true, null, null)
                    : new Perfil(necessidade, false, pacientes, Normaliza.txt(diagnosticos));
        }

        boolean preenchido() {
            return Normaliza.preenchido(pacientes, diagnosticos);
        }
    }

    public static Bloco5Dados vazio() {
        return new Bloco5Dados(List.of(), false, null, perfilVazio(), null, false, null, null, null, null, null, false,
                Set.of(), null, null, null);
    }

    private static List<Perfil> perfilVazio() {
        return Arrays.stream(NecessidadePaciente.values()).map(Perfil::vazio).toList();
    }

    @Override
    public Bloco5Dados comControle(Long novaVersao, LocalDateTime novoAtualizadoEm) {
        return new Bloco5Dados(unidades, unidadesNaoDisponivel, dataExtracaoUnidades, perfil, perfilOutroDescricao,
                perfilNaoDisponivel, pacientesMais24h, tempoMedioPermanenciaHoras, obitosAguardandoVaga,
                altaSemProcedimento, registroInformatizado, indicadoresNaoDisponivel, motivos, motivoOutro,
                novaVersao, novoAtualizadoEm);
    }

    @Override
    public Bloco5Dados normalizado() {
        Set<MotivoSemVaga> marcados = Normaliza.conjunto(motivos);
        boolean semUnidades = Boolean.TRUE.equals(unidadesNaoDisponivel);
        boolean semPerfil = Boolean.TRUE.equals(perfilNaoDisponivel);
        boolean semIndicadores = Boolean.TRUE.equals(indicadoresNaoDisponivel);
        List<Perfil> linhas = Normaliza.linhasFixas(perfil, NecessidadePaciente.values(), Perfil::necessidade, Perfil::vazio)
                .stream().map(Perfil::normalizado).toList();
        return new Bloco5Dados(
                semUnidades ? List.of() : Normaliza.lista(unidades, Unidade::normalizada), semUnidades, dataExtracaoUnidades,
                semPerfil ? perfilVazio() : linhas, Normaliza.txt(perfilOutroDescricao), semPerfil,
                semIndicadores ? null : pacientesMais24h, semIndicadores ? null : tempoMedioPermanenciaHoras,
                semIndicadores ? null : obitosAguardandoVaga, semIndicadores ? null : altaSemProcedimento,
                semIndicadores ? null : registroInformatizado, semIndicadores,
                marcados, marcados.contains(MotivoSemVaga.OUTRO) ? Normaliza.txt(motivoOutro) : null,
                version, atualizadoEm);
    }

    @Override
    public void validarRegras() {
        Normaliza.semRepetidas(perfil, Perfil::necessidade, "perfil");
    }

    @Override
    public List<Pendencia> pendencias() {
        List<Pendencia> lista = new ArrayList<>();
        boolean temUnidade = unidades != null && unidades.stream().anyMatch(u -> u != null && u.preenchida());
        if (!temUnidade && !Boolean.TRUE.equals(unidadesNaoDisponivel)) {
            lista.add(Pendencia.obrigatorio(5, "unidades", "5.1: informe ao menos uma unidade ou marque \"Não disponível\""));
        }
        boolean temPerfil = perfil != null && perfil.stream().anyMatch(p -> p != null && (p.preenchido() || Boolean.TRUE.equals(p.naoDisponivel())));
        if (!temPerfil && !Boolean.TRUE.equals(perfilNaoDisponivel)) {
            lista.add(Pendencia.obrigatorio(5, "perfil", "5.2: preencha ao menos uma linha ou marque \"Não disponível\""));
        }
        boolean temIndicador = Normaliza.preenchido(pacientesMais24h, tempoMedioPermanenciaHoras, obitosAguardandoVaga,
                altaSemProcedimento, registroInformatizado);
        if (!temIndicador && !Boolean.TRUE.equals(indicadoresNaoDisponivel)) {
            lista.add(Pendencia.obrigatorio(5, "pacientesMais24h", "5.3: preencha ao menos um indicador ou marque \"Não disponível\""));
        }
        if (motivos == null || motivos.isEmpty()) {
            lista.add(Pendencia.obrigatorio(5, "motivos", "5.4: marque ao menos um motivo"));
        } else if (motivos.contains(MotivoSemVaga.OUTRO) && Normaliza.vazio(motivoOutro)) {
            lista.add(Pendencia.obrigatorio(5, "motivoOutro", "5.4: descreva o outro motivo"));
        }
        if (unidades != null) {
            for (int i = 0; i < unidades.size(); i++) {
                Unidade u = unidades.get(i);
                if (u != null && Normaliza.maior(u.pacientes72h(), u.pacientes24h())) {
                    lista.add(Pendencia.coerencia(5, "unidades[" + i + "].pacientes72h",
                            "5.1, unidade " + (i + 1) + ": pacientes há mais de 72 h é maior que os há mais de 24 h"));
                }
            }
        }
        return lista;
    }
}
