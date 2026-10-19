"use client";

import { ApiError, postJson } from "@/lib/api";
import { mensagemDeErro } from "@/lib/erros";
import { Alert, Box, Button, Paper, Stack, TextField, Typography } from "@mui/material";
import { useRouter } from "next/navigation";
import { type FormEvent, useState } from "react";

/** Login do admin único (equipe CISBAF). O questionário dos municípios não passa por aqui. */
export default function LoginPage() {
  const router = useRouter();
  const [email, setEmail] = useState("");
  const [senha, setSenha] = useState("");
  const [erro, setErro] = useState("");
  const [entrando, setEntrando] = useState(false);

  async function entrar(e: FormEvent) {
    e.preventDefault();
    setErro("");
    if (!email.trim() || !senha) { setErro("Informe o e-mail e a senha."); return; }
    setEntrando(true);
    try {
      await postJson("/auth/login", { email: email.trim(), senha });
      router.replace("/admin");
    } catch (error) {
      // 401 = credenciais erradas, 429 = bloqueio temporário: ambos já trazem a mensagem certa do servidor.
      setErro(error instanceof ApiError && (error.status === 401 || error.status === 429 || error.status === 400) ? error.message : mensagemDeErro(error));
      setEntrando(false);
    }
  }

  return (
    <Box sx={{ minHeight: "100vh", display: "grid", placeItems: "center", p: 2 }}>
      <Paper variant="outlined" sx={{ p: { xs: 3, sm: 4 }, width: "100%", maxWidth: 420 }}>
        <Stack alignItems="center" spacing={1} sx={{ mb: 3 }}>
          <Box component="img" src="/cisbaf.png" alt="CISBAF" sx={{ height: 56, width: 56, objectFit: "contain" }} />
          <Typography variant="h5" component="h1" sx={{ fontWeight: 800 }}>Área da equipe</Typography>
          <Typography variant="body2" color="text.secondary" align="center">
            Acesso da equipe do CISBAF aos questionários recebidos.
          </Typography>
        </Stack>
        <Box component="form" onSubmit={entrar} noValidate>
          <Stack spacing={2}>
            <TextField label="E-mail" type="email" value={email} onChange={(e) => setEmail(e.target.value)}
              autoComplete="username" autoFocus fullWidth slotProps={{ htmlInput: { maxLength: 150 } }} />
            <TextField label="Senha" type="password" value={senha} onChange={(e) => setSenha(e.target.value)}
              autoComplete="current-password" fullWidth slotProps={{ htmlInput: { maxLength: 200 } }} />
            {erro && <Alert severity="error" role="alert">{erro}</Alert>}
            <Button type="submit" variant="contained" size="large" disabled={entrando}>
              {entrando ? "Entrando…" : "Entrar"}
            </Button>
          </Stack>
        </Box>
      </Paper>
    </Box>
  );
}
