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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
        Map<String, String> erros = new LinkedHashMap<>();
        Normaliza.naoFutura(erros, "dataExtracaoUnidades", dataExtracaoUnidades);
        Normaliza.lancarSeHouver(erros);
    }

    @Override
    public List<Pendencia> pendencias() {
        List<Pendencia> lista = new ArrayList<>();
        boolean semUnidades = Boolean.TRUE.equals(unidadesNaoDisponivel);
        boolean temUnidade = unidades != null && unidades.stream().anyMatch(u -> u != null && u.preenchida());
        if (!temUnidade && !semUnidades) {
            lista.add(Pendencia.obrigatorio(5, "unidades", "5.1: informe ao menos uma unidade ou marque \"Não disponível\""));
        }
        if (!semUnidades) {
            Normaliza.exigir(lista, 5, "dataExtracaoUnidades", "5.1: informe a data de extração dos dados", dataExtracaoUnidades);
            if (unidades != null) {
                for (int i = 0; i < unidades.size(); i++) {
                    Unidade u = unidades.get(i);
                    if (u == null || !u.preenchida()) continue;
                    String p = "unidades[" + i + "].";
                    String r = "5.1, unidade " + (i + 1) + ": ";
                    Normaliza.exigir(lista, 5, p + "nome", r + "informe o nome da unidade", u.nome());
                    Normaliza.exigir(lista, 5, p + "cnes", r + "informe o CNES", u.cnes());
                    Normaliza.exigir(lista, 5, p + "tipo", r + "informe o tipo da unidade", u.tipo());
                    Normaliza.exigir(lista, 5, p + "leitosObservacao", r + "informe os leitos de observação", u.leitosObservacao());
                    Normaliza.exigir(lista, 5, p + "pacientes24h", r + "informe os pacientes há mais de 24 h", u.pacientes24h());
                    Normaliza.exigir(lista, 5, p + "pacientes72h", r + "informe quantos destes estão há mais de 72 h", u.pacientes72h());
                    Normaliza.exigir(lista, 5, p + "ventilacaoMecanica", r + "informe os pacientes em ventilação mecânica", u.ventilacaoMecanica());
                }
            }
        }
        if (!Boolean.TRUE.equals(perfilNaoDisponivel) && perfil != null) {
            for (int i = 0; i < perfil.size(); i++) {
                Perfil pf = perfil.get(i);
                if (pf == null || pf.necessidade() == null || Boolean.TRUE.equals(pf.naoDisponivel())) continue;
                boolean outro = pf.necessidade() == NecessidadePaciente.OUTRO;
                // A linha "Outro" é opcional; começou a preencher, tem que completar.
                if (outro && !pf.preenchido() && Normaliza.vazio(perfilOutroDescricao)) continue;
                String p = "perfil[" + i + "].";
                String r = "5.2, " + (outro ? "linha \"Outro\"" : "linha " + (i + 1)) + ": ";
                if (!outro && !pf.preenchido()) {
                    lista.add(Pendencia.obrigatorio(5, p + "pacientes", r + "preencha a linha ou marque \"Não disponível\""));
                    continue;
                }
                if (outro) Normaliza.exigir(lista, 5, "perfilOutroDescricao", r + "descreva a necessidade", perfilOutroDescricao);
                Normaliza.exigir(lista, 5, p + "pacientes", r + "informe o número de pacientes (0 se não houver; ou marque \"Não disponível\" na linha)", pf.pacientes());
                // Sem pacientes não há diagnóstico a informar.
                if (pf.pacientes() == null || pf.pacientes() > 0) {
                    Normaliza.exigir(lista, 5, p + "diagnosticos", r + "informe os principais diagnósticos (CID-10)", pf.diagnosticos());
                }
            }
        }
        if (!Boolean.TRUE.equals(indicadoresNaoDisponivel)) {
            Normaliza.exigir(lista, 5, "pacientesMais24h", "5.3: informe os pacientes que ficaram mais de 24 h aguardando vaga (ou marque \"Não disponível\")", pacientesMais24h);
            Normaliza.exigir(lista, 5, "tempoMedioPermanenciaHoras", "5.3: informe o tempo médio de permanência", tempoMedioPermanenciaHoras);
            Normaliza.exigir(lista, 5, "obitosAguardandoVaga", "5.3: informe os óbitos de quem aguardava vaga", obitosAguardandoVaga);
            Normaliza.exigir(lista, 5, "altaSemProcedimento", "5.3: informe as altas sem o procedimento indicado", altaSemProcedimento);
            Normaliza.exigir(lista, 5, "registroInformatizado", "5.3: informe se há registro informatizado do tempo de permanência", registroInformatizado);
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
