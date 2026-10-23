"use client";

import { useAdmin } from "@/components/admin/AdminContext";
import { ApiError, deleteJson, downloadFile, getJson } from "@/lib/api";
import { mensagemDeErro } from "@/lib/erros";
import { formatarDataHora } from "@/lib/formatters";
import type { AdminResumo, FiltroStatus } from "@/types/admin";
import DeleteIcon from "@mui/icons-material/Delete";
import DownloadIcon from "@mui/icons-material/Download";
import PictureAsPdfIcon from "@mui/icons-material/PictureAsPdf";
import VisibilityIcon from "@mui/icons-material/Visibility";
import {
  Alert, Box, Button, Chip, CircularProgress, Dialog, DialogActions, DialogContent, DialogContentText, DialogTitle, Menu, MenuItem, Paper, Stack, Table, TableBody, TableCell, TableContainer, TableHead,
  TableRow, ToggleButton, ToggleButtonGroup, Tooltip, Typography,
} from "@mui/material";
import Link from "next/link";
import { useCallback, useEffect, useMemo, useState } from "react";

const FILTROS: { valor: FiltroStatus; rotulo: string }[] = [
  { valor: "TODOS", rotulo: "Todos" },
  { valor: "ENVIADO", rotulo: "Enviados" },
  { valor: "RASCUNHO", rotulo: "Em preenchimento" },
];

const CONSOLIDADOS = [
  { chave: "excel-enviados", formato: "excel", status: "ENVIADO", rotulo: "Excel · só enviados", arquivo: "questionarios-consolidado-enviado.xlsx" },
  { chave: "excel-todos", formato: "excel", status: "TODOS", rotulo: "Excel · todos", arquivo: "questionarios-consolidado-todos.xlsx" },
  { chave: "pdf-enviados", formato: "pdf", status: "ENVIADO", rotulo: "PDF · só enviados", arquivo: "questionarios-consolidado-enviado.pdf" },
  { chave: "pdf-todos", formato: "pdf", status: "TODOS", rotulo: "PDF · todos", arquivo: "questionarios-consolidado-todos.pdf" },
] as const;

/** Lista dos questionários recebidos, com Ver e Excel (por município e consolidado). */
export default function AdminPage() {
  const { sessaoExpirou } = useAdmin();
  const [filtro, setFiltro] = useState<FiltroStatus>("TODOS");
  const [lista, setLista] = useState<AdminResumo[] | null>(null);
  const [erro, setErro] = useState("");
  const [baixando, setBaixando] = useState("");
  const [excluindo, setExcluindo] = useState<AdminResumo | null>(null);
  const [apagando, setApagando] = useState(false);
  const [menuConsolidado, setMenuConsolidado] = useState<HTMLElement | null>(null);

  const tratar = useCallback((e: unknown) => {
    if (e instanceof ApiError && e.status === 401) sessaoExpirou();
    else setErro(mensagemDeErro(e));
  }, [sessaoExpirou]);

  const carregar = useCallback(async () => {
    setErro("");
    try {
      setLista(await getJson<AdminResumo[]>(`/admin/questionarios?status=${filtro}`));
    } catch (e) {
      tratar(e);
    }
  }, [filtro, tratar]);

  useEffect(() => { void carregar(); }, [carregar]);

  async function baixar(chave: string, caminho: string, nome: string) {
    setBaixando(chave);
    setErro("");
    try {
      await downloadFile(caminho, nome);
    } catch (e) {
      tratar(e);
    } finally {
      setBaixando("");
    }
  }

  async function confirmarExclusao() {
    if (!excluindo) return;
    setApagando(true);
    setErro("");
    try {
      await deleteJson(`/admin/questionarios/${excluindo.publicId}`);
      setExcluindo(null);
      await carregar();
    } catch (e) {
      setExcluindo(null);
      tratar(e);
    } finally {
      setApagando(false);
    }
  }

  const resumo = useMemo(() => {
    const itens = lista ?? [];
    return {
      total: itens.length,
      enviados: itens.filter((i) => i.status === "ENVIADO").length,
      repetidos: itens.filter((i) => i.municipioRepetido).length,
      semMunicipio: itens.filter((i) => i.semMunicipio).length,
    };
  }, [lista]);

  return (
    <Box>
      <Stack direction={{ xs: "column", md: "row" }} justifyContent="space-between" alignItems={{ md: "center" }} spacing={2} sx={{ mb: 3 }}>
        <Box>
          <Typography variant="h4" component="h1">Questionários recebidos</Typography>
          <Typography color="text.secondary">
            {lista === null ? "Carregando…" : `${resumo.total} no filtro atual, ${resumo.enviados} enviado(s).`}
          </Typography>
        </Box>
        <Box>
          <Button variant="contained" startIcon={<DownloadIcon />} disabled={baixando !== ""} data-testid="consolidado"
            onClick={(e) => setMenuConsolidado(e.currentTarget)}>
            {baixando.startsWith("c") ? "Gerando…" : "Baixar consolidado"}
          </Button>
          <Menu anchorEl={menuConsolidado} open={menuConsolidado !== null} onClose={() => setMenuConsolidado(null)}>
            {CONSOLIDADOS.map((c) => (
              <MenuItem key={c.chave} data-testid={`consolidado-${c.chave}`} onClick={() => {
                setMenuConsolidado(null);
                void baixar(`c-${c.chave}`, `/admin/questionarios/${c.formato}?status=${c.status}`, c.arquivo);
              }}>
                {c.formato === "pdf" ? <PictureAsPdfIcon fontSize="small" sx={{ mr: 1 }} /> : <DownloadIcon fontSize="small" sx={{ mr: 1 }} />}
                {c.rotulo}
              </MenuItem>
            ))}
          </Menu>
        </Box>
      </Stack>

      <Stack direction="row" spacing={2} alignItems="center" flexWrap="wrap" useFlexGap sx={{ mb: 2 }}>
        <ToggleButtonGroup exclusive size="small" value={filtro} onChange={(_, v: FiltroStatus | null) => v && setFiltro(v)} aria-label="Filtro por situação">
          {FILTROS.map((f) => <ToggleButton key={f.valor} value={f.valor}>{f.rotulo}</ToggleButton>)}
        </ToggleButtonGroup>
        {resumo.repetidos > 0 && <Chip color="warning" size="small" label={`${resumo.repetidos} com município repetido`} />}
        {resumo.semMunicipio > 0 && <Chip color="warning" size="small" variant="outlined" label={`${resumo.semMunicipio} sem município`} />}
      </Stack>

      {erro && <Alert severity="error" sx={{ mb: 2 }} role="alert">{erro}</Alert>}
      {lista === null && !erro && <Stack alignItems="center" sx={{ py: 6 }}><CircularProgress /></Stack>}

      {lista !== null && (
        <TableContainer component={Paper} variant="outlined">
          <Table size="small" aria-label="Questionários recebidos" data-testid="tabela-questionarios">
            <TableHead>
              <TableRow>
                <TableCell>Município</TableCell>
                <TableCell>Situação</TableCell>
                <TableCell align="center">Blocos completos</TableCell>
                <TableCell align="center">Pendências</TableCell>
                <TableCell>Última atualização</TableCell>
                <TableCell>Enviado em</TableCell>
                <TableCell align="right">Ações</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {lista.length === 0 && (
                <TableRow><TableCell colSpan={7} align="center" sx={{ py: 5, color: "text.secondary" }}>Nenhum questionário neste filtro.</TableCell></TableRow>
              )}
              {lista.map((q) => (
                <TableRow key={q.publicId} hover data-testid={`linha-${q.publicId}`}>
                  <TableCell>
                    <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap" useFlexGap>
                      <Typography sx={{ fontWeight: 600 }} color={q.municipio ? "text.primary" : "text.disabled"}>
                        {q.municipio ?? "(sem município)"}
                      </Typography>
                      {q.municipioRepetido && (
                        <Tooltip title="Outro questionário traz o mesmo município (ou escrito de forma parecida). Escolha qual usar.">
                          <Chip size="small" color="warning" label="Repetido" />
                        </Tooltip>
                      )}
                    </Stack>
                  </TableCell>
                  <TableCell>
                    <Chip size="small" color={q.status === "ENVIADO" ? "success" : "default"} label={q.status === "ENVIADO" ? "Enviado" : "Em preenchimento"} />
                  </TableCell>
                  <TableCell align="center">{q.blocosCompletos} de {q.totalBlocos}</TableCell>
                  <TableCell align="center">{q.pendencias === 0 ? "—" : q.pendencias}</TableCell>
                  <TableCell>{formatarDataHora(q.atualizadoEm)}</TableCell>
                  <TableCell>{formatarDataHora(q.enviadoEm) ?? "—"}</TableCell>
                  <TableCell align="right">
                    <Stack direction="row" spacing={1} justifyContent="flex-end">
                      <Button size="small" component={Link} href={`/admin/questionarios/${q.publicId}`} startIcon={<VisibilityIcon />}>Ver</Button>
                      <Button size="small" startIcon={<DownloadIcon />} disabled={baixando !== ""}
                        onClick={() => void baixar(`${q.publicId}-x`, `/admin/questionarios/${q.publicId}/excel`, "questionario.xlsx")}>
                        {baixando === `${q.publicId}-x` ? "Gerando…" : "Excel"}
                      </Button>
                      <Button size="small" startIcon={<PictureAsPdfIcon />} disabled={baixando !== ""}
                        onClick={() => void baixar(`${q.publicId}-p`, `/admin/questionarios/${q.publicId}/pdf`, "questionario.pdf")}>
                        {baixando === `${q.publicId}-p` ? "Gerando…" : "PDF"}
                      </Button>
                      <Button size="small" color="error" startIcon={<DeleteIcon />} disabled={baixando !== ""}
                        data-testid={`excluir-${q.publicId}`} onClick={() => setExcluindo(q)}>
                        Apagar
                      </Button>
                    </Stack>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      )}

      <Dialog open={excluindo !== null} onClose={() => !apagando && setExcluindo(null)}>
        <DialogTitle>Apagar questionário?</DialogTitle>
        <DialogContent>
          <DialogContentText>
            O questionário de {excluindo?.municipio ?? "(sem município)"} e todos os seus anexos serão apagados de forma definitiva.
            Esta ação não pode ser desfeita.
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setExcluindo(null)} disabled={apagando}>Cancelar</Button>
          <Button color="error" variant="contained" onClick={() => void confirmarExclusao()} disabled={apagando} data-testid="confirmar-exclusao">
            {apagando ? "Apagando…" : "Apagar"}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
