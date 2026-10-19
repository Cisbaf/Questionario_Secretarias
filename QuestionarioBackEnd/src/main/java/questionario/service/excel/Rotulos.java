package questionario.service.excel;

import java.util.HashMap;
import java.util.Map;

/**
 * Textos do documento para as opções fixas (os mesmos do front, em lib/rotulos.ts), usados no Excel. Gerado a partir
 * de lib/rotulos.ts: se o texto de uma opção mudar lá, mude aqui também.
 */
final class Rotulos {

    private static final Map<String, String> TEXTOS = new HashMap<>();
    private static final Map<String, String> PROCEDIMENTOS = new HashMap<>();

    static {
        TEXTOS.put("ResolveSozinho.SIM", "Sim");
        TEXTOS.put("ResolveSozinho.EM_PARTE", "Em parte");
        TEXTOS.put("ResolveSozinho.NAO", "Não");
        TEXTOS.put("LinhaCuidado.CARDIOVASCULAR_AVC", "Cardiovascular e AVC (hemodinâmica, cirurgia cardíaca, unidade de AVC)");
        TEXTOS.put("LinhaCuidado.TRAUMA_ORTOPEDIA", "Trauma e ortopedia");
        TEXTOS.put("LinhaCuidado.ONCOLOGIA", "Oncologia (diagnóstico, cirurgia, quimioterapia, radioterapia)");
        TEXTOS.put("LinhaCuidado.DIAGNOSTICO_IMAGEM", "Diagnóstico por imagem (tomografia, ressonância, mamografia)");
        TEXTOS.put("LinhaCuidado.QUEIMADOS", "Queimados");
        TEXTOS.put("LinhaCuidado.ATENCAO_PRIMARIA_ESF", "Atenção primária e Estratégia Saúde da Família");
        TEXTOS.put("LinhaCuidado.GESTACAO_PARTO_NASCIMENTO", "Gestação, parto e nascimento (incluindo UTI neonatal)");
        TEXTOS.put("LinhaCuidado.SAUDE_CRIANCA_PEDIATRIA", "Saúde da criança e leitos pediátricos");
        TEXTOS.put("LinhaCuidado.UTI_ADULTO", "UTI adulto");
        TEXTOS.put("LinhaCuidado.CLINICA_MEDICA_RETAGUARDA", "Clínica médica e leitos de retaguarda");
        TEXTOS.put("LinhaCuidado.CIRURGIAS_ELETIVAS_MEDIA", "Cirurgias eletivas de média complexidade");
        TEXTOS.put("LinhaCuidado.SAUDE_MENTAL", "Saúde mental (CAPS e leitos em hospital geral)");
        TEXTOS.put("LinhaCuidado.NEFROLOGIA_TRS", "Nefrologia e terapia renal substitutiva");
        TEXTOS.put("LinhaCuidado.OFTALMOLOGIA", "Oftalmologia");
        TEXTOS.put("LinhaCuidado.DOENCAS_CRONICAS", "Doenças crônicas (hipertensão, diabetes)");
        TEXTOS.put("LinhaCuidado.REABILITACAO_PCD", "Reabilitação e pessoa com deficiência");
        TEXTOS.put("LinhaCuidado.PALIATIVOS_ATENCAO_DOMICILIAR", "Cuidados paliativos e atenção domiciliar");
        TEXTOS.put("LinhaCuidado.OUTRA", "Outra");
        TEXTOS.put("Impedimento.FINANCIAMENTO_TETO_MAC", "Financiamento / teto MAC insuficiente");
        TEXTOS.put("Impedimento.COFINANCIAMENTO_ESTADUAL", "Cofinanciamento estadual atrasado ou sem garantia de continuidade");
        TEXTOS.put("Impedimento.FALTA_PROFISSIONAIS", "Falta de profissionais (especialistas, intensivistas, anestesistas)");
        TEXTOS.put("Impedimento.ESTRUTURA_EQUIPAMENTOS", "Estrutura física e equipamentos");
        TEXTOS.put("Impedimento.CONTRATAR_CREDENCIAR_PRESTADORES", "Dificuldade de contratar ou credenciar prestadores");
        TEXTOS.put("Impedimento.REGULACAO_FRAGMENTADA", "Regulação fragmentada entre sistemas");
        TEXTOS.put("Impedimento.TRANSPORTE_DISTANCIA", "Transporte sanitário e distância");
        TEXTOS.put("Impedimento.DEPENDENCIA_SERVICOS_ESTADUAIS", "Dependência de serviços estaduais ou da capital");
        TEXTOS.put("Impedimento.OUTRO", "Outro");
        TEXTOS.put("TemaUrgencia.GOVERNANCA_PACTUACAO", "Governança e pactuação entre os municípios");
        TEXTOS.put("TemaUrgencia.ORGANIZACAO_REDE", "Organização da rede de atenção à saúde (atenção primária, especializada e hospitalar)");
        TEXTOS.put("TemaUrgencia.FINANCIAMENTO", "Financiamento da saúde");
        TEXTOS.put("TemaUrgencia.TRANSPORTE_REGULACAO", "Transporte sanitário e regulação do acesso");
        TEXTOS.put("TemaUrgencia.PROFISSIONAIS", "Atração, fixação e qualificação de profissionais");
        TEXTOS.put("TemaUrgencia.MONITORAMENTO_AVALIACAO", "Monitoramento e avaliação");
        TEXTOS.put("SistemaRegulacao.SER", "SER – Sistema Estadual de Regulação");
        TEXTOS.put("SistemaRegulacao.SISREG", "SISREG");
        TEXTOS.put("SistemaRegulacao.MARQUE_FACIL", "Marque Fácil (CISBAF)");
        TEXTOS.put("SistemaRegulacao.PROPRIO", "Sistema próprio do município");
        TEXTOS.put("SistemaRegulacao.PLANILHA_MANUAL", "Planilha / controle manual");
        TEXTOS.put("SistemaRegulacao.OUTRO", "Outro");
        TEXTOS.put("TipoFila.UNICA", "Única");
        TEXTOS.put("TipoFila.POR_UNIDADE", "Por unidade");
        TEXTOS.put("TipoFila.MISTA", "Mista");
        TEXTOS.put("SimNaoParcial.SIM", "Sim");
        TEXTOS.put("SimNaoParcial.NAO", "Não");
        TEXTOS.put("SimNaoParcial.PARCIAL", "Parcial");
        TEXTOS.put("TipoLeito.CLINICO_ADULTO", "Clínico adulto");
        TEXTOS.put("TipoLeito.CIRURGICO_ADULTO", "Cirúrgico adulto");
        TEXTOS.put("TipoLeito.ORTOPEDIA_TRAUMA", "Ortopedia / trauma");
        TEXTOS.put("TipoLeito.UTI_ADULTO", "UTI adulto");
        TEXTOS.put("TipoLeito.UTI_PEDIATRICA", "UTI pediátrica");
        TEXTOS.put("TipoLeito.UTI_NEONATAL", "UTI neonatal");
        TEXTOS.put("TipoLeito.INTERMEDIARIA_SEMI", "Unidade intermediária / semi-intensiva");
        TEXTOS.put("TipoLeito.PEDIATRICO", "Pediátrico");
        TEXTOS.put("TipoLeito.OBSTETRICO", "Obstétrico");
        TEXTOS.put("TipoLeito.PSIQUIATRICO", "Psiquiátrico / saúde mental");
        TEXTOS.put("TipoLeito.CARDIOLOGIA_HEMODINAMICA", "Cardiologia / hemodinâmica");
        TEXTOS.put("TipoLeito.NEUROLOGIA_AVC", "Neurologia / AVC");
        TEXTOS.put("TipoLeito.ONCOLOGIA", "Oncologia");
        TEXTOS.put("TipoLeito.OUTRO", "Outro");
        TEXTOS.put("CentralRegulacao.MUNICIPAL", "Municipal");
        TEXTOS.put("CentralRegulacao.ESTADUAL", "Estadual");
        TEXTOS.put("CentralRegulacao.CRUR_SAMU", "CRUR-BF / SAMU");
        TEXTOS.put("CentralRegulacao.OUTRA", "Outra");
        TEXTOS.put("OrigemDado.OPERA_FACIL", "Opera Fácil");
        TEXTOS.put("OrigemDado.SER", "SER");
        TEXTOS.put("OrigemDado.SISREG", "SISREG");
        TEXTOS.put("OrigemDado.CONTROLE_PROPRIO", "Controle próprio");
        TEXTOS.put("OrigemDado.OUTRO", "Outro");
        TEXTOS.put("TipoUnidadeUrgencia.UPA", "UPA");
        TEXTOS.put("TipoUnidadeUrgencia.PS", "Pronto-socorro (PS)");
        TEXTOS.put("TipoUnidadeUrgencia.PA", "Pronto atendimento (PA)");
        TEXTOS.put("TipoUnidadeUrgencia.HOSPITAL", "Hospital");
        TEXTOS.put("NecessidadePaciente.LEITO_CLINICO", "Leito clínico");
        TEXTOS.put("NecessidadePaciente.LEITO_CIRURGICO", "Leito cirúrgico (cirurgia geral)");
        TEXTOS.put("NecessidadePaciente.CIRURGIA_ORTOPEDICA_TRAUMA", "Cirurgia ortopédica / trauma");
        TEXTOS.put("NecessidadePaciente.UTI_ADULTO", "UTI adulto");
        TEXTOS.put("NecessidadePaciente.UTI_LEITO_PEDIATRICO", "UTI ou leito pediátrico");
        TEXTOS.put("NecessidadePaciente.LEITO_PSIQUIATRICO", "Leito psiquiátrico");
        TEXTOS.put("NecessidadePaciente.HEMODINAMICA_CIRURGIA_CARDIACA", "Hemodinâmica ou cirurgia cardíaca");
        TEXTOS.put("NecessidadePaciente.NEURO_AVC", "Neurologia / neurocirurgia / AVC");
        TEXTOS.put("NecessidadePaciente.HEMODIALISE", "Hemodiálise");
        TEXTOS.put("NecessidadePaciente.EXAME_IMAGEM_DIAGNOSTICO", "Exame de imagem ou outro procedimento diagnóstico");
        TEXTOS.put("NecessidadePaciente.AVALIACAO_ESPECIALISTA", "Avaliação de especialista");
        TEXTOS.put("NecessidadePaciente.CUIDADOS_PROLONGADOS_PALIATIVOS", "Leito de cuidados prolongados / paliativos");
        TEXTOS.put("NecessidadePaciente.OUTRO", "Outro");
        TEXTOS.put("MotivoSemVaga.FALTA_LEITO_NA_REGIAO", "Falta de leito do tipo necessário na região");
        TEXTOS.put("MotivoSemVaga.LEITO_FORA_DA_REGULACAO", "Leito existe, mas não está sob regulação pública");
        TEXTOS.put("MotivoSemVaga.DEMORA_DA_CENTRAL", "Demora da central em responder");
        TEXTOS.put("MotivoSemVaga.FALTA_DE_TRANSPORTE", "Falta de transporte para a transferência");
        TEXTOS.put("MotivoSemVaga.FALTA_ESPECIALISTA_REFERENCIA", "Falta de especialista ou de procedimento na referência");
        TEXTOS.put("MotivoSemVaga.RECUSA_HOSPITAL_REFERENCIA", "Recusa do hospital de referência");
        TEXTOS.put("MotivoSemVaga.SEM_RETAGUARDA_ALTA", "Paciente sem retaguarda para alta (social, cuidados prolongados)");
        TEXTOS.put("MotivoSemVaga.OUTRO", "Outro");
        TEXTOS.put("SituacaoClinica.FRATURA_CIRURGICA", "Fratura com indicação cirúrgica");
        TEXTOS.put("SituacaoClinica.INFARTO_SCA", "Infarto / síndrome coronariana");
        TEXTOS.put("SituacaoClinica.AVC", "AVC");
        TEXTOS.put("SituacaoClinica.SUSPEITA_CANCER", "Suspeita de câncer");
        TEXTOS.put("SituacaoClinica.CANCER_CONFIRMADO", "Câncer confirmado");
        TEXTOS.put("SituacaoClinica.DOENCA_RENAL_CRONICA", "Doença renal crônica");
        TEXTOS.put("SituacaoClinica.GESTANTE_ALTO_RISCO", "Gestante de alto risco");
        TEXTOS.put("SituacaoClinica.PALIATIVO_AVANCADO", "Câncer avançado ou doença sem possibilidade de cura");
        TEXTOS.put("SituacaoClinica.OUTRO", "Outro");
        TEXTOS.put("TipoLeitoPrivado.CLINICO", "Clínico");
        TEXTOS.put("TipoLeitoPrivado.CIRURGICO", "Cirúrgico");
        TEXTOS.put("TipoLeitoPrivado.UTI_ADULTO", "UTI adulto");
        TEXTOS.put("TipoLeitoPrivado.PEDIATRICO", "Pediátrico");
        TEXTOS.put("TipoLeitoPrivado.OBSTETRICO", "Obstétrico");
        TEXTOS.put("TipoLeitoPrivado.OUTRO", "Outro");
        TEXTOS.put("AparelhoImagem.TOMOGRAFO", "Tomógrafo");
        TEXTOS.put("AparelhoImagem.RESSONANCIA", "Ressonância");
        TEXTOS.put("AparelhoImagem.OUTRO", "Outro");
        TEXTOS.put("DocumentoAnexavel.PLANO_MUNICIPAL_PAS", "Plano Municipal de Saúde e Programação Anual de Saúde vigentes");
        TEXTOS.put("DocumentoAnexavel.RELATORIO_FILA_REGULACAO", "Relatório de fila extraído do sistema de regulação");
        TEXTOS.put("DocumentoAnexavel.RELATORIO_PERMANENCIA_UPA", "Relatório de permanência das UPAs");
        TEXTOS.put("DocumentoAnexavel.ESTUDOS_DEMANDA_REPRIMIDA", "Estudos ou levantamentos próprios sobre demanda reprimida");
        TEXTOS.put("DocumentoAnexavel.RESOLUCOES_COFINANCIAMENTO", "Resoluções, deliberações ou termos de cofinanciamento estadual vigentes");
        TEXTOS.put("DocumentoAnexavel.CONTRATOS_HOSPITAIS_PRIVADOS", "Contratos ou convênios com hospitais privados e filantrópicos");
        PROCEDIMENTOS.put("FRATURA_CIRURGICA", "Cirurgia ortopédica");
        PROCEDIMENTOS.put("INFARTO_SCA", "Cateterismo, angioplastia");
        PROCEDIMENTOS.put("AVC", "Reabilitação, exames de imagem");
        PROCEDIMENTOS.put("SUSPEITA_CANCER", "Biópsia, consulta oncológica");
        PROCEDIMENTOS.put("CANCER_CONFIRMADO", "Quimioterapia / radioterapia");
        PROCEDIMENTOS.put("DOENCA_RENAL_CRONICA", "Hemodiálise / fístula");
        PROCEDIMENTOS.put("GESTANTE_ALTO_RISCO", "Pré-natal de alto risco");
        PROCEDIMENTOS.put("PALIATIVO_AVANCADO", "Leito de cuidados paliativos ou atenção domiciliar");
        PROCEDIMENTOS.put("OUTRO", "");
    }

    private Rotulos() {
    }

    /** Texto da opção; sem opção (nulo) volta vazio. */
    static String de(Enum<?> opcao) {
        if (opcao == null) return "";
        return TEXTOS.getOrDefault(opcao.getDeclaringClass().getSimpleName() + "." + opcao.name(), opcao.name());
    }

    /** Classificação de risco usa "Em parte" no lugar de "Parcial" (como no documento). */
    static String simNaoEmParte(Enum<?> opcao) {
        return opcao != null && "PARCIAL".equals(opcao.name()) ? "Em parte" : de(opcao);
    }

    static String procedimentoDaSituacao(Enum<?> situacao) {
        return situacao == null ? "" : PROCEDIMENTOS.getOrDefault(situacao.name(), "");
    }
}
