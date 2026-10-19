// Motor do rascunho local (localStorage). Regra do projeto: tudo que a pessoa digita num bloco é guardado aqui ANTES
// de ir ao servidor, e só é apagado depois que o servidor confirma (2xx). Assim um erro de rede, um 500 ou fechar a
// aba por engano não perde o que foi digitado.
//
// Chave:   qsms:v1:q{uuid}:b{n}      (o "v1" permite invalidar rascunhos se o formato de um bloco mudar)
// Valor:   { schema, baseVersion, savedLocallyAt, data }
//
// Todo acesso fica dentro de try/catch: o localStorage pode estar bloqueado, cheio ou desligado (janela privada,
// política do navegador). Nesses casos o formulário continua funcionando; só perde a proteção, e quem chama é avisado
// pelo retorno de gravarRascunho para mostrar um aviso discreto.

const SCHEMA = 1;
const TOTAL_BLOCOS = 8;
const EVENTO = "qsms:rascunhos";

export type RascunhoLocal<T> = {
  schema: number;
  /** Versão do bloco no servidor sobre a qual a pessoa estava editando (null = ainda não existia no servidor). */
  baseVersion: number | null;
  /** Horário local do navegador, "AAAA-MM-DDTHH:mm:ss" (sem fuso, no mesmo formato que o servidor usa). */
  savedLocallyAt: string;
  data: T;
};

export const chaveDoRascunho = (uuid: string, numero: number) => `qsms:v1:q${uuid}:b${numero}`;

function agoraLocalIso(): string {
  const d = new Date();
  const dois = (n: number) => String(n).padStart(2, "0");
  return `${d.getFullYear()}-${dois(d.getMonth() + 1)}-${dois(d.getDate())}T${dois(d.getHours())}:${dois(d.getMinutes())}:${dois(d.getSeconds())}`;
}

function avisarMudanca() {
  try {
    window.dispatchEvent(new Event(EVENTO));
  } catch {
    // sem window/eventos: ninguém para avisar
  }
}

/** Lê o rascunho do bloco. Valor ausente, ilegível ou de outro formato (schema) = sem rascunho. */
export function lerRascunho<T>(uuid: string, numero: number): RascunhoLocal<T> | null {
  try {
    const bruto = window.localStorage.getItem(chaveDoRascunho(uuid, numero));
    if (!bruto) return null;
    const valor = JSON.parse(bruto) as Partial<RascunhoLocal<T>> | null;
    if (!valor || valor.schema !== SCHEMA || typeof valor.data !== "object" || valor.data === null) return null;
    return {
      schema: SCHEMA,
      baseVersion: typeof valor.baseVersion === "number" ? valor.baseVersion : null,
      savedLocallyAt: typeof valor.savedLocallyAt === "string" ? valor.savedLocallyAt : "",
      data: valor.data,
    };
  } catch {
    return null;
  }
}

/** Grava (de forma síncrona). Devolve false se o navegador não deixou (cheio ou bloqueado). */
export function gravarRascunho<T>(uuid: string, numero: number, baseVersion: number | null, data: T): boolean {
  try {
    const valor: RascunhoLocal<T> = { schema: SCHEMA, baseVersion, savedLocallyAt: agoraLocalIso(), data };
    window.localStorage.setItem(chaveDoRascunho(uuid, numero), JSON.stringify(valor));
    avisarMudanca();
    return true;
  } catch {
    return false;
  }
}

export function apagarRascunho(uuid: string, numero: number): void {
  try {
    window.localStorage.removeItem(chaveDoRascunho(uuid, numero));
  } catch {
    // sem localStorage não há o que apagar
  }
  avisarMudanca();
}

/** Números dos blocos (1..8) que têm rascunho local ainda não confirmado pelo servidor. */
export function blocosComRascunho(uuid: string): number[] {
  const pendentes: number[] = [];
  for (let n = 1; n <= TOTAL_BLOCOS; n++) {
    if (lerRascunho(uuid, n)) pendentes.push(n);
  }
  return pendentes;
}

/** Para useSyncExternalStore: avisa quando um rascunho muda nesta aba ou em outra aba do mesmo navegador. */
export function assinarRascunhos(callback: () => void): () => void {
  window.addEventListener(EVENTO, callback);
  window.addEventListener("storage", callback);
  return () => {
    window.removeEventListener(EVENTO, callback);
    window.removeEventListener("storage", callback);
  };
}
