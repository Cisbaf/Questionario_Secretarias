"use client";

import { useQuestionario } from "@/components/QuestionarioContext";
import { ApiError, getJson, postJson } from "@/lib/api";
import { mensagemDeErro } from "@/lib/erros";
import { formatarDataHora } from "@/lib/formatters";
import { lerRascunho } from "@/lib/rascunhoLocal";
import { limparQuestionarioAtual } from "@/lib/questionarioAtual";
import { resumirRespostas } from "@/lib/resumo";
import { BLOCOS } from "@/lib/blocos";
import type { Pendencia, Questionario, Respostas } from "@/types";
import { Alert, Box, Button, Card, CircularProgress, Stack, Typography } from "@mui/material";
import Link from "next/link";
import { useCallback, useEffect, useState } from "react";

/** Passo final: resumo do que foi respondido, o que ainda falta e o botão que envia de vez. */
export default function RevisaoEnvio() {
  const { uuid, questionario, recarregarQuestionario } = useQuestionario();
  const [respostas, setRespostas] = useState<Respostas | null>(null);
  const [pendencias, setPendencias] = useState<Pendencia[]>([]);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState("");
  const [enviando, setEnviando] = useState(false);
  const [rascunhosLocais, setRascunhosLocais] = useState<number[]>([]);
  const [enviadoAgora, setEnviadoAgora] = useState<Questionario | null>(null);

  const base = `/questionarios/${encodeURIComponent(uuid)}`;

  const carregar = useCallback(async () => {
    setCarregando(true);
    setErro("");
    try {
      const [r, p] = await Promise.all([getJson<Respostas>(`${base}/respostas`), getJson<Pendencia[]>(`${base}/pendencias`)]);
      setRespostas(r);
      setPendencias(p);
      setRascunhosLocais(BLOCOS.map((b) => b.numero).filter((n) => lerRascunho(uuid, n) !== null));
    } catch (e) {
      setErro(mensagemDeErro(e));
    } finally {
      setCarregando(false);
    }
  }, [base, uuid]);

  useEffect(() => { void carregar(); }, [carregar]);

  async function enviar() {
    setEnviando(true);
    setErro("");
    try {
      const resposta = await postJson<Questionario>(`${base}/enviar`);
      limparQuestionarioAtual();
      setEnviadoAgora(resposta);
      await recarregarQuestionario();
      await carregar();
    } catch (e) {
      if (e instanceof ApiError && e.status === 409) {
        setErro(e.message);
        await carregar(); // a lista de pendências pode ter mudado
      } else {
        setErro(mensagemDeErro(e));
      }
    } finally {
      setEnviando(false);
    }
  }

  const enviado = (questionario?.status ?? respostas?.status) === "ENVIADO";
  const enviadoEm = enviadoAgora?.enviadoEm ?? questionario?.enviadoEm ?? respostas?.enviadoEm ?? null;
  const bloqueado = pendencias.length > 0 || rascunhosLocais.length > 0;

  return (
    <Box>
      <Typography variant="h5" component="h2" sx={{ mb: 3 }}>Revisão e envio</Typography>

      {carregando && !respostas && (
        <Stack direction="row" spacing={2} alignItems="center"><CircularProgress size={20} /><Typography>Reunindo as respostas…</Typography></Stack>
      )}
      {erro && <Alert severity="error" sx={{ mb: 3 }}>{erro}</Alert>}

      {enviado && (
        <Alert severity="success" sx={{ mb: 3 }} data-testid="aviso-enviado">
          Questionário enviado{enviadoEm ? ` em ${formatarDataHora(enviadoEm)}` : ""}. Obrigado! Ele não pode mais ser alterado.
          Guarde o link desta página caso queira consultá-lo.
        </Alert>
      )}

      {respostas && (
        <Stack spacing={2} sx={{ mb: 4 }}>
          {resumirRespostas(respostas).map((b) => (
            <Card key={b.numero} variant="outlined" sx={{ p: 2 }}>
              <Stack direction="row" justifyContent="space-between" alignItems="flex-start" spacing={2}>
                <Typography variant="subtitle1" component="h3">Bloco {b.numero} — {b.titulo}</Typography>
                <Button component={Link} href={`/questionario/${uuid}/${b.numero}`} size="small">{enviado ? "Ver" : "Editar"}</Button>
              </Stack>
              <ul style={{ margin: "4px 0 0", paddingLeft: 20 }}>
                {b.linhas.map((l) => <li key={l}><Typography variant="body2">{l}</Typography></li>)}
              </ul>
            </Card>
          ))}
        </Stack>
      )}

      {!enviado && respostas && (
        <>
          {rascunhosLocais.length > 0 && (
            <Alert severity="warning" sx={{ mb: 2 }} data-testid="aviso-rascunhos">
              Há alterações guardadas só neste navegador, ainda não salvas, no{rascunhosLocais.length > 1 ? "s" : ""} bloco
              {rascunhosLocais.length > 1 ? "s" : ""}{" "}
              {rascunhosLocais.map((n, i) => (
                <span key={n}>{i > 0 ? ", " : ""}<Link href={`/questionario/${uuid}/${n}`}>{n}</Link></span>
              ))}
              . Abra e salve antes de enviar.
            </Alert>
          )}

          {pendencias.length > 0 ? (
            <Alert severity="warning" sx={{ mb: 3 }} data-testid="lista-pendencias">
              <Typography variant="body2" sx={{ fontWeight: 600, mb: 0.5 }}>
                Falta resolver {pendencias.length === 1 ? "1 ponto" : `${pendencias.length} pontos`} para poder enviar:
              </Typography>
              <ul style={{ margin: 0, paddingLeft: 20 }}>
                {pendencias.map((p) => (
                  <li key={`${p.bloco}-${p.campo}`}>
                    <Link href={`/questionario/${uuid}/${p.bloco}`}>Bloco {p.bloco}</Link>: {p.mensagem}
                    {p.tipo === "COERENCIA" ? " (números que não batem)" : ""}
                  </li>
                ))}
              </ul>
            </Alert>
          ) : (
            rascunhosLocais.length === 0 && <Alert severity="success" sx={{ mb: 3 }}>Tudo preenchido. Você já pode enviar.</Alert>
          )}

          <Stack direction="row" spacing={1.5} justifyContent="space-between" alignItems="center">
            <Button component={Link} href={`/questionario/${uuid}/8`}>Anterior</Button>
            <Button variant="contained" onClick={() => void enviar()} disabled={bloqueado || enviando}
              startIcon={enviando ? <CircularProgress size={16} /> : undefined}>
              Enviar questionário
            </Button>
          </Stack>
          <Typography variant="body2" color="text.secondary" sx={{ mt: 1.5 }}>
            Depois de enviado, o questionário não pode mais ser alterado.
          </Typography>
        </>
      )}
    </Box>
  );
}
