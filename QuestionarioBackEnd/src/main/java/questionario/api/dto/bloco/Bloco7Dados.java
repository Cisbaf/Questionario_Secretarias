package questionario.api.dto.bloco;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import questionario.api.dto.Pendencia;
import questionario.domain.enums.AparelhoImagem;
import questionario.domain.enums.SimNaoParcial;
import questionario.domain.enums.TipoLeitoPrivado;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Bloco 7 do documento: financiamento e capacidade (itens 7.1 a 7.3). Três listas variáveis. */
public record Bloco7Dados(
        @Size(max = 20, message = "Informe no máximo 20 linhas") @Valid List<Repasse> repasses,
        Boolean repassesNaoDisponivel,
        @Size(max = 20, message = "Informe no máximo 20 linhas") @Valid List<LeitoPrivado> leitosPrivados,
        Boolean leitosPrivadosNaoDisponivel,
        @Size(max = 20, message = "Informe no máximo 20 linhas") @Valid List<Aparelho> aparelhos,
        Boolean aparelhosNaoDisponivel,
        Long version,
        LocalDateTime atualizadoEm) implements BlocoDados<Bloco7Dados> {

    public static final int MAX_LINHAS = 20;

    public record Repasse(
            @Size(max = 200, message = "Máximo de 200 caracteres") String unidade,
            @Cnes String cnes,
            @Size(max = 100, message = "Máximo de 100 caracteres") String atoNumero,
            LocalDate atoData,
            @Dinheiro BigDecimal valorMensal,
            LocalDate vigenciaAte,
            SimNaoParcial repasseEmDia,
            @Size(max = 500, message = "Máximo de 500 caracteres") String observacao) {

        Repasse normalizado() {
            return new Repasse(Normaliza.txt(unidade), Normaliza.txt(cnes), Normaliza.txt(atoNumero), atoData,
                    valorMensal, vigenciaAte, repasseEmDia, Normaliza.txt(observacao));
        }

        boolean preenchido() {
            return Normaliza.preenchido(unidade, cnes, atoNumero, atoData, valorMensal, vigenciaAte, repasseEmDia, observacao);
        }
    }

    public record LeitoPrivado(
            @Size(max = 200, message = "Máximo de 200 caracteres") String hospital,
            @Cnes String cnes,
            TipoLeitoPrivado tipoLeito,
            @Contagem Integer contratadosSus,
            @Contagem Integer poderiamSerContratados,
            @Dinheiro BigDecimal valorDiaria,
            @Size(max = 500, message = "Máximo de 500 caracteres") String observacao) {

        LeitoPrivado normalizado() {
            return new LeitoPrivado(Normaliza.txt(hospital), Normaliza.txt(cnes), tipoLeito, contratadosSus,
                    poderiamSerContratados, valorDiaria, Normaliza.txt(observacao));
        }

        boolean preenchido() {
            return Normaliza.preenchido(hospital, cnes, tipoLeito, contratadosSus, poderiamSerContratados, valorDiaria, observacao);
        }
    }

    public record Aparelho(
            @Size(max = 200, message = "Máximo de 200 caracteres") String estabelecimento,
            @Cnes String cnes,
            AparelhoImagem aparelho,
            @Contagem Integer quantidadeCnes,
            @Contagem Integer emUsoDeFato,
            SimNaoParcial atendeSus,
            @Size(max = 500, message = "Máximo de 500 caracteres") String correcaoNecessaria) {

        Aparelho normalizado() {
            return new Aparelho(Normaliza.txt(estabelecimento), Normaliza.txt(cnes), aparelho, quantidadeCnes,
                    emUsoDeFato, atendeSus, Normaliza.txt(correcaoNecessaria));
        }

        boolean preenchido() {
            return Normaliza.preenchido(estabelecimento, cnes, aparelho, quantidadeCnes, emUsoDeFato, atendeSus, correcaoNecessaria);
        }
    }

    public static Bloco7Dados vazio() {
        return new Bloco7Dados(List.of(), false, List.of(), false, List.of(), false, null, null);
    }

    @Override
    public Bloco7Dados comControle(Long novaVersao, LocalDateTime novoAtualizadoEm) {
        return new Bloco7Dados(repasses, repassesNaoDisponivel, leitosPrivados, leitosPrivadosNaoDisponivel,
                aparelhos, aparelhosNaoDisponivel, novaVersao, novoAtualizadoEm);
    }

    @Override
    public Bloco7Dados normalizado() {
        boolean semRepasses = Boolean.TRUE.equals(repassesNaoDisponivel);
        boolean semLeitos = Boolean.TRUE.equals(leitosPrivadosNaoDisponivel);
        boolean semAparelhos = Boolean.TRUE.equals(aparelhosNaoDisponivel);
        return new Bloco7Dados(
                semRepasses ? List.of() : Normaliza.lista(repasses, Repasse::normalizado), semRepasses,
                semLeitos ? List.of() : Normaliza.lista(leitosPrivados, LeitoPrivado::normalizado), semLeitos,
                semAparelhos ? List.of() : Normaliza.lista(aparelhos, Aparelho::normalizado), semAparelhos,
                version, atualizadoEm);
    }

    @Override
    public void validarRegras() {
        Map<String, String> erros = new LinkedHashMap<>();
        if (repasses != null) {
            for (int i = 0; i < repasses.size(); i++) {
                if (repasses.get(i) != null) Normaliza.naoFutura(erros, "repasses[" + i + "].atoData", repasses.get(i).atoData());
            }
        }
        Normaliza.lancarSeHouver(erros);
    }

    @Override
    public List<Pendencia> pendencias() {
        List<Pendencia> lista = new ArrayList<>();
        if ((repasses == null || repasses.stream().noneMatch(r -> r != null && r.preenchido())) && !Boolean.TRUE.equals(repassesNaoDisponivel)) {
            lista.add(Pendencia.obrigatorio(7, "repasses", "7.1: informe ao menos um repasse ou marque \"Não disponível\""));
        }
        if ((leitosPrivados == null || leitosPrivados.stream().noneMatch(l -> l != null && l.preenchido())) && !Boolean.TRUE.equals(leitosPrivadosNaoDisponivel)) {
            lista.add(Pendencia.obrigatorio(7, "leitosPrivados", "7.2: informe ao menos um hospital ou marque \"Não disponível\""));
        }
        if ((aparelhos == null || aparelhos.stream().noneMatch(a -> a != null && a.preenchido())) && !Boolean.TRUE.equals(aparelhosNaoDisponivel)) {
            lista.add(Pendencia.obrigatorio(7, "aparelhos", "7.3: informe ao menos um estabelecimento ou marque \"Não disponível\""));
        }
        // Linha começada, linha completa. A observação (e a correção necessária) é livre e fica opcional.
        if (repasses != null && !Boolean.TRUE.equals(repassesNaoDisponivel)) {
            for (int i = 0; i < repasses.size(); i++) {
                Repasse r = repasses.get(i);
                if (r == null || !r.preenchido()) continue;
                String p = "repasses[" + i + "].";
                String m = "7.1, linha " + (i + 1) + ": ";
                Normaliza.exigir(lista, 7, p + "unidade", m + "informe a unidade ou o serviço", r.unidade());
                Normaliza.exigir(lista, 7, p + "cnes", m + "informe o CNES", r.cnes());
                Normaliza.exigir(lista, 7, p + "atoNumero", m + "informe o número do ato", r.atoNumero());
                Normaliza.exigir(lista, 7, p + "atoData", m + "informe a data do ato", r.atoData());
                Normaliza.exigir(lista, 7, p + "valorMensal", m + "informe o valor mensal", r.valorMensal());
                Normaliza.exigir(lista, 7, p + "vigenciaAte", m + "informe até quando vale", r.vigenciaAte());
                Normaliza.exigir(lista, 7, p + "repasseEmDia", m + "informe se o repasse está em dia", r.repasseEmDia());
            }
        }
        if (leitosPrivados != null && !Boolean.TRUE.equals(leitosPrivadosNaoDisponivel)) {
            for (int i = 0; i < leitosPrivados.size(); i++) {
                LeitoPrivado l = leitosPrivados.get(i);
                if (l == null || !l.preenchido()) continue;
                String p = "leitosPrivados[" + i + "].";
                String m = "7.2, linha " + (i + 1) + ": ";
                Normaliza.exigir(lista, 7, p + "hospital", m + "informe o hospital", l.hospital());
                Normaliza.exigir(lista, 7, p + "cnes", m + "informe o CNES", l.cnes());
                Normaliza.exigir(lista, 7, p + "tipoLeito", m + "informe o tipo de leito", l.tipoLeito());
                Normaliza.exigir(lista, 7, p + "contratadosSus", m + "informe os leitos já contratados pelo SUS", l.contratadosSus());
                Normaliza.exigir(lista, 7, p + "poderiamSerContratados", m + "informe os leitos que poderiam ser contratados", l.poderiamSerContratados());
                Normaliza.exigir(lista, 7, p + "valorDiaria", m + "informe o valor pago por diária", l.valorDiaria());
            }
        }
        if (aparelhos != null && !Boolean.TRUE.equals(aparelhosNaoDisponivel)) {
            for (int i = 0; i < aparelhos.size(); i++) {
                Aparelho a = aparelhos.get(i);
                if (a == null || !a.preenchido()) continue;
                String p = "aparelhos[" + i + "].";
                String m = "7.3, linha " + (i + 1) + ": ";
                Normaliza.exigir(lista, 7, p + "estabelecimento", m + "informe o estabelecimento", a.estabelecimento());
                Normaliza.exigir(lista, 7, p + "cnes", m + "informe o CNES", a.cnes());
                Normaliza.exigir(lista, 7, p + "aparelho", m + "informe o aparelho", a.aparelho());
                Normaliza.exigir(lista, 7, p + "quantidadeCnes", m + "informe a quantidade no CNES", a.quantidadeCnes());
                Normaliza.exigir(lista, 7, p + "emUsoDeFato", m + "informe quantos estão em uso de fato", a.emUsoDeFato());
                Normaliza.exigir(lista, 7, p + "atendeSus", m + "informe se atende o SUS", a.atendeSus());
            }
        }
        if (aparelhos != null) {
            for (int i = 0; i < aparelhos.size(); i++) {
                Aparelho a = aparelhos.get(i);
                if (a != null && Normaliza.maior(a.emUsoDeFato(), a.quantidadeCnes())) {
                    lista.add(Pendencia.coerencia(7, "aparelhos[" + i + "].emUsoDeFato",
                            "7.3, linha " + (i + 1) + ": em uso de fato é maior que a quantidade no CNES"));
                }
            }
        }
        return lista;
    }
}
