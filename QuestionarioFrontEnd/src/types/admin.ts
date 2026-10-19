import type { Pendencia, Respostas, StatusQuestionario } from "@/types";

/** Uma linha da lista do admin (GET /admin/questionarios). */
export type AdminResumo = {
  publicId: string;
  municipio: string | null;
  status: StatusQuestionario;
  blocosSalvos: number;
  blocosCompletos: number;
  totalBlocos: number;
  pendencias: number;
  criadoEm: string;
  atualizadoEm: string;
  enviadoEm: string | null;
  municipioRepetido: boolean;
  semMunicipio: boolean;
};

/** GET /admin/questionarios/{uuid}: as respostas dos 8 blocos e o que ainda falta. */
export type AdminDetalhe = { respostas: Respostas; pendencias: Pendencia[] };

export type FiltroStatus = "TODOS" | "ENVIADO" | "RASCUNHO";
