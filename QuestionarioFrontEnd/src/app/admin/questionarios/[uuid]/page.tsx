"use client";

import { useAdmin } from "@/components/admin/AdminContext";
import RespostasCompletas from "@/components/admin/RespostasCompletas";
import { ApiError, downloadFile, getJson } from "@/lib/api";
import { mensagemDeErro, naoEncontrado } from "@/lib/erros";
import { formatarDataHora } from "@/lib/formatters";
import type { AdminDetalhe } from "@/types/admin";
import ArrowBackIcon from "@mui/icons-material/ArrowBack";
import DownloadIcon from "@mui/icons-material/Download";
import PictureAsPdfIcon from "@mui/icons-material/PictureAsPdf";
import { Alert, Box, Button, Chip, CircularProgress, Stack, Typography } from "@mui/material";
import Link from "next/link";
import { useParams } from "next/navigation";
import { useEffect, useState } from "react";

/** O que um município respondeu, só leitura, com as pendências no alto e o Excel desse município. */
export default function AdminQuestionarioPage() {
  const { uuid } = useParams<{ uuid: string }>();
  const { sessaoExpirou } = useAdmin();
  const [detalhe, setDetalhe] = useState<AdminDetalhe | null>(null);
  const [erro, setErro] = useState("");
  const [baixando, setBaixando] = useState("");

  useEffect(() => {
    let ativo = true;
    getJson<AdminDetalhe>(`/admin/questionarios/${encodeURIComponent(uuid)}`)
      .then((d) => { if (ativo) setDetalhe(d); })
      .catch((e) => {
        if (!ativo) return;
        if (e instanceof ApiError && e.status === 401) sessaoExpirou();
        else setErro(naoEncontrado(e) ? "Questionário não encontrado." : mensagemDeErro(e));
      });
    return () => { ativo = false; };
  }, [uuid, sessaoExpirou]);

  async function baixar(formato: "excel" | "pdf") {
    setBaixando(formato);
    try {
      await downloadFile(`/admin/questionarios/${encodeURIComponent(uuid)}/${formato}`, formato === "pdf" ? "questionario.pdf" : "questionario.xlsx");
    } catch (e) {
      if (e instanceof ApiError && e.status === 401) sessaoExpirou();
      else setErro(mensagemDeErro(e));
    } finally {
      setBaixando("");
    }
  }

  const r = detalhe?.respostas;
  return (
    <Box>
      <Button component={Link} href="/admin" size="small" startIcon={<ArrowBackIcon />} sx={{ mb: 2 }}>Voltar à lista</Button>
      {erro && <Alert severity="error" role="alert" sx={{ mb: 2 }}>{erro}</Alert>}
      {!r && !erro && <Stack alignItems="center" sx={{ py: 6 }}><CircularProgress /></Stack>}
      {r && detalhe && (
        <>
          <Stack direction={{ xs: "column", md: "row" }} justifyContent="space-between" alignItems={{ md: "flex-start" }} spacing={2} sx={{ mb: 3 }}>
            <Box>
              <Typography variant="h4" component="h1" data-testid="titulo-municipio">{r.bloco1.municipio ?? "(sem município)"}</Typography>
              <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap" useFlexGap sx={{ mt: 1 }}>
                <Chip size="small" color={r.status === "ENVIADO" ? "success" : "default"} label={r.status === "ENVIADO" ? "Enviado" : "Em preenchimento"} />
                <Typography variant="body2" color="text.secondary">
                  Iniciado em {formatarDataHora(r.criadoEm)}{r.enviadoEm ? ` · enviado em ${formatarDataHora(r.enviadoEm)}` : ""}
                </Typography>
              </Stack>
            </Box>
            <Stack direction="row" spacing={1}>
              <Button variant="contained" startIcon={<DownloadIcon />} disabled={baixando !== ""} onClick={() => void baixar("excel")}>
                {baixando === "excel" ? "Gerando…" : "Baixar Excel"}
              </Button>
              <Button variant="outlined" startIcon={<PictureAsPdfIcon />} disabled={baixando !== ""} onClick={() => void baixar("pdf")}>
                {baixando === "pdf" ? "Gerando…" : "Baixar PDF"}
              </Button>
            </Stack>
          </Stack>

          {detalhe.pendencias.length > 0 ? (
            <Alert severity="warning" sx={{ mb: 3 }} data-testid="pendencias-admin">
              <strong>
                {r.status === "ENVIADO" ? "Pontos em aberto:" : `Ainda falta resolver ${detalhe.pendencias.length} ponto(s) para o envio:`}
              </strong>
              {[...new Set(detalhe.pendencias.map((p) => p.bloco))].map((bloco) => {
                const doBloco = detalhe.pendencias.filter((p) => p.bloco === bloco);
                return (
                  <Box component="details" key={bloco} sx={{ mt: 1 }}>
                    <Box component="summary" sx={{ cursor: "pointer", fontWeight: 600 }}>
                      Bloco {bloco}: {doBloco.length} ponto(s)
                    </Box>
                    <Box component="ul" sx={{ m: 0, mt: 0.5, pl: 2.5 }}>
                      {doBloco.map((p) => <li key={p.campo}>{p.mensagem}</li>)}
                    </Box>
                  </Box>
                );
              })}
            </Alert>
          ) : (
            <Alert severity="success" sx={{ mb: 3 }}>Sem pendências: todos os campos obrigatórios estão preenchidos.</Alert>
          )}
          <RespostasCompletas r={r} />
        </>
      )}
    </Box>
  );
}
