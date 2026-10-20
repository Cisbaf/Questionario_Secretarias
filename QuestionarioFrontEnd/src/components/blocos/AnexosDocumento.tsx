"use client";

import {
  ACCEPT, FORMATOS_TEXTO, MAX_ARQUIVOS_POR_DOCUMENTO, MAX_MB, enviarAnexo, excluirAnexo, mensagemDoErro, recusaLocal,
  tamanhoLegivel, urlDoAnexo,
} from "@/lib/anexos";
import { rotuloDe, DOCUMENTOS_ANEXAVEIS, type DocumentoAnexavel } from "@/lib/rotulos";
import type { Anexo } from "@/types";
import AttachFileIcon from "@mui/icons-material/AttachFile";
import DeleteOutlineIcon from "@mui/icons-material/DeleteOutline";
import { Alert, Box, Button, IconButton, Link, Paper, Stack, Typography } from "@mui/material";
import { useRef, useState } from "react";

/**
 * Área de arquivos de UM documento marcado no 8.2: até {@link MAX_ARQUIVOS_POR_DOCUMENTO} arquivos. O envio é
 * imediato (não depende do botão Salvar do bloco). O link de download é um <a>, que continua funcionando com o
 * questionário já enviado (somente leitura).
 */
export default function AnexosDocumento({ uuid, documento, anexos, enviado, aoMudar }: {
  uuid: string; documento: DocumentoAnexavel; anexos: Anexo[]; enviado: boolean;
  aoMudar: (novos: Anexo[]) => void;
}) {
  const entrada = useRef<HTMLInputElement>(null);
  const [enviando, setEnviando] = useState(false);
  const [removendo, setRemovendo] = useState<number | null>(null);
  const [erro, setErro] = useState<string | null>(null);
  const meus = anexos.filter((a) => a.documento === documento);
  const vagas = MAX_ARQUIVOS_POR_DOCUMENTO - meus.length;

  async function escolher(lista: FileList | null) {
    const arquivos = Array.from(lista ?? []);
    if (entrada.current) entrada.current.value = "";
    if (arquivos.length === 0) return;
    setErro(null);
    if (arquivos.length > vagas) {
      setErro(`Só cabem mais ${vagas} arquivo(s) neste documento (máximo de ${MAX_ARQUIVOS_POR_DOCUMENTO}).`);
      return;
    }
    for (const a of arquivos) {
      const recusa = recusaLocal(a);
      if (recusa) { setErro(recusa); return; }
    }
    setEnviando(true);
    let atuais = anexos;
    try {
      for (const a of arquivos) {
        const novo = await enviarAnexo(uuid, documento, a);
        atuais = [...atuais, novo];
        aoMudar(atuais);
      }
    } catch (e) {
      setErro(mensagemDoErro(e));
    } finally {
      setEnviando(false);
    }
  }

  async function remover(a: Anexo) {
    setErro(null); setRemovendo(a.id);
    try {
      await excluirAnexo(uuid, a.id);
      aoMudar(anexos.filter((x) => x.id !== a.id));
    } catch (e) {
      setErro(mensagemDoErro(e));
    } finally {
      setRemovendo(null);
    }
  }

  return (
    <Paper variant="outlined" sx={{ p: 2, mb: 2 }} data-testid={`anexos-${documento}`}>
      <Typography variant="subtitle2" sx={{ fontWeight: 700 }}>{rotuloDe(DOCUMENTOS_ANEXAVEIS, documento)}</Typography>
      <Typography variant="caption" color="text.secondary" component="p" sx={{ mb: 1 }}>
        {meus.length} de {MAX_ARQUIVOS_POR_DOCUMENTO} arquivos · até {MAX_MB} MB cada · {FORMATOS_TEXTO}
      </Typography>

      {meus.length > 0 && (
        <Stack spacing={0.5} sx={{ mb: 1 }} component="ul" style={{ listStyle: "none", padding: 0, margin: 0 }}>
          {meus.map((a) => (
            <Box component="li" key={a.id} sx={{ display: "flex", alignItems: "center", gap: 1 }} data-testid="anexo-item">
              <AttachFileIcon fontSize="small" color="action" />
              <Link href={urlDoAnexo(uuid, a.id)} download underline="hover" sx={{ wordBreak: "break-all" }}>{a.nome}</Link>
              <Typography variant="caption" color="text.secondary" sx={{ whiteSpace: "nowrap" }}>{tamanhoLegivel(a.tamanho)}</Typography>
              {!enviado && (
                <IconButton size="small" aria-label={`Remover ${a.nome}`} disabled={removendo === a.id || enviando}
                  onClick={() => void remover(a)} sx={{ ml: "auto" }}>
                  <DeleteOutlineIcon fontSize="small" />
                </IconButton>
              )}
            </Box>
          ))}
        </Stack>
      )}

      {!enviado && (
        <>
          <input ref={entrada} type="file" hidden multiple accept={ACCEPT} data-testid={`arquivo-${documento}`}
            onChange={(e) => void escolher(e.target.files)} />
          <Button size="small" variant="outlined" startIcon={<AttachFileIcon />}
            disabled={vagas <= 0 || enviando} onClick={() => entrada.current?.click()}>
            {enviando ? "Enviando…" : vagas <= 0 ? "Limite de arquivos atingido" : "Anexar arquivo"}
          </Button>
        </>
      )}
      {enviado && meus.length === 0 && (
        <Typography variant="body2" color="text.secondary">Nenhum arquivo anexado.</Typography>
      )}
      {erro && <Alert severity="error" sx={{ mt: 1 }} role="alert">{erro}</Alert>}
    </Paper>
  );
}
