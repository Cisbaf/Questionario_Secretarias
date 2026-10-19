export const A_DEFINIR = "a definir";

/** "2026-11-05" -> "05/11/2026", sem passar por Date para não deslocar o dia por fuso horário. */
export function formatarData(iso: string | null | undefined): string | null {
  if (!iso) return null;
  const [ano, mes, dia] = iso.slice(0, 10).split("-");
  if (!ano || !mes || !dia) return iso;
  return `${dia}/${mes}/${ano}`;
}

/** "2026-10-19T10:40:12.123" -> "19/10/2026 às 10:40". */
export function formatarDataHora(iso: string | null | undefined): string | null {
  const data = formatarData(iso);
  if (!data || !iso) return null;
  const hora = iso.slice(11, 16);
  return hora.length === 5 ? `${data} às ${hora}` : data;
}

export function formatarPeriodo(inicio: string | null, fim: string | null): string {
  const de = formatarData(inicio);
  const ate = formatarData(fim);
  if (de && ate) return `${de} a ${ate}`;
  if (de) return `a partir de ${de}`;
  if (ate) return `até ${ate}`;
  return A_DEFINIR;
}
