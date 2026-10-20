import { apiRequest, ApiError, deleteJson, getJson } from "@/lib/api";
import type { Anexo } from "@/types";
import type { DocumentoAnexavel } from "@/lib/rotulos";

export type DocumentoEnviavel = DocumentoAnexavel | "PLANILHA_COLETA";

/** Modelo da planilha de coleta (arquivo estático do site) e o formato que volta preenchido. */
export const URL_MODELO_PLANILHA = "/modelos/Planilha_Coleta_Filas_UPA_Plano_Macro_BF.xlsx";
export const EXTENSOES_PLANILHA = ["xlsx", "xls", "ods"] as const;

/** Limites do 8.2 (o servidor confere de novo; aqui é só para avisar antes). */
export const MAX_ARQUIVOS_POR_DOCUMENTO = 3;
export const MAX_MB = 10;
export const EXTENSOES = ["pdf", "doc", "docx", "xls", "xlsx", "odt", "ods", "csv", "png", "jpg", "jpeg"] as const;
export const ACCEPT = EXTENSOES.map((e) => `.${e}`).join(",");
export const FORMATOS_TEXTO = "PDF, Word, Excel, CSV, OpenDocument ou imagem (PNG/JPG)";

export const listarAnexos = (uuid: string) => getJson<Anexo[]>(`/questionarios/${encodeURIComponent(uuid)}/anexos`);

export function enviarAnexo(uuid: string, documento: DocumentoEnviavel, arquivo: File): Promise<Anexo> {
  const dados = new FormData();
  dados.append("documento", documento);
  dados.append("arquivo", arquivo);
  return apiRequest<Anexo>(`/questionarios/${encodeURIComponent(uuid)}/anexos`, { method: "POST", body: dados });
}

export const excluirAnexo = (uuid: string, id: number) =>
  deleteJson(`/questionarios/${encodeURIComponent(uuid)}/anexos/${id}`);

/** Link direto de download (o servidor manda como anexo, com o nome original). */
export const urlDoAnexo = (uuid: string, id: number) => `/api/questionarios/${encodeURIComponent(uuid)}/anexos/${id}/arquivo`;

/** Mensagem de recusa feita no próprio navegador, antes de enviar; null se o arquivo parece ok. */
export function recusaLocal(arquivo: File, permitidas: readonly string[] = EXTENSOES): string | null {
  const ext = arquivo.name.includes(".") ? arquivo.name.split(".").pop()!.toLowerCase() : "";
  if (permitidas === EXTENSOES_PLANILHA && !permitidas.includes(ext)) return `“${arquivo.name}”: envie a planilha preenchida em Excel (.xlsx ou .xls) ou OpenDocument (.ods).`;
  if (!permitidas.includes(ext)) return `“${arquivo.name}”: formato não aceito. Envie ${FORMATOS_TEXTO}.`;
  if (arquivo.size === 0) return `“${arquivo.name}” está vazio.`;
  if (arquivo.size > MAX_MB * 1024 * 1024) return `“${arquivo.name}” passa de ${MAX_MB} MB.`;
  return null;
}

export function tamanhoLegivel(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${Math.round(bytes / 1024)} KB`;
  return `${(bytes / 1024 / 1024).toFixed(1).replace(".", ",")} MB`;
}

export function mensagemDoErro(e: unknown): string {
  if (e instanceof ApiError) return e.status === 413 ? `O arquivo passa de ${MAX_MB} MB.` : e.message;
  return "Não foi possível enviar agora. Confira a conexão e tente de novo.";
}
