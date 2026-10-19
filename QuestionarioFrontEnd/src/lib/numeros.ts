/**
 * Números digitados em pt-BR. Os formulários guardam o texto como a pessoa digitou ("1.234,56") e só convertem para
 * número ao enviar ao servidor; assim nada é arredondado ou reformatado enquanto ela digita.
 */

export type TipoNumero = "inteiro" | "decimal" | "dinheiro";

const MAX_INTEIRO = 99_999_999;
const MAX_DECIMAL = 9_999_999.99;
const MAX_DINHEIRO = 999_999_999_999.99; // 12 dígitos: cabe em um número JS sem perder os centavos

const MENSAGENS: Record<TipoNumero, string> = {
  inteiro: "Use um número inteiro, zero ou maior",
  decimal: "Use um número, com até 2 casas depois da vírgula",
  dinheiro: "Use um valor em reais, com vírgula nos centavos (ex.: 1.234,56)",
};

/** Texto digitado -> número. {@code null} se vazio, {@code NaN} se inválido. */
export function paraNumero(texto: string, tipo: TipoNumero): number | null {
  const limpo = texto.trim();
  if (limpo === "") return null;
  let normalizado: string;
  if (tipo === "inteiro") {
    if (!/^\d+$/.test(limpo)) return NaN;
    normalizado = limpo;
  } else if (tipo === "decimal") {
    if (!/^\d+([.,]\d{0,2})?$/.test(limpo)) return NaN;
    normalizado = limpo.replace(",", ".");
  } else {
    // dinheiro: ponto só como separador de milhar, vírgula nos centavos
    if (!/^(\d+|\d{1,3}(\.\d{3})+)(,\d{0,2})?$/.test(limpo)) return NaN;
    normalizado = limpo.replace(/\./g, "").replace(",", ".");
  }
  const numero = Number(normalizado);
  const maximo = tipo === "inteiro" ? MAX_INTEIRO : tipo === "decimal" ? MAX_DECIMAL : MAX_DINHEIRO;
  return Number.isFinite(numero) && numero <= maximo ? numero : NaN;
}

/** Mensagem de erro do texto digitado, ou {@code undefined} se estiver bem (ou vazio). */
export function erroDeNumero(texto: string, tipo: TipoNumero): string | undefined {
  return Number.isNaN(paraNumero(texto, tipo)) ? MENSAGENS[tipo] : undefined;
}

/** Número do servidor -> texto pt-BR para o campo ("1234.5" -> "1.234,50" em dinheiro, "45.5" -> "45,5"). */
export function deNumero(valor: number | null | undefined, tipo: TipoNumero): string {
  if (valor === null || valor === undefined) return "";
  if (tipo === "inteiro") return String(valor);
  if (tipo === "dinheiro") return valor.toLocaleString("pt-BR", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  return valor.toLocaleString("pt-BR", { minimumFractionDigits: 0, maximumFractionDigits: 2, useGrouping: false });
}

/** Valor numérico de um campo já validado (nulo se vazio). */
export function numeroOuNulo(texto: string, tipo: TipoNumero): number | null {
  const n = paraNumero(texto, tipo);
  return n === null || Number.isNaN(n) ? null : n;
}

/** Texto -> {@code null} se vazio, senão aparado. */
export function textoOuNulo(texto: string): string | null {
  const limpo = texto.trim();
  return limpo === "" ? null : limpo;
}

/** Só os dígitos, limitado a {@code max} (SIGTAP 10, CNES 7). */
export function soDigitos(texto: string, max: number): string {
  return texto.replace(/\D/g, "").slice(0, max);
}

/**
 * Filtra o que a pessoa digita ou cola: letras e símbolos nem aparecem no campo.
 * inteiro: só dígitos. decimal: dígitos e uma vírgula (o ponto vira vírgula), até 2 casas.
 * dinheiro: dígitos, ponto de milhar e uma vírgula, até 2 casas.
 */
export function filtrarNumero(texto: string, tipo: TipoNumero): string {
  if (tipo === "inteiro") return texto.replace(/\D/g, "").slice(0, 8);
  let t = texto.replace(/[^\d.,]/g, "");
  if (tipo === "decimal") t = t.replace(/\./g, ",");
  const i = t.indexOf(",");
  if (i >= 0) t = `${t.slice(0, i)},${t.slice(i + 1).replace(/[.,]/g, "").slice(0, 2)}`;
  return t.slice(0, 20);
}
