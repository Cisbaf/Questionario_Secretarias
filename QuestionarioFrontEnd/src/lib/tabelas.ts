import { deNumero, erroDeNumero, numeroOuNulo, soDigitos, textoOuNulo, type TipoNumero } from "@/lib/numeros";

/**
 * Descrição das colunas de uma tabela do questionário. Cada linha do formulário é um Record de textos (como a pessoa
 * digitou); este módulo converte de/para o JSON do servidor e valida, com os mesmos nomes de campo do backend
 * ("filas[0].sigtap"), para o erro de uma resposta 400 cair no campo certo.
 */
export type TipoCampo = TipoNumero | "texto" | "sigtap" | "cnes" | "data" | "opcao";

export type CampoSpec = {
  nome: string;
  rotulo: string;
  tipo: TipoCampo;
  max?: number;
  opcoes?: readonly { valor: string; rotulo: string }[];
  /** Mostra o campo só quando outro campo da linha tem este valor (ex.: "Outro" -> descrição). */
  soSe?: { campo: string; valor: string };
  /** Mostra o campo só na linha fixa com esta chave. */
  soNaChave?: string;
  largo?: boolean;
};

export type Linha = Record<string, string>;

export const campoVisivel = (c: CampoSpec, linha: Linha, chave?: string): boolean =>
  (!c.soSe || linha[c.soSe.campo] === c.soSe.valor) && (!c.soNaChave || c.soNaChave === chave);

export function linhaVazia(specs: readonly CampoSpec[]): Linha {
  return Object.fromEntries(specs.map((c) => [c.nome, ""]));
}

export const linhaEstaVazia = (linha: Linha): boolean => Object.values(linha).every((v) => v.trim() === "");

export function linhaDeServidor(specs: readonly CampoSpec[], o: Record<string, unknown> | null | undefined): Linha {
  const linha = linhaVazia(specs);
  if (!o) return linha;
  for (const c of specs) {
    const v = o[c.nome];
    if (v === null || v === undefined) continue;
    linha[c.nome] = typeof v === "number"
      ? deNumero(v, c.tipo === "inteiro" || c.tipo === "decimal" || c.tipo === "dinheiro" ? c.tipo : "inteiro")
      : String(v);
  }
  return linha;
}

export function linhaParaServidor(specs: readonly CampoSpec[], linha: Linha, chave?: string): Record<string, unknown> {
  const saida: Record<string, unknown> = {};
  for (const c of specs) {
    const texto = linha[c.nome] ?? "";
    const visivel = campoVisivel(c, linha, chave);
    if (!visivel) { saida[c.nome] = null; continue; }
    if (c.tipo === "inteiro" || c.tipo === "decimal" || c.tipo === "dinheiro") saida[c.nome] = numeroOuNulo(texto, c.tipo);
    else saida[c.nome] = textoOuNulo(texto);
  }
  return saida;
}

export function erroDeCampo(c: CampoSpec, texto: string): string | undefined {
  if (c.tipo === "inteiro" || c.tipo === "decimal" || c.tipo === "dinheiro") return erroDeNumero(texto, c.tipo);
  if (c.tipo === "sigtap") return texto !== "" && !/^\d{10}$/.test(texto) ? "O código SIGTAP tem 10 números" : undefined;
  if (c.tipo === "cnes") return texto !== "" && !/^\d{7}$/.test(texto) ? "O CNES tem 7 números" : undefined;
  if (c.tipo === "texto" && c.max && texto.trim().length > c.max) return `Máximo de ${c.max} caracteres`;
  return undefined;
}

/** Aplica a máscara de digitação do campo (só números em SIGTAP/CNES). */
export function filtrarDigitacao(c: CampoSpec, texto: string): string {
  if (c.tipo === "sigtap") return soDigitos(texto, 10);
  if (c.tipo === "cnes") return soDigitos(texto, 7);
  return texto;
}

export function validarLinhas(
  specs: readonly CampoSpec[], linhas: Linha[], nome: string, chaves?: readonly string[],
): Record<string, string> {
  const erros: Record<string, string> = {};
  linhas.forEach((linha, i) => {
    for (const c of specs) {
      if (!campoVisivel(c, linha, chaves?.[i])) continue;
      const e = erroDeCampo(c, linha[c.nome] ?? "");
      if (e) erros[`${nome}[${i}].${c.nome}`] = e;
    }
  });
  return erros;
}

/** Linhas dinâmicas: as vazias não vão ao servidor. */
export const linhasPreenchidas = (linhas: Linha[]): Linha[] => linhas.filter((l) => !linhaEstaVazia(l));

export const num = (texto: string | undefined, tipo: TipoNumero = "inteiro"): number | null =>
  numeroOuNulo(texto ?? "", tipo);

// ---------- tabelas de linhas fixas (uma linha por opção do documento) ----------

export type FixasValores = { linhas: Linha[]; naoDisp: boolean[] };

export function fixasDeServidor(
  specs: readonly CampoSpec[], chaves: readonly string[], campoChave: string,
  recebidas: Record<string, unknown>[] | null | undefined,
): FixasValores {
  const porChave = new Map((recebidas ?? []).map((r) => [String(r[campoChave]), r]));
  return {
    linhas: chaves.map((k) => linhaDeServidor(specs, porChave.get(k))),
    naoDisp: chaves.map((k) => porChave.get(k)?.naoDisponivel === true),
  };
}

export function fixasParaServidor(
  specs: readonly CampoSpec[], chaves: readonly string[], campoChave: string, v: FixasValores,
): Record<string, unknown>[] {
  return chaves.map((k, i) => ({
    ...linhaParaServidor(specs, v.naoDisp[i] ? linhaVazia(specs) : v.linhas[i], k),
    [campoChave]: k,
    naoDisponivel: v.naoDisp[i],
  }));
}

export const fixasVazias = (specs: readonly CampoSpec[], n: number): FixasValores => ({
  linhas: Array.from({ length: n }, () => linhaVazia(specs)),
  naoDisp: Array.from({ length: n }, () => false),
});

/** Marca ou desmarca "Não disponível" numa linha fixa; ao marcar, limpa os campos dela. */
export function marcarNaoDisp(specs: readonly CampoSpec[], v: FixasValores, i: number, marcado: boolean): FixasValores {
  return {
    naoDisp: v.naoDisp.map((x, j) => (j === i ? marcado : x)),
    linhas: marcado ? v.linhas.map((l, j) => (j === i ? linhaVazia(specs) : l)) : v.linhas,
  };
}

/** Tabela dinâmica: pelo menos uma linha em branco para a pessoa começar. */
export function dinamicasDeServidor(specs: readonly CampoSpec[], recebidas: Record<string, unknown>[] | null | undefined): Linha[] {
  const linhas = (recebidas ?? []).map((r) => linhaDeServidor(specs, r));
  return linhas.length > 0 ? linhas : [linhaVazia(specs)];
}

export const dinamicasParaServidor = (specs: readonly CampoSpec[], linhas: Linha[]) =>
  linhasPreenchidas(linhas).map((l) => linhaParaServidor(specs, l));

/** Verdadeiro se há alguma coisa preenchida. */
export const algumaPreenchida = (linhas: Linha[]) => linhas.some((l) => !linhaEstaVazia(l));
