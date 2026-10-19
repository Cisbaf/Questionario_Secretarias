import { BLOCOS } from "@/lib/blocos";
import {
  CENTRAIS_REGULACAO, DOCUMENTOS_ANEXAVEIS, MOTIVOS_SEM_VAGA, SISTEMAS_REGULACAO, TIPOS_FILA, rotuloDe,
  SIM_NAO_EM_PARTE, SIM_NAO_PARCIAL,
} from "@/lib/rotulos";
import { formatarData } from "@/lib/formatters";
import type { Respostas } from "@/types";

export type ResumoBloco = { numero: number; titulo: string; linhas: string[] };

const lista = (rotulos: string[]) => (rotulos.length > 0 ? rotulos.join("; ") : "não informado");
const qtd = (n: number, singular: string, plural: string) => (n === 1 ? `1 ${singular}` : `${n} ${plural}`);
const contaLinhas = <T extends object>(linhas: T[] | undefined, naoDisp: boolean, singular: string, plural: string) =>
  naoDisp ? "marcado como não disponível"
    : qtd((linhas ?? []).filter((l) => Object.values(l).some((v) => v !== null && v !== "")).length, singular, plural);
const data = (d: string | null) => formatarData(d) ?? "não informada";

/** Resumo curto de cada bloco para a tela de revisão: o que foi respondido, sem repetir cada número. */
export function resumirRespostas(r: Respostas): ResumoBloco[] {
  const b1 = r.bloco1, b2 = r.bloco2, b3 = r.bloco3, b4 = r.bloco4, b5 = r.bloco5, b6 = r.bloco6, b7 = r.bloco7, b8 = r.bloco8;
  const dados: string[][] = [
    [
      `Município: ${b1.municipio ?? "não informado"}`,
      `Responsável: ${b1.responsavel ?? "não informado"}`,
    ],
    [
      `Prioridades preenchidas: ${(b2.prioridades ?? []).filter((p) => p.titulo || p.problema).length} de 5`,
      `Linhas de cuidado avaliadas: ${Object.keys(b2.grausLinhaCuidado ?? {}).length}`,
      `Ordem dos temas: ${Object.keys(b2.ordemTemas ?? {}).length === 6 ? "definida" : "não definida"}`,
    ],
    [
      `Sistemas: ${lista((b3.sistemas ?? []).map((s) => rotuloDe(SISTEMAS_REGULACAO, s)))}`,
      `Fila: ${rotuloDe(TIPOS_FILA, b3.tipoFila) || "não informada"}; classificação de risco: ${rotuloDe(SIM_NAO_EM_PARTE, b3.classificacaoRisco) || "não informada"}`,
      `Filas por especialidade: ${contaLinhas(b3.filas, b3.filasNaoDisponivel, "fila", "filas")} (extração: ${data(b3.dataExtracao)})`,
    ],
    [
      `Tipos de leito com dados: ${b4.leitosNaoDisponivel ? "marcado como não disponível" : (b4.leitos ?? []).filter((l) => !l.naoDisponivel && [l.solicitacoes, l.atendidas, l.canceladas, l.tempoMedioHoras, l.aguardando].some((x) => x !== null)).length}`,
      `Central: ${lista((b4.centrais ?? []).map((c) => rotuloDe(CENTRAIS_REGULACAO, c)))}`,
      `Eletivas: ${contaLinhas(b4.eletivas, b4.eletivasNaoDisponivel, "linha", "linhas")}`,
    ],
    [
      `Unidades de urgência: ${contaLinhas(b5.unidades, b5.unidadesNaoDisponivel, "unidade", "unidades")}`,
      `Perfil dos pacientes: ${b5.perfilNaoDisponivel ? "marcado como não disponível" : qtd((b5.perfil ?? []).filter((p) => p.pacientes !== null || p.diagnosticos).length, "necessidade", "necessidades")}`,
      `Indicadores: ${b5.indicadoresNaoDisponivel ? "marcado como não disponível" : "preenchidos"}; registro informatizado: ${rotuloDe(SIM_NAO_PARCIAL, b5.registroInformatizado) || "não informado"}`,
      `Motivos: ${lista((b5.motivos ?? []).map((m) => rotuloDe(MOTIVOS_SEM_VAGA, m)))}`,
    ],
    [
      `Situações com dados: ${b6.tabelaNaoDisponivel ? "marcado como não disponível" : (b6.situacoes ?? []).filter((s) => !s.naoDisponivel && (s.pacientesAguardando !== null || s.esperaMediaDias !== null || s.ondeFeito)).length}`,
    ],
    [
      `Repasses: ${contaLinhas(b7.repasses, b7.repassesNaoDisponivel, "linha", "linhas")}`,
      `Leitos privados: ${contaLinhas(b7.leitosPrivados, b7.leitosPrivadosNaoDisponivel, "linha", "linhas")}`,
      `Aparelhos de imagem: ${contaLinhas(b7.aparelhos, b7.aparelhosNaoDisponivel, "linha", "linhas")}`,
    ],
    [
      `Observações: ${b8.observacoes ? "preenchidas" : "nenhuma"}`,
      `Documentos a enviar depois: ${lista((b8.documentos ?? []).map((d) => rotuloDe(DOCUMENTOS_ANEXAVEIS, d)))}`,
      `Assinatura: ${b8.assinaturaNome ?? "não informada"}, ${data(b8.assinaturaData)}; declaração ${b8.declaracao ? "marcada" : "não marcada"}`,
    ],
  ];
  return BLOCOS.map((b, i) => ({ numero: b.numero, titulo: b.titulo, linhas: dados[i] }));
}
