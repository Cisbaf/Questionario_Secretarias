package questionario.service.excel;

import questionario.api.dto.Bloco1Response;
import questionario.api.dto.Bloco2Response;
import questionario.api.dto.PrioridadeDto;
import questionario.api.dto.RespostasResponse;
import questionario.api.dto.bloco.Bloco3Dados;
import questionario.api.dto.bloco.Bloco4Dados;
import questionario.api.dto.bloco.Bloco5Dados;
import questionario.api.dto.bloco.Bloco6Dados;
import questionario.api.dto.bloco.Bloco7Dados;
import questionario.api.dto.bloco.Bloco8Dados;
import questionario.domain.enums.CentralRegulacao;
import questionario.domain.enums.LinhaCuidado;
import questionario.domain.enums.MotivoSemVaga;
import questionario.domain.enums.NecessidadePaciente;
import questionario.domain.enums.OrigemDado;
import questionario.domain.enums.SistemaRegulacao;
import questionario.domain.enums.SituacaoClinica;
import questionario.domain.enums.TemaUrgencia;
import questionario.domain.enums.TipoLeito;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Transforma as respostas de um questionario nas tabelas do documento. E a unica fonte das duas planilhas: a de um
 * municipio usa as tabelas como estao; o consolidado empilha, aba a aba, as linhas de todos os municipios.
 */
final class TabelasDoQuestionario {

    static final String NAO_DISPONIVEL = "Não disponível";

    private TabelasDoQuestionario() {
    }

    static List<Tabela> de(RespostasResponse r) {
        List<Tabela> t = new ArrayList<>();
        bloco1(t, r.bloco1(), r.anexos());
        bloco2(t, r.bloco2());
        bloco3(t, r.bloco3());
        bloco4(t, r.bloco4());
        bloco5(t, r.bloco5());
        bloco6(t, r.bloco6());
        bloco7(t, r.bloco7());
        bloco8(t, r.bloco8(), r.anexos());
        return t;
    }

    // ---------- Bloco 1 e 2 ----------

    private static void bloco1(List<Tabela> t, Bloco1Response b, List<questionario.api.dto.AnexoResponse> anexos) {
        t.add(Tabela.nova("1 Identificação", "Município", "Secretário(a) Municipal de Saúde", "Responsável pelo preenchimento",
                "Cargo / setor", "E-mail", "Telefone / WhatsApp", "Data do preenchimento")
                .linha(b.municipio(), b.secretario(), b.responsavel(), b.cargoSetor(), b.email(), b.telefone(), b.dataPreenchimento()));
        Tabela planilha = Tabela.nova("1 Planilha de coleta", "Arquivo enviado", "Tamanho (KB)", "Enviada em");
        for (var x : nz(anexos)) {
            if (x.documento() == questionario.domain.enums.DocumentoAnexavel.PLANILHA_COLETA) {
                planilha.linha(x.nome(), (int) Math.max(1, Math.round(x.tamanho() / 1024.0)), Formato.dataHora(x.criadoEm()));
            }
        }
        t.add(planilha);
    }

    private static void bloco2(List<Tabela> t, Bloco2Response b) {
        Tabela p = Tabela.nova("2.1 Prioridades", "Nº", "Prioridade", "Qual problema ela responde", "O município resolve sozinho?");
        List<PrioridadeDto> prioridades = b.prioridades();
        for (int i = 0; i < prioridades.size(); i++) {
            PrioridadeDto d = prioridades.get(i);
            if (d.titulo() == null && d.problema() == null && d.resolveSozinho() == null) continue;
            p.linha(i + 1, d.titulo(), d.problema(), Rotulos.de(d.resolveSozinho()));
        }
        t.add(p);

        Tabela g = Tabela.nova("2.2 Linhas de cuidado", "Linha de cuidado", "Grau de prioridade (1 a 5)");
        for (LinhaCuidado linha : LinhaCuidado.values()) {
            Integer grau = b.grausLinhaCuidado().get(linha);
            if (linha == LinhaCuidado.OUTRA) {
                if (grau != null || !vazio(b.linhaOutraDescricao())) g.linha("Outra: " + nz(b.linhaOutraDescricao()), grau);
            } else {
                g.linha(Rotulos.de(linha), grau);
            }
        }
        t.add(g);

        String impedimentos = b.impedimentos().stream().sorted().map(Rotulos::de).collect(Collectors.joining("; "));
        if (!vazio(b.impedimentoOutro())) impedimentos += (impedimentos.isEmpty() ? "" : "; ") + "Outro: " + b.impedimentoOutro();
        t.add(Tabela.nova("2.3 a 2.5 Respostas", "Maiores gargalos de acesso (2.3)", "Serviços para atender os municípios vizinhos (2.4)",
                "O que mais impede ampliar a oferta (2.5)").linha(b.gargalos(), b.servicosParaVizinhos(), impedimentos));

        Tabela o = Tabela.nova("2.6 Ordem dos temas", "Tema", "Ordem (1 = mais urgente)");
        for (TemaUrgencia tema : TemaUrgencia.values()) o.linha(Rotulos.de(tema), b.ordemTemas().get(tema));
        t.add(o);
    }

    // ---------- Bloco 3 ----------

    private static void bloco3(List<Tabela> t, Bloco3Dados b) {
        String sistemas = ordenados(b.sistemas()).stream().map(s -> {
            if (s == SistemaRegulacao.PROPRIO) return Rotulos.de(s) + (vazio(b.sistemaProprioNome()) ? "" : ": " + b.sistemaProprioNome());
            if (s == SistemaRegulacao.OUTRO) return Rotulos.de(s) + (vazio(b.sistemaOutroNome()) ? "" : ": " + b.sistemaOutroNome());
            return Rotulos.de(s);
        }).collect(Collectors.joining("; "));
        t.add(Tabela.nova("3.1 e 3.2 Fila", "Sistemas de regulação (3.1)", "A fila é única, por unidade ou mista?",
                "Classificação de risco ou prioridade clínica?", "Última revisão da fila (mês/ano)")
                .linha(sistemas, Rotulos.de(b.tipoFila()), Rotulos.simNaoEmParte(b.classificacaoRisco()), mesAno(b.ultimaRevisao())));

        Tabela f = Tabela.nova("3.3 Filas", "Especialidade / exame", "Código SIGTAP", "Total aguardando", "Com prioridade alta",
                "Espera média (dias)", "Maior espera (dias)", "Aguardando há mais de 180 dias", "Data de extração");
        if (Boolean.TRUE.equals(b.filasNaoDisponivel())) {
            f.linha(NAO_DISPONIVEL, null, null, null, null, null, null, b.dataExtracao());
        } else {
            for (Bloco3Dados.Fila x : nz(b.filas())) {
                f.linha(x.especialidade(), x.sigtap(), x.totalAguardando(), x.prioridadeAlta(), x.esperaMediaDias(),
                        x.maiorEsperaDias(), x.aguardandoMais180(), b.dataExtracao());
            }
        }
        t.add(f);
    }

    // ---------- Bloco 4 ----------

    private static void bloco4(List<Tabela> t, Bloco4Dados b) {
        Tabela l = Tabela.nova("4.1 Pedidos de leito", "Tipo de leito", "Solicitações no período", "Atendidas (vaga concedida)",
                "Canceladas ou óbito antes da vaga", "Tempo médio até a vaga (horas)", "Aguardando na data da extração", "Data de extração");
        if (Boolean.TRUE.equals(b.leitosNaoDisponivel())) {
            l.linha(NAO_DISPONIVEL, null, null, null, null, null, b.dataExtracaoLeitos());
        } else {
            for (Bloco4Dados.LinhaLeito x : nz(b.leitos())) {
                String tipo = x.tipo() == TipoLeito.OUTRO ? "Outro" + (vazio(b.leitoOutroDescricao()) ? "" : ": " + b.leitoOutroDescricao()) : Rotulos.de(x.tipo());
                if (Boolean.TRUE.equals(x.naoDisponivel())) {
                    l.linha(tipo, NAO_DISPONIVEL, NAO_DISPONIVEL, NAO_DISPONIVEL, NAO_DISPONIVEL, NAO_DISPONIVEL, b.dataExtracaoLeitos());
                } else if (x.solicitacoes() != null || x.atendidas() != null || x.canceladas() != null || x.tempoMedioHoras() != null || x.aguardando() != null) {
                    l.linha(tipo, x.solicitacoes(), x.atendidas(), x.canceladas(), x.tempoMedioHoras(), x.aguardando(), b.dataExtracaoLeitos());
                }
            }
        }
        t.add(l);

        String centrais = ordenados(b.centrais()).stream()
                .map(c -> c == CentralRegulacao.OUTRA ? "Outra" + (vazio(b.centralOutra()) ? "" : ": " + b.centralOutra()) : Rotulos.de(c))
                .collect(Collectors.joining("; "));
        t.add(Tabela.nova("4.1 Central", "Central(is) que recebe(m) os pedidos").linha(centrais));

        Tabela e = Tabela.nova("4.2 Cirurgias eletivas", "Especialidade / procedimento", "Código SIGTAP", "Total aguardando",
                "Espera média (dias)", "Maior espera (dias)", "Origem do dado", "Data de extração");
        if (Boolean.TRUE.equals(b.eletivasNaoDisponivel())) {
            e.linha(NAO_DISPONIVEL, null, null, null, null, null, b.dataExtracaoEletivas());
        } else {
            for (Bloco4Dados.Eletiva x : nz(b.eletivas())) {
                String origem = x.origem() == OrigemDado.OUTRO ? "Outro" + (vazio(x.origemOutro()) ? "" : ": " + x.origemOutro()) : Rotulos.de(x.origem());
                e.linha(x.especialidade(), x.sigtap(), x.totalAguardando(), x.esperaMediaDias(), x.maiorEsperaDias(), origem, b.dataExtracaoEletivas());
            }
        }
        t.add(e);
    }

    // ---------- Bloco 5 ----------

    private static void bloco5(List<Tabela> t, Bloco5Dados b) {
        Tabela u = Tabela.nova("5.1 Unidades de urgência", "Unidade", "CNES", "Tipo", "Leitos de observação",
                "Pacientes no censo há mais de 24 h", "Destes, há mais de 72 h", "Em ventilação mecânica", "Data de extração");
        if (Boolean.TRUE.equals(b.unidadesNaoDisponivel())) {
            u.linha(NAO_DISPONIVEL, null, null, null, null, null, null, b.dataExtracaoUnidades());
        } else {
            for (Bloco5Dados.Unidade x : nz(b.unidades())) {
                u.linha(x.nome(), x.cnes(), Rotulos.de(x.tipo()), x.leitosObservacao(), x.pacientes24h(), x.pacientes72h(),
                        x.ventilacaoMecanica(), b.dataExtracaoUnidades());
            }
        }
        t.add(u);

        Tabela p = Tabela.nova("5.2 Perfil dos pacientes", "O que precisam", "Nº de pacientes", "Principais diagnósticos (CID-10)");
        if (Boolean.TRUE.equals(b.perfilNaoDisponivel())) {
            p.linha(NAO_DISPONIVEL, null, null);
        } else {
            for (Bloco5Dados.Perfil x : nz(b.perfil())) {
                String nome = x.necessidade() == NecessidadePaciente.OUTRO ? "Outro" + (vazio(b.perfilOutroDescricao()) ? "" : ": " + b.perfilOutroDescricao()) : Rotulos.de(x.necessidade());
                if (Boolean.TRUE.equals(x.naoDisponivel())) p.linha(nome, NAO_DISPONIVEL, NAO_DISPONIVEL);
                else if (x.pacientes() != null || !vazio(x.diagnosticos())) p.linha(nome, x.pacientes(), x.diagnosticos());
            }
        }
        t.add(p);

        String motivos = ordenados(b.motivos()).stream()
                .map(m -> m == MotivoSemVaga.OUTRO ? "Outro" + (vazio(b.motivoOutro()) ? "" : ": " + b.motivoOutro()) : Rotulos.de(m))
                .collect(Collectors.joining("; "));
        Tabela i = Tabela.nova("5.3 e 5.4 Indicadores", "Pacientes que ficaram mais de 24 h aguardando vaga",
                "Tempo médio de permanência (horas)", "Óbitos de quem aguardava vaga ou procedimento",
                "Altas da UPA sem o procedimento indicado", "Registro informatizado do tempo de permanência?", "Por que não conseguem vaga (5.4)");
        if (Boolean.TRUE.equals(b.indicadoresNaoDisponivel())) {
            i.linha(NAO_DISPONIVEL, NAO_DISPONIVEL, NAO_DISPONIVEL, NAO_DISPONIVEL, NAO_DISPONIVEL, motivos);
        } else {
            i.linha(b.pacientesMais24h(), b.tempoMedioPermanenciaHoras(), b.obitosAguardandoVaga(), b.altaSemProcedimento(),
                    Rotulos.de(b.registroInformatizado()), motivos);
        }
        t.add(i);
    }

    // ---------- Bloco 6 ----------

    private static void bloco6(List<Tabela> t, Bloco6Dados b) {
        Tabela s = Tabela.nova("6 Seguimento", "Situação clínica", "Procedimento de seguimento", "Pacientes aguardando",
                "Espera média (dias)", "Onde é feito hoje (município / serviço)");
        if (Boolean.TRUE.equals(b.tabelaNaoDisponivel())) {
            s.linha(NAO_DISPONIVEL, null, null, null, null);
        } else {
            for (Bloco6Dados.Seguimento x : nz(b.situacoes())) {
                boolean outro = x.situacao() == SituacaoClinica.OUTRO;
                String situacao = outro ? "Outro" + (vazio(x.situacaoOutra()) ? "" : ": " + x.situacaoOutra()) : Rotulos.de(x.situacao());
                String procedimento = outro ? x.procedimentoOutro() : Rotulos.procedimentoDaSituacao(x.situacao());
                if (Boolean.TRUE.equals(x.naoDisponivel())) {
                    s.linha(situacao, procedimento, NAO_DISPONIVEL, NAO_DISPONIVEL, NAO_DISPONIVEL);
                } else if (x.pacientesAguardando() != null || x.esperaMediaDias() != null || !vazio(x.ondeFeito())) {
                    s.linha(situacao, procedimento, x.pacientesAguardando(), x.esperaMediaDias(), x.ondeFeito());
                }
            }
        }
        t.add(s);
    }

    // ---------- Bloco 7 ----------

    private static void bloco7(List<Tabela> t, Bloco7Dados b) {
        Tabela r = Tabela.nova("7.1 Repasses estaduais", "Unidade ou serviço", "CNES", "Ato (número)", "Ato (data)",
                "Valor mensal (R$)", "Vigência (até)", "Repasse em dia?", "Observação");
        if (Boolean.TRUE.equals(b.repassesNaoDisponivel())) {
            r.linha(NAO_DISPONIVEL, null, null, null, null, null, null, null);
        } else {
            for (Bloco7Dados.Repasse x : nz(b.repasses())) {
                r.linha(x.unidade(), x.cnes(), x.atoNumero(), x.atoData(), Reais.de(x.valorMensal()), x.vigenciaAte(),
                        Rotulos.de(x.repasseEmDia()), x.observacao());
            }
        }
        t.add(r);

        Tabela l = Tabela.nova("7.2 Leitos privados", "Hospital", "CNES", "Tipo de leito", "Já contratados pelo SUS",
                "Poderiam ser contratados", "Valor pago por diária (R$)", "Observação");
        if (Boolean.TRUE.equals(b.leitosPrivadosNaoDisponivel())) {
            l.linha(NAO_DISPONIVEL, null, null, null, null, null, null);
        } else {
            for (Bloco7Dados.LeitoPrivado x : nz(b.leitosPrivados())) {
                l.linha(x.hospital(), x.cnes(), Rotulos.de(x.tipoLeito()), x.contratadosSus(), x.poderiamSerContratados(),
                        Reais.de(x.valorDiaria()), x.observacao());
            }
        }
        t.add(l);

        Tabela a = Tabela.nova("7.3 Aparelhos de imagem", "Estabelecimento", "CNES", "Aparelho", "Quantidade no CNES",
                "Em uso de fato", "Atende o SUS?", "Correção necessária");
        if (Boolean.TRUE.equals(b.aparelhosNaoDisponivel())) {
            a.linha(NAO_DISPONIVEL, null, null, null, null, null, null);
        } else {
            for (Bloco7Dados.Aparelho x : nz(b.aparelhos())) {
                a.linha(x.estabelecimento(), x.cnes(), Rotulos.de(x.aparelho()), x.quantidadeCnes(), x.emUsoDeFato(),
                        Rotulos.de(x.atendeSus()), x.correcaoNecessaria());
            }
        }
        t.add(a);
    }

    // ---------- Bloco 8 ----------

    private static void bloco8(List<Tabela> t, Bloco8Dados b, List<questionario.api.dto.AnexoResponse> anexos) {
        String documentos = ordenados(b.documentos()).stream().map(Rotulos::de).collect(Collectors.joining("; "));
        t.add(Tabela.nova("8 Observações finais", "Algo que a equipe precisa saber (8.1)", "Documentos que podem ser anexados (8.2)",
                "Assinatura (secretário(a))", "Data da assinatura", "Declaração de veracidade")
                .linha(b.observacoes(), documentos, b.assinaturaNome(), b.assinaturaData(), Boolean.TRUE.equals(b.declaracao()) ? "Sim" : "Não"));
        Tabela a = Tabela.nova("8.2 Arquivos anexados", "Documento", "Arquivo", "Tamanho (KB)", "Enviado em");
        for (var x : nz(anexos)) {
            if (!x.documento().doItem82()) continue;
            a.linha(Rotulos.de(x.documento()), x.nome(), (int) Math.max(1, Math.round(x.tamanho() / 1024.0)), Formato.dataHora(x.criadoEm()));
        }
        t.add(a);
    }

    // ---------- utilidades ----------

    private static boolean vazio(String s) {
        return s == null || s.isBlank();
    }

    private static <T> List<T> nz(List<T> lista) {
        return lista == null ? List.of() : lista;
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    /** Conjuntos de enum na ordem do documento (o Set vem sem ordem). */
    private static <E extends Enum<E>> List<E> ordenados(Collection<E> conjunto) {
        if (conjunto == null) return List.of();
        return conjunto.stream().sorted(Comparator.comparingInt(Enum::ordinal)).toList();
    }

    /** "2026-09" vira "09/2026". */
    private static String mesAno(String anoMes) {
        if (anoMes == null || !anoMes.matches("\\d{4}-\\d{2}")) return anoMes;
        return anoMes.substring(5) + "/" + anoMes.substring(0, 4);
    }
}
