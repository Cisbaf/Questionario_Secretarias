import { ApiError } from "./api";

export const MENSAGEM_SEM_SERVIDOR = "Não foi possível falar com o servidor. Tente novamente em instantes.";

/** Texto de erro para mostrar à pessoa: mensagem da regra quando é um erro de negócio, genérica quando é de infraestrutura. */
export function mensagemDeErro(error: unknown): string {
  if (error instanceof ApiError) {
    return error.status >= 500 ? MENSAGEM_SEM_SERVIDOR : error.message;
  }
  return MENSAGEM_SEM_SERVIDOR;
}

export const naoEncontrado = (error: unknown) => error instanceof ApiError && error.status === 404;
