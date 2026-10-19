import { AppRouterCacheProvider } from "@mui/material-nextjs/v13-appRouter";
import type { Metadata } from "next";
import type { ReactNode } from "react";
import "./globals.css";
import Providers from "./providers";

export const metadata: Metadata = {
  title: "CISBAF | Questionário às Secretarias Municipais de Saúde",
  description: "Levantamento para o planejamento regional da Baixada Fluminense",
};

export default function RootLayout({ children }: { children: ReactNode }) {
  return (
    <html lang="pt-BR">
      <body>
        <AppRouterCacheProvider>
          <Providers>{children}</Providers>
        </AppRouterCacheProvider>
      </body>
    </html>
  );
}
