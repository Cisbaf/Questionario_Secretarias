"use client";

import { createContext, useContext } from "react";

export type ContextoAdmin = {
  email: string;
  /** Volta ao /login quando o servidor diz 401 (sessão vencida). */
  sessaoExpirou: () => void;
};

const Contexto = createContext<ContextoAdmin | null>(null);
export const AdminProvider = Contexto.Provider;

export function useAdmin(): ContextoAdmin {
  const contexto = useContext(Contexto);
  if (!contexto) throw new Error("useAdmin fora da área do admin");
  return contexto;
}
