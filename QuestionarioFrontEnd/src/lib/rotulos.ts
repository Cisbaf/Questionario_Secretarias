/**
 * Textos do documento "Questionário às Secretarias Municipais de Saúde" para as opções fixas dos blocos. O backend
 * só conhece os códigos (enums); o texto que a pessoa lê mora aqui, na ordem do documento.
 */

export const RESOLVE_SOZINHO = [
  { valor: "SIM", rotulo: "Sim" },
  { valor: "EM_PARTE", rotulo: "Em parte" },
  { valor: "NAO", rotulo: "Não" },
] as const;
export type ResolveSozinho = (typeof RESOLVE_SOZINHO)[number]["valor"];

/** Bloco 2, item 2.2. {@code OUTRA} é a linha aberta ("Outra: ____"). */
export const LINHAS_CUIDADO = [
  { valor: "CARDIOVASCULAR_AVC", rotulo: "Cardiovascular e AVC", detalhe: "hemodinâmica, cirurgia cardíaca, unidade de AVC" },
  { valor: "TRAUMA_ORTOPEDIA", rotulo: "Trauma e ortopedia" },
  { valor: "ONCOLOGIA", rotulo: "Oncologia", detalhe: "diagnóstico, cirurgia, quimioterapia, radioterapia" },
  { valor: "DIAGNOSTICO_IMAGEM", rotulo: "Diagnóstico por imagem", detalhe: "tomografia, ressonância, mamografia" },
  { valor: "QUEIMADOS", rotulo: "Queimados" },
  { valor: "ATENCAO_PRIMARIA_ESF", rotulo: "Atenção primária e Estratégia Saúde da Família" },
  { valor: "GESTACAO_PARTO_NASCIMENTO", rotulo: "Gestação, parto e nascimento", detalhe: "incluindo UTI neonatal" },
  { valor: "SAUDE_CRIANCA_PEDIATRIA", rotulo: "Saúde da criança e leitos pediátricos" },
  { valor: "UTI_ADULTO", rotulo: "UTI adulto" },
  { valor: "CLINICA_MEDICA_RETAGUARDA", rotulo: "Clínica médica e leitos de retaguarda" },
  { valor: "CIRURGIAS_ELETIVAS_MEDIA", rotulo: "Cirurgias eletivas de média complexidade" },
  { valor: "SAUDE_MENTAL", rotulo: "Saúde mental", detalhe: "CAPS e leitos em hospital geral" },
  { valor: "NEFROLOGIA_TRS", rotulo: "Nefrologia e terapia renal substitutiva" },
  { valor: "OFTALMOLOGIA", rotulo: "Oftalmologia" },
  { valor: "DOENCAS_CRONICAS", rotulo: "Doenças crônicas", detalhe: "hipertensão, diabetes" },
  { valor: "REABILITACAO_PCD", rotulo: "Reabilitação e pessoa com deficiência" },
  { valor: "PALIATIVOS_ATENCAO_DOMICILIAR", rotulo: "Cuidados paliativos e atenção domiciliar" },
  { valor: "OUTRA", rotulo: "Outra" },
] as const;
export type LinhaCuidado = (typeof LINHAS_CUIDADO)[number]["valor"];

/** Bloco 2, item 2.5. */
export const IMPEDIMENTOS = [
  { valor: "FINANCIAMENTO_TETO_MAC", rotulo: "Financiamento / teto MAC insuficiente" },
  { valor: "COFINANCIAMENTO_ESTADUAL", rotulo: "Cofinanciamento estadual atrasado ou sem garantia de continuidade" },
  { valor: "FALTA_PROFISSIONAIS", rotulo: "Falta de profissionais (especialistas, intensivistas, anestesistas)" },
  { valor: "ESTRUTURA_EQUIPAMENTOS", rotulo: "Estrutura física e equipamentos" },
  { valor: "CONTRATAR_CREDENCIAR_PRESTADORES", rotulo: "Dificuldade de contratar ou credenciar prestadores" },
  { valor: "REGULACAO_FRAGMENTADA", rotulo: "Regulação fragmentada entre sistemas" },
  { valor: "TRANSPORTE_DISTANCIA", rotulo: "Transporte sanitário e distância" },
  { valor: "DEPENDENCIA_SERVICOS_ESTADUAIS", rotulo: "Dependência de serviços estaduais ou da capital" },
  { valor: "OUTRO", rotulo: "Outro" },
] as const;
export type Impedimento = (typeof IMPEDIMENTOS)[number]["valor"];

/** Bloco 2, item 2.6, na ordem em que aparecem no documento. */
export const TEMAS_URGENCIA = [
  { valor: "GOVERNANCA_PACTUACAO", rotulo: "Governança e pactuação entre os municípios" },
  { valor: "ORGANIZACAO_REDE", rotulo: "Organização da rede de atenção à saúde (atenção primária, especializada e hospitalar)" },
  { valor: "FINANCIAMENTO", rotulo: "Financiamento da saúde" },
  { valor: "TRANSPORTE_REGULACAO", rotulo: "Transporte sanitário e regulação do acesso" },
  { valor: "PROFISSIONAIS", rotulo: "Atração, fixação e qualificação de profissionais" },
  { valor: "MONITORAMENTO_AVALIACAO", rotulo: "Monitoramento e avaliação" },
] as const;
export type TemaUrgencia = (typeof TEMAS_URGENCIA)[number]["valor"];

export const rotuloDoTema = (tema: TemaUrgencia) => TEMAS_URGENCIA.find((t) => t.valor === tema)?.rotulo ?? tema;

// ---------- Bloco 3 ----------

export const SISTEMAS_REGULACAO = [
  { valor: "SER", rotulo: "SER – Sistema Estadual de Regulação" },
  { valor: "SISREG", rotulo: "SISREG" },
  { valor: "MARQUE_FACIL", rotulo: "Marque Fácil (CISBAF)" },
  { valor: "PROPRIO", rotulo: "Sistema próprio do município" },
  { valor: "PLANILHA_MANUAL", rotulo: "Planilha / controle manual" },
  { valor: "OUTRO", rotulo: "Outro" },
] as const;
export type SistemaRegulacao = (typeof SISTEMAS_REGULACAO)[number]["valor"];

export const TIPOS_FILA = [
  { valor: "UNICA", rotulo: "Única" },
  { valor: "POR_UNIDADE", rotulo: "Por unidade" },
  { valor: "MISTA", rotulo: "Mista" },
] as const;
export type TipoFila = (typeof TIPOS_FILA)[number]["valor"];

/** Sim / Não / Parcial. O documento usa "Em parte" na classificação de risco e "Parcial" no registro informatizado. */
export type SimNaoParcial = "SIM" | "NAO" | "PARCIAL";
export const SIM_NAO_EM_PARTE = [
  { valor: "SIM", rotulo: "Sim" }, { valor: "NAO", rotulo: "Não" }, { valor: "PARCIAL", rotulo: "Em parte" },
] as const;
export const SIM_NAO_PARCIAL = [
  { valor: "SIM", rotulo: "Sim" }, { valor: "NAO", rotulo: "Não" }, { valor: "PARCIAL", rotulo: "Parcial" },
] as const;

// ---------- Bloco 4 ----------

export const TIPOS_LEITO = [
  { valor: "CLINICO_ADULTO", rotulo: "Clínico adulto" },
  { valor: "CIRURGICO_ADULTO", rotulo: "Cirúrgico adulto" },
  { valor: "ORTOPEDIA_TRAUMA", rotulo: "Ortopedia / trauma" },
  { valor: "UTI_ADULTO", rotulo: "UTI adulto" },
  { valor: "UTI_PEDIATRICA", rotulo: "UTI pediátrica" },
  { valor: "UTI_NEONATAL", rotulo: "UTI neonatal" },
  { valor: "INTERMEDIARIA_SEMI", rotulo: "Unidade intermediária / semi-intensiva" },
  { valor: "PEDIATRICO", rotulo: "Pediátrico" },
  { valor: "OBSTETRICO", rotulo: "Obstétrico" },
  { valor: "PSIQUIATRICO", rotulo: "Psiquiátrico / saúde mental" },
  { valor: "CARDIOLOGIA_HEMODINAMICA", rotulo: "Cardiologia / hemodinâmica" },
  { valor: "NEUROLOGIA_AVC", rotulo: "Neurologia / AVC" },
  { valor: "ONCOLOGIA", rotulo: "Oncologia" },
  { valor: "OUTRO", rotulo: "Outro" },
] as const;
export type TipoLeito = (typeof TIPOS_LEITO)[number]["valor"];

export const CENTRAIS_REGULACAO = [
  { valor: "MUNICIPAL", rotulo: "Municipal" },
  { valor: "ESTADUAL", rotulo: "Estadual" },
  { valor: "CRUR_SAMU", rotulo: "CRUR-BF / SAMU" },
  { valor: "OUTRA", rotulo: "Outra" },
] as const;
export type CentralRegulacao = (typeof CENTRAIS_REGULACAO)[number]["valor"];

export const ORIGENS_DADO = [
  { valor: "OPERA_FACIL", rotulo: "Opera Fácil" },
  { valor: "SER", rotulo: "SER" },
  { valor: "SISREG", rotulo: "SISREG" },
  { valor: "CONTROLE_PROPRIO", rotulo: "Controle próprio" },
  { valor: "OUTRO", rotulo: "Outro" },
] as const;
export type OrigemDado = (typeof ORIGENS_DADO)[number]["valor"];

// ---------- Bloco 5 ----------

export const TIPOS_UNIDADE_URGENCIA = [
  { valor: "UPA", rotulo: "UPA" },
  { valor: "PS", rotulo: "Pronto-socorro (PS)" },
  { valor: "PA", rotulo: "Pronto atendimento (PA)" },
  { valor: "HOSPITAL", rotulo: "Hospital" },
] as const;
export type TipoUnidadeUrgencia = (typeof TIPOS_UNIDADE_URGENCIA)[number]["valor"];

export const NECESSIDADES_PACIENTE = [
  { valor: "LEITO_CLINICO", rotulo: "Leito clínico" },
  { valor: "LEITO_CIRURGICO", rotulo: "Leito cirúrgico (cirurgia geral)" },
  { valor: "CIRURGIA_ORTOPEDICA_TRAUMA", rotulo: "Cirurgia ortopédica / trauma" },
  { valor: "UTI_ADULTO", rotulo: "UTI adulto" },
  { valor: "UTI_LEITO_PEDIATRICO", rotulo: "UTI ou leito pediátrico" },
  { valor: "LEITO_PSIQUIATRICO", rotulo: "Leito psiquiátrico" },
  { valor: "HEMODINAMICA_CIRURGIA_CARDIACA", rotulo: "Hemodinâmica ou cirurgia cardíaca" },
  { valor: "NEURO_AVC", rotulo: "Neurologia / neurocirurgia / AVC" },
  { valor: "HEMODIALISE", rotulo: "Hemodiálise" },
  { valor: "EXAME_IMAGEM_DIAGNOSTICO", rotulo: "Exame de imagem ou outro procedimento diagnóstico" },
  { valor: "AVALIACAO_ESPECIALISTA", rotulo: "Avaliação de especialista" },
  { valor: "CUIDADOS_PROLONGADOS_PALIATIVOS", rotulo: "Leito de cuidados prolongados / paliativos" },
  { valor: "OUTRO", rotulo: "Outro" },
] as const;
export type NecessidadePaciente = (typeof NECESSIDADES_PACIENTE)[number]["valor"];

export const MOTIVOS_SEM_VAGA = [
  { valor: "FALTA_LEITO_NA_REGIAO", rotulo: "Falta de leito do tipo necessário na região" },
  { valor: "LEITO_FORA_DA_REGULACAO", rotulo: "Leito existe, mas não está sob regulação pública" },
  { valor: "DEMORA_DA_CENTRAL", rotulo: "Demora da central em responder" },
  { valor: "FALTA_DE_TRANSPORTE", rotulo: "Falta de transporte para a transferência" },
  { valor: "FALTA_ESPECIALISTA_REFERENCIA", rotulo: "Falta de especialista ou de procedimento na referência" },
  { valor: "RECUSA_HOSPITAL_REFERENCIA", rotulo: "Recusa do hospital de referência" },
  { valor: "SEM_RETAGUARDA_ALTA", rotulo: "Paciente sem retaguarda para alta (social, cuidados prolongados)" },
  { valor: "OUTRO", rotulo: "Outro" },
] as const;
export type MotivoSemVaga = (typeof MOTIVOS_SEM_VAGA)[number]["valor"];

// ---------- Bloco 6 ----------

/** Situação clínica e procedimento de seguimento, exatamente como no documento. {@code OUTRO} é editável. */
export const SITUACOES_CLINICAS = [
  { valor: "FRATURA_CIRURGICA", situacao: "Fratura com indicação cirúrgica", procedimento: "Cirurgia ortopédica" },
  { valor: "INFARTO_SCA", situacao: "Infarto / síndrome coronariana", procedimento: "Cateterismo, angioplastia" },
  { valor: "AVC", situacao: "AVC", procedimento: "Reabilitação, exames de imagem" },
  { valor: "SUSPEITA_CANCER", situacao: "Suspeita de câncer", procedimento: "Biópsia, consulta oncológica" },
  { valor: "CANCER_CONFIRMADO", situacao: "Câncer confirmado", procedimento: "Quimioterapia / radioterapia" },
  { valor: "DOENCA_RENAL_CRONICA", situacao: "Doença renal crônica", procedimento: "Hemodiálise / fístula" },
  { valor: "GESTANTE_ALTO_RISCO", situacao: "Gestante de alto risco", procedimento: "Pré-natal de alto risco" },
  { valor: "PALIATIVO_AVANCADO", situacao: "Câncer avançado ou doença sem possibilidade de cura", procedimento: "Leito de cuidados paliativos ou atenção domiciliar" },
  { valor: "OUTRO", situacao: "Outro", procedimento: "" },
] as const;
export type SituacaoClinica = (typeof SITUACOES_CLINICAS)[number]["valor"];

// ---------- Bloco 7 ----------

export const TIPOS_LEITO_PRIVADO = [
  { valor: "CLINICO", rotulo: "Clínico" },
  { valor: "CIRURGICO", rotulo: "Cirúrgico" },
  { valor: "UTI_ADULTO", rotulo: "UTI adulto" },
  { valor: "PEDIATRICO", rotulo: "Pediátrico" },
  { valor: "OBSTETRICO", rotulo: "Obstétrico" },
  { valor: "OUTRO", rotulo: "Outro" },
] as const;
export type TipoLeitoPrivado = (typeof TIPOS_LEITO_PRIVADO)[number]["valor"];

export const APARELHOS_IMAGEM = [
  { valor: "TOMOGRAFO", rotulo: "Tomógrafo" },
  { valor: "RESSONANCIA", rotulo: "Ressonância" },
  { valor: "OUTRO", rotulo: "Outro" },
] as const;
export type AparelhoImagem = (typeof APARELHOS_IMAGEM)[number]["valor"];

// ---------- Bloco 8 ----------

export const DOCUMENTOS_ANEXAVEIS = [
  { valor: "PLANO_MUNICIPAL_PAS", rotulo: "Plano Municipal de Saúde e Programação Anual de Saúde vigentes" },
  { valor: "RELATORIO_FILA_REGULACAO", rotulo: "Relatório de fila extraído do sistema de regulação" },
  { valor: "RELATORIO_PERMANENCIA_UPA", rotulo: "Relatório de permanência das UPAs" },
  { valor: "ESTUDOS_DEMANDA_REPRIMIDA", rotulo: "Estudos ou levantamentos próprios sobre demanda reprimida" },
  { valor: "RESOLUCOES_COFINANCIAMENTO", rotulo: "Resoluções, deliberações ou termos de cofinanciamento estadual vigentes" },
  { valor: "CONTRATOS_HOSPITAIS_PRIVADOS", rotulo: "Contratos ou convênios com hospitais privados e filantrópicos" },
] as const;
export type DocumentoAnexavel = (typeof DOCUMENTOS_ANEXAVEIS)[number]["valor"];

/** Rótulo de um valor de enum em uma das listas acima; devolve o próprio código se não achar. */
export function rotuloDe(lista: readonly { valor: string; rotulo: string }[], valor: string | null | undefined): string {
  if (!valor) return "";
  return lista.find((o) => o.valor === valor)?.rotulo ?? valor;
}
