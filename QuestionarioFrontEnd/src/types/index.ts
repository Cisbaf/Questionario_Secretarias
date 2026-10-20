import type {
  AparelhoImagem, CentralRegulacao, DocumentoAnexavel, Impedimento, LinhaCuidado, MotivoSemVaga, NecessidadePaciente,
  OrigemDado, ResolveSozinho, SimNaoParcial, SistemaRegulacao, SituacaoClinica, TemaUrgencia, TipoFila, TipoLeito,
  TipoLeitoPrivado, TipoUnidadeUrgencia,
} from "@/lib/rotulos";

/** Capa do questionário (GET /levantamentos/ativo). Datas em AAAA-MM-DD; nulo = "a definir". */
export type Levantamento = {
  titulo: string;
  periodoReferenciaInicio: string | null;
  periodoReferenciaFim: string | null;
  dataCensoUpa: string | null;
  prazoDevolucao: string | null;
  enviarPara: string | null;
};

export type StatusQuestionario = "RASCUNHO" | "ENVIADO";

/** Situação de um bloco no servidor. {@code version === null}: ainda não foi salvo. */
export type BlocoSituacao = {
  numero: number;
  version: number | null;
  atualizadoEm: string | null;
};

/** Questionário aberto pelo link público (publicId). O id numérico interno nunca chega ao front. */
export type Questionario = {
  publicId: string;
  status: StatusQuestionario;
  criadoEm: string;
  enviadoEm: string | null;
  blocos: BlocoSituacao[];
};

/** Resposta do servidor para qualquer bloco: os campos do bloco mais o controle de versão. */
export type RespostaDoBloco = {
  version: number | null;
  atualizadoEm: string | null;
};

/** Bloco 1 como o servidor devolve. Campo vazio chega como null. */
export type Bloco1Servidor = RespostaDoBloco & {
  municipio: string | null;
  secretario: string | null;
  responsavel: string | null;
  cargoSetor: string | null;
  email: string | null;
  telefone: string | null;
  dataPreenchimento: string | null;
};

/** Bloco 2 como o servidor devolve: 5 linhas de prioridade sempre; mapas com todas as chaves (null = sem resposta). */
export type Bloco2Servidor = RespostaDoBloco & {
  prioridades: { titulo: string | null; problema: string | null; resolveSozinho: ResolveSozinho | null }[];
  grausLinhaCuidado: Partial<Record<LinhaCuidado, number | null>>;
  linhaOutraDescricao: string | null;
  gargalos: string | null;
  servicosParaVizinhos: string | null;
  impedimentos: Impedimento[];
  impedimentoOutro: string | null;
  ordemTemas: Partial<Record<TemaUrgencia, number | null>>;
};

// ---------- Blocos 3 a 8 como o servidor devolve (campo vazio = null; números são números JSON) ----------

export type Bloco3Servidor = RespostaDoBloco & {
  sistemas: SistemaRegulacao[];
  sistemaProprioNome: string | null;
  sistemaOutroNome: string | null;
  tipoFila: TipoFila | null;
  classificacaoRisco: SimNaoParcial | null;
  ultimaRevisao: string | null;
  dataExtracao: string | null;
  filasNaoDisponivel: boolean;
  filas: {
    especialidade: string | null; sigtap: string | null; totalAguardando: number | null; prioridadeAlta: number | null;
    esperaMediaDias: number | null; maiorEsperaDias: number | null; aguardandoMais180: number | null;
  }[];
};

export type Bloco4Servidor = RespostaDoBloco & {
  leitos: {
    tipo: TipoLeito; naoDisponivel: boolean; solicitacoes: number | null; atendidas: number | null;
    canceladas: number | null; tempoMedioHoras: number | null; aguardando: number | null;
  }[];
  leitoOutroDescricao: string | null;
  leitosNaoDisponivel: boolean;
  dataExtracaoLeitos: string | null;
  centrais: CentralRegulacao[];
  centralOutra: string | null;
  eletivas: {
    especialidade: string | null; sigtap: string | null; totalAguardando: number | null; esperaMediaDias: number | null;
    maiorEsperaDias: number | null; origem: OrigemDado | null; origemOutro: string | null;
  }[];
  eletivasNaoDisponivel: boolean;
  dataExtracaoEletivas: string | null;
};

export type Bloco5Servidor = RespostaDoBloco & {
  unidades: {
    nome: string | null; cnes: string | null; tipo: TipoUnidadeUrgencia | null; leitosObservacao: number | null;
    pacientes24h: number | null; pacientes72h: number | null; ventilacaoMecanica: number | null;
  }[];
  unidadesNaoDisponivel: boolean;
  dataExtracaoUnidades: string | null;
  perfil: { necessidade: NecessidadePaciente; naoDisponivel: boolean; pacientes: number | null; diagnosticos: string | null }[];
  perfilOutroDescricao: string | null;
  perfilNaoDisponivel: boolean;
  pacientesMais24h: number | null;
  tempoMedioPermanenciaHoras: number | null;
  obitosAguardandoVaga: number | null;
  altaSemProcedimento: number | null;
  registroInformatizado: SimNaoParcial | null;
  indicadoresNaoDisponivel: boolean;
  motivos: MotivoSemVaga[];
  motivoOutro: string | null;
};

export type Bloco6Servidor = RespostaDoBloco & {
  situacoes: {
    situacao: SituacaoClinica; situacaoOutra: string | null; procedimentoOutro: string | null; naoDisponivel: boolean;
    pacientesAguardando: number | null; esperaMediaDias: number | null; ondeFeito: string | null;
  }[];
  tabelaNaoDisponivel: boolean;
};

export type Bloco7Servidor = RespostaDoBloco & {
  repasses: {
    unidade: string | null; cnes: string | null; atoNumero: string | null; atoData: string | null;
    valorMensal: number | null; vigenciaAte: string | null; repasseEmDia: SimNaoParcial | null; observacao: string | null;
  }[];
  repassesNaoDisponivel: boolean;
  leitosPrivados: {
    hospital: string | null; cnes: string | null; tipoLeito: TipoLeitoPrivado | null; contratadosSus: number | null;
    poderiamSerContratados: number | null; valorDiaria: number | null; observacao: string | null;
  }[];
  leitosPrivadosNaoDisponivel: boolean;
  aparelhos: {
    estabelecimento: string | null; cnes: string | null; aparelho: AparelhoImagem | null; quantidadeCnes: number | null;
    emUsoDeFato: number | null; atendeSus: SimNaoParcial | null; correcaoNecessaria: string | null;
  }[];
  aparelhosNaoDisponivel: boolean;
};

export type Bloco8Servidor = RespostaDoBloco & {
  observacoes: string | null;
  documentos: DocumentoAnexavel[];
  assinaturaNome: string | null;
  assinaturaData: string | null;
  declaracao: boolean;
};

/** Arquivo anexado ao item 8.2 (GET /anexos). */
export type Anexo = { id: number; documento: DocumentoAnexavel | "PLANILHA_COLETA"; nome: string; tamanho: number; criadoEm: string };

/** Algo que impede o envio (GET /pendencias). {@code campo} é o caminho do campo dentro do bloco. */
export type Pendencia = { bloco: number; campo: string; mensagem: string; tipo: "OBRIGATORIO" | "COERENCIA" };

/** GET /respostas: todos os blocos de uma vez (revisão e visualização). */
export type Respostas = {
  publicId: string;
  status: StatusQuestionario;
  criadoEm: string;
  enviadoEm: string | null;
  bloco1: Bloco1Servidor;
  bloco2: Bloco2Servidor;
  bloco3: Bloco3Servidor;
  bloco4: Bloco4Servidor;
  bloco5: Bloco5Servidor;
  bloco6: Bloco6Servidor;
  bloco7: Bloco7Servidor;
  bloco8: Bloco8Servidor;
  anexos: Anexo[];
};
