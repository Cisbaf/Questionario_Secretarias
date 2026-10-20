"use client";

import {
  EXTENSOES_PLANILHA, MAX_MB, URL_MODELO_PLANILHA, enviarAnexo, excluirAnexo, listarAnexos, mensagemDoErro, recusaLocal,
  tamanhoLegivel, urlDoAnexo,
} from "@/lib/anexos";
import type { Anexo } from "@/types";
import AttachFileIcon from "@mui/icons-material/AttachFile";
import DeleteOutlineIcon from "@mui/icons-material/DeleteOutline";
import DownloadIcon from "@mui/icons-material/Download";
import UploadFileIcon from "@mui/icons-material/UploadFile";
import { Alert, Box, Button, IconButton, Link, Paper, Stack, Typography } from "@mui/material";
import { useEffect, useRef, useState } from "react";

const ACCEPT = EXTENSOES_PLANILHA.map((e) => `.${e}`).join(",");

/**
 * Planilha de coleta do Bloco 1: a pessoa baixa o modelo, preenche e sobe de novo aqui. Um arquivo só (enviar outro
 * troca o anterior). O envio é imediato; com o questionário já enviado a área vira só consulta. Os links de download
 * são <a> (funcionam mesmo dentro do fieldset desabilitado do modo somente leitura).
 */
export default function PlanilhaColeta({ uuid, enviado }: { uuid: string; enviado: boolean }) {
  const entrada = useRef<HTMLInputElement>(null);
  const [atual, setAtual] = useState<Anexo | null>(null);
  const [carregou, setCarregou] = useState(false);
  const [enviando, setEnviando] = useState(false);
  const [removendo, setRemovendo] = useState(false);
  const [erro, setErro] = useState<string | null>(null);

  useEffect(() => {
    let ativo = true;
    listarAnexos(uuid)
      .then((l) => { if (ativo) setAtual(l.find((a) => a.documento === "PLANILHA_COLETA") ?? null); })
      .catch(() => { if (ativo) setErro("Não foi possível carregar a planilha já enviada. Recarregue a página."); })
      .finally(() => { if (ativo) setCarregou(true); });
    return () => { ativo = false; };
  }, [uuid]);

  async function escolher(lista: FileList | null) {
    const arquivo = lista?.[0];
    if (entrada.current) entrada.current.value = "";
    if (!arquivo) return;
    setErro(null);
    const recusa = recusaLocal(arquivo, EXTENSOES_PLANILHA);
    if (recusa) { setErro(recusa); return; }
    setEnviando(true);
    try {
      setAtual(await enviarAnexo(uuid, "PLANILHA_COLETA", arquivo));
    } catch (e) {
      setErro(mensagemDoErro(e));
    } finally {
      setEnviando(false);
    }
  }

  async function remover() {
    if (!atual) return;
    setErro(null); setRemovendo(true);
    try {
      await excluirAnexo(uuid, atual.id);
      setAtual(null);
    } catch (e) {
      setErro(mensagemDoErro(e));
    } finally {
      setRemovendo(false);
    }
  }

  return (
    <Paper variant="outlined" sx={{ p: { xs: 2, sm: 3 }, mt: 4 }} data-testid="planilha-coleta">
      <Typography variant="h6" component="h3" sx={{ fontWeight: 700, mb: 0.5 }}>Planilha de coleta (Excel)</Typography>
      <Typography variant="body2" sx={{ mb: 2 }}>
        Além deste questionário, o município precisa devolver uma planilha com os dados detalhados de filas, leitos,
        cirurgias, censo das UPAs e outros. Faça assim:
      </Typography>
      <Box component="ol" sx={{ m: 0, mb: 2, pl: 3, "& li": { mb: 0.5 } }}>
        <li><Typography variant="body2"><strong>Baixe</strong> a planilha modelo abaixo.</Typography></li>
        <li><Typography variant="body2"><strong>Preencha</strong> as questões em cada aba (as instruções estão na primeira aba). É uma planilha por município.</Typography></li>
        <li><Typography variant="body2"><strong>Suba de novo</strong> a planilha já preenchida, aqui mesmo, no botão “Enviar planilha preenchida”.</Typography></li>
      </Box>

      <Stack direction={{ xs: "column", sm: "row" }} spacing={1.5} alignItems={{ sm: "center" }}>
        <Button variant="outlined" startIcon={<DownloadIcon />} component="a" href={URL_MODELO_PLANILHA}
          download data-testid="baixar-modelo-planilha">
          Baixar planilha modelo
        </Button>
        {!enviado && (
          <>
            <input ref={entrada} type="file" hidden accept={ACCEPT} data-testid="arquivo-planilha-coleta"
              onChange={(e) => void escolher(e.target.files)} />
            <Button variant="contained" startIcon={<UploadFileIcon />} disabled={enviando || !carregou}
              onClick={() => entrada.current?.click()}>
              {enviando ? "Enviando…" : atual ? "Trocar planilha preenchida" : "Enviar planilha preenchida"}
            </Button>
          </>
        )}
      </Stack>

      <Typography variant="caption" color="text.secondary" component="p" sx={{ mt: 1 }}>
        Formatos: Excel (.xlsx ou .xls) ou OpenDocument (.ods), até {MAX_MB} MB. Só uma planilha: se enviar outra, ela substitui a anterior.
      </Typography>

      {carregou && (
        atual ? (
          <Alert severity="success" icon={<AttachFileIcon fontSize="inherit" />} sx={{ mt: 2 }} data-testid="planilha-enviada"
            action={!enviado && (
              <IconButton size="small" aria-label="Remover a planilha enviada" disabled={removendo || enviando} onClick={() => void remover()}>
                <DeleteOutlineIcon fontSize="small" />
              </IconButton>
            )}>
            Planilha enviada:{" "}
            <Link href={urlDoAnexo(uuid, atual.id)} download underline="hover" sx={{ wordBreak: "break-all" }}>{atual.nome}</Link>
            {" "}({tamanhoLegivel(atual.tamanho)})
          </Alert>
        ) : (
          <Alert severity={enviado ? "info" : "warning"} sx={{ mt: 2 }} data-testid="planilha-pendente">
            {enviado ? "Nenhuma planilha foi enviada." : "Ainda não há planilha preenchida enviada."}
          </Alert>
        )
      )}
      {erro && <Alert severity="error" sx={{ mt: 1 }} role="alert">{erro}</Alert>}
    </Paper>
  );
}
