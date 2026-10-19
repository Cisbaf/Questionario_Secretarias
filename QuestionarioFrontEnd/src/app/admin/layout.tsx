"use client";

import { AdminProvider } from "@/components/admin/AdminContext";
import { ApiError, getJson, postJson } from "@/lib/api";
import { mensagemDeErro } from "@/lib/erros";
import LogoutIcon from "@mui/icons-material/Logout";
import { Alert, Box, Button, CircularProgress, Container, Stack, Typography } from "@mui/material";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { type ReactNode, useCallback, useEffect, useMemo, useState } from "react";

type Estado = { fase: "verificando" } | { fase: "ok"; email: string } | { fase: "erro"; mensagem: string };

/**
 * Moldura da área do admin: confere o login ao abrir (GET /auth/validate) e leva ao /login se não houver sessão.
 * A proteção de verdade está no backend (/api/admin/** dá 401 sem token); aqui é só para não mostrar uma tela vazia.
 */
export default function AdminLayout({ children }: { children: ReactNode }) {
  const router = useRouter();
  const [estado, setEstado] = useState<Estado>({ fase: "verificando" });

  const paraOLogin = useCallback(() => router.replace("/login"), [router]);

  useEffect(() => {
    let ativo = true;
    getJson<{ email: string }>("/auth/validate")
      .then((r) => { if (ativo) setEstado({ fase: "ok", email: r.email }); })
      .catch((e) => {
        if (!ativo) return;
        if (e instanceof ApiError && e.status === 401) paraOLogin();
        else setEstado({ fase: "erro", mensagem: mensagemDeErro(e) });
      });
    return () => { ativo = false; };
  }, [paraOLogin]);

  async function sair() {
    try { await postJson("/auth/logout"); } finally { paraOLogin(); }
  }

  const contexto = useMemo(() => (estado.fase === "ok" ? { email: estado.email, sessaoExpirou: paraOLogin } : null), [estado, paraOLogin]);

  if (estado.fase === "verificando") {
    return <Stack alignItems="center" sx={{ pt: 12 }}><CircularProgress aria-label="Verificando o login" /></Stack>;
  }
  if (estado.fase === "erro" || !contexto) {
    return (
      <Container maxWidth="sm" sx={{ pt: 8 }}>
        <Alert severity="error">{estado.fase === "erro" ? estado.mensagem : "Não foi possível abrir a área da equipe."}</Alert>
      </Container>
    );
  }

  return (
    <AdminProvider value={contexto}>
      <Box sx={{ minHeight: "100vh", pb: 8 }}>
        <Box component="header" sx={{ bgcolor: "#1D4A3A", color: "#fff" }}>
          <Container maxWidth="xl" sx={{ display: "flex", alignItems: "center", gap: 2, py: 1.5, flexWrap: "wrap" }}>
            <Box sx={{ bgcolor: "#fff", borderRadius: 1.5, p: 0.5, display: "flex" }}>
              <Box component="img" src="/cisbaf.png" alt="CISBAF" sx={{ height: 36, width: 36, objectFit: "contain" }} />
            </Box>
            <Box component={Link} href="/admin" sx={{ color: "inherit", textDecoration: "none", flex: 1, minWidth: 200 }}>
              <Typography sx={{ fontWeight: 800, lineHeight: 1.1 }}>CISBAF · Área da equipe</Typography>
              <Typography sx={{ fontSize: 12, color: "#BFD3C8" }}>Questionários das Secretarias Municipais de Saúde</Typography>
            </Box>
            <Typography sx={{ fontSize: 13, color: "#BFD3C8" }} data-testid="admin-email">{estado.email}</Typography>
            <Button onClick={() => void sair()} color="inherit" size="small" startIcon={<LogoutIcon />} sx={{ border: "1px solid rgba(255,255,255,0.35)" }}>
              Sair
            </Button>
          </Container>
        </Box>
        <Container maxWidth="xl" sx={{ pt: 4 }}>{children}</Container>
      </Box>
    </AdminProvider>
  );
}
