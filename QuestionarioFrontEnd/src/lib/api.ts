/** Erro devolvido pela API: {@code fields} = erro por campo (400); {@code code} = tipo de conflito (409). */
export class ApiError extends Error {
  constructor(
    public status: number,
    message: string,
    public fields?: Record<string, string>,
    public code?: string,
  ) { super(message); }
}

type ApiErrorPayload = {
  message?: string;
  fields?: Record<string, string>;
  code?: string;
};

function errorMessage(payload: ApiErrorPayload | null, fallback: string) {
  const fieldError = payload?.fields && Object.values(payload.fields).find(Boolean);
  return fieldError ?? payload?.message ?? fallback;
}

export async function apiRequest<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`/api${path}`, {
    ...init,
    credentials: "include",
    // FormData: não definir Content-Type, o navegador precisa gerar o boundary do multipart.
    headers: { ...(init?.body && !(init.body instanceof FormData) ? { "Content-Type": "application/json" } : {}), ...init?.headers },
  });
  if (!response.ok) {
    const payload = await response.json().catch(() => null) as ApiErrorPayload | null;
    throw new ApiError(response.status, errorMessage(payload, `Erro ${response.status}`), payload?.fields, payload?.code);
  }
  if (response.status === 204) return undefined as T;
  return response.json() as Promise<T>;
}

/** Lê o nome do arquivo do Content-Disposition (filename*=UTF-8''... ou filename="..."). */
function nomeDoArquivo(cabecalho: string | null): string | null {
  if (!cabecalho) return null;
  const utf8 = /filename\*=UTF-8''([^;]+)/i.exec(cabecalho);
  if (utf8) { try { return decodeURIComponent(utf8[1]); } catch { /* cai no filename simples */ } }
  const simples = /filename="?([^";]+)"?/i.exec(cabecalho);
  return simples ? simples[1] : null;
}

export async function downloadFile(path: string, fallbackFileName: string): Promise<void> {
  const response = await fetch(`/api${path}`, { credentials: "include" });

  if (!response.ok) {
    const payload = await response.json().catch(() => null) as ApiErrorPayload | null;
    throw new ApiError(response.status, errorMessage(payload, `Erro ${response.status}`));
  }

  const nome = nomeDoArquivo(response.headers.get("content-disposition")) ?? fallbackFileName;
  const blob = await response.blob();
  const url = URL.createObjectURL(blob);

  const link = document.createElement("a");
  link.href = url;
  link.download = nome;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);

  URL.revokeObjectURL(url);
}

export const getJson = <T,>(path: string) => apiRequest<T>(path, { cache: "no-store" });
export const postJson = <T,>(path: string, body?: unknown) => apiRequest<T>(path, { method: "POST", body: body === undefined ? undefined : JSON.stringify(body) });
export const putJson = <T,>(path: string, body: unknown) => apiRequest<T>(path, { method: "PUT", body: JSON.stringify(body) });
export const deleteJson = (path: string) => apiRequest<void>(path, { method: "DELETE" });
