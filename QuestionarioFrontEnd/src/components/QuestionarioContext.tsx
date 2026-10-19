"use client";

import type { Questionario } from "@/types";
import { createContext, useContext } from "react";

export type ContextoDoQuestionario = {
  uuid: string;
  /** Null enquanto carrega ou se o servidor estiver fora do ar (o rascunho local ainda pode ser usado). */
  questionario: Questionario | null;
  /** Recarrega a situação dos blocos (chamado depois que um bloco é salvo). */
  recarregarQuestionario: () => Promise<void>;
};

const Contexto = createContext<ContextoDoQuestionario | null>(null);

export const QuestionarioProvider = Contexto.Provider;

export function useQuestionario(): ContextoDoQuestionario {
  const contexto = useContext(Contexto);
  if (!contexto) throw new Error("useQuestionario fora do QuestionarioProvider");
  return contexto;
}
