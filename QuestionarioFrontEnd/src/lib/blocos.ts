/** Os 8 blocos do documento, na ordem. {@code etapa}: em que etapa do plano cada bloco entra (`pronto`: os blocos 1 e 2 já existem). */
export type InfoBloco = { numero: number; titulo: string; etapa: string; pronto: boolean };

export const BLOCOS: InfoBloco[] = [
  { numero: 1, titulo: "Identificação", etapa: "E03", pronto: true },
  { numero: 2, titulo: "Prioridades de atenção à saúde", etapa: "E04", pronto: true },
  { numero: 3, titulo: "Fila da regulação ambulatorial", etapa: "E05", pronto: true },
  { numero: 4, titulo: "Regulação hospitalar", etapa: "E06", pronto: true },
  { numero: 5, titulo: "UPAs e prontos-socorros sem vaga", etapa: "E07", pronto: true },
  { numero: 6, titulo: "Seguimento após a urgência", etapa: "E08", pronto: true },
  { numero: 7, titulo: "Financiamento e capacidade", etapa: "E09", pronto: true },
  { numero: 8, titulo: "Observações finais", etapa: "E10", pronto: true },
];

export const PASSO_REVISAO = "revisao";

/** "1".."8" ou "revisao"; qualquer outra coisa não é um passo. */
export function interpretarPasso(passo: string): number | typeof PASSO_REVISAO | null {
  if (passo === PASSO_REVISAO) return PASSO_REVISAO;
  return /^[1-8]$/.test(passo) ? Number(passo) : null;
}
