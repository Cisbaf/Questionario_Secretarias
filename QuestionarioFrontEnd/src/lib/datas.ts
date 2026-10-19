/** Hoje no fuso de Brasília, "AAAA-MM-DD" (o navegador pode estar em outro fuso). */
export function hojeIso(): string {
  return new Intl.DateTimeFormat("sv-SE", { timeZone: "America/Sao_Paulo" }).format(new Date());
}

export const mesAtualIso = (): string => hojeIso().slice(0, 7);

/** Erro de uma data digitada ("AAAA-MM-DD"); vazio é válido. {@code permiteFutura}: vigências, prazos. */
export function erroDeData(texto: string, permiteFutura = false): string | undefined {
  if (texto === "") return undefined;
  if (!/^\d{4}-\d{2}-\d{2}$/.test(texto)) return "Data inválida";
  const [a, m, d] = texto.split("-").map(Number);
  const real = new Date(Date.UTC(a, m - 1, d));
  if (a < 1900 || real.getUTCFullYear() !== a || real.getUTCMonth() !== m - 1 || real.getUTCDate() !== d) return "Data inválida";
  if (!permiteFutura && texto > hojeIso()) return "A data não pode ser futura";
  return undefined;
}

export function erroDeMes(texto: string): string | undefined {
  if (texto === "") return undefined;
  if (/^(\d{4}-|-\d{2})$/.test(texto)) return "Escolha o mês e o ano";
  if (!/^\d{4}-(0[1-9]|1[0-2])$/.test(texto)) return "Mês e ano inválidos";
  return texto > mesAtualIso() ? "O mês não pode ser futuro" : undefined;
}
