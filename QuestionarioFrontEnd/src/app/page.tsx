"use client";

import Cabecalho from "@/components/Cabecalho";
import { CabecalhoDoQuestionario, InstrucoesDoQuestionario, QuadroDeDatas } from "@/components/CapaQuestionario";
import LinkQuestionario from "@/components/LinkQuestionario";
import { getJson, postJson } from "@/lib/api";
import { mensagemDeErro, naoEncontrado } from "@/lib/erros";
import { formatarDataHora } from "@/lib/formatters";
import { definirQuestionarioAtual, lerQuestionarioAtual, limparQuestionarioAtual } from "@/lib/questionarioAtual";
import type { Levantamento, Questionario } from "@/types";
import {
  Alert, Box, Button, CircularProgress, Container, Dialog, DialogActions, DialogContent, DialogContentText,
  DialogTitle, Divider, Paper, Stack, Typography,
} from "@mui/material";
import { useRouter } from "next/navigation";
import { useCallback, useEffect, useState } from "react";

type Capa =
  | { estado: "carregando" }
  | { estado: "ok"; levantamento: Levantamento }
  | { estado: "sem-levantamento"; mensagem: string }
  | { estado: "erro"; mensagem: string };

export default function Home() {
  const router = useRouter();
  const [capa, setCapa] = useState<Capa>({ estado: "carregando" });
  // Questionário que este navegador já estava preenchendo (confirmado no servidor).
  const [emAndamento, setEmAndamento] = useState<Questionario | null>(null);
  const [iniciando, setIniciando] = useState(false);
  const [erroInicio, setErroInicio] = useState("");
  const [criado, setCriado] = useState<Questionario | null>(null);
  const [confirmarNovo, setConfirmarNovo] = useState(false);

  const carregarCapa = useCallback(async () => {
    setCapa({ estado: "carregando" });
    try {
      setCapa({ estado: "ok", levantamento: await getJson<Levantamento>("/levantamentos/ativo") });
    } catch (error) {
      setCapa(naoEncontrado(error)
        ? { estado: "sem-levantamento", mensagem: mensagemDeErro(error) }
        : { estado: "erro", mensagem: mensagemDeErro(error) });
    }
  }, []);

  useEffect(() => {
    void carregarCapa();
  }, [carregarCapa]);

  useEffect(() => {
    const atual = lerQuestionarioAtual();
    if (!atual) return;
    let cancelado = false;
    getJson<Questionario>(`/questionarios/${atual}`)
      .then((questionario) => {
        if (!cancelado) setEmAndamento(questionario);
      })
      .catch((error) => {
        // Só esquece o questionário se o servidor disse que ele não existe; falha de rede não apaga nada.
        if (naoEncontrado(error)) limparQuestionarioAtual();
      });
    return () => {
      cancelado = true;
    };
  }, []);

  async function iniciar() {
    setConfirmarNovo(false);
    setIniciando(true);
    setErroInicio("");
    try {
      const questionario = await postJson<Questionario>("/questionarios");
      definirQuestionarioAtual(questionario.publicId);
      setCriado(questionario);
    } catch (error) {
      setErroInicio(mensagemDeErro(error));
    } finally {
      setIniciando(false);
    }
  }

  const podeIniciar = capa.estado === "ok";

  return (
    <Box sx={{ minHeight: "100vh", pb: 8 }}>
      <Cabecalho />
      <Container maxWidth="md" sx={{ pt: 5 }}>
        <CabecalhoDoQuestionario levantamento={capa.estado === "ok" ? capa.levantamento : null} />

        {capa.estado === "carregando" && (
          <Stack direction="row" spacing={2} alignItems="center" sx={{ mb: 3 }}>
            <CircularProgress size={20} />
            <Typography>Carregando…</Typography>
          </Stack>
        )}
        {capa.estado === "ok" && <QuadroDeDatas levantamento={capa.levantamento} />}
        {capa.estado === "sem-levantamento" && (
          <Alert severity="info" sx={{ mb: 3 }}>
            {capa.mensagem}. Volte quando a equipe do CISBAF abrir o questionário.
          </Alert>
        )}
        {capa.estado === "erro" && (
          <Alert severity="error" sx={{ mb: 3 }} action={<Button color="inherit" size="small" onClick={carregarCapa}>Tentar de novo</Button>}>
            {capa.mensagem}
          </Alert>
        )}

        <Paper variant="outlined" sx={{ p: 3, mb: 5 }}>
          {emAndamento ? (
            <>
              <Typography variant="h6" gutterBottom>Você tem um questionário em andamento</Typography>
              <Typography color="text.secondary" sx={{ mb: 2 }}>
                Iniciado neste navegador em {formatarDataHora(emAndamento.criadoEm)}.
              </Typography>
              <Stack direction={{ xs: "column", sm: "row" }} spacing={1.5}>
                <Button variant="contained" size="large" onClick={() => router.push(`/questionario/${emAndamento.publicId}`)}>
                  Continuar o questionário
                </Button>
                <Button variant="text" disabled={!podeIniciar || iniciando} onClick={() => setConfirmarNovo(true)}>
                  Iniciar um novo
                </Button>
              </Stack>
            </>
          ) : (
            <>
              <Typography variant="h6" gutterBottom>Pronto para começar?</Typography>
              <Typography color="text.secondary" sx={{ mb: 2 }}>
                Leia as instruções abaixo e inicie o questionário. Você receberá um link para voltar a ele depois.
              </Typography>
              <Button variant="contained" size="large" disabled={!podeIniciar || iniciando} onClick={iniciar}>
                {iniciando ? "Iniciando…" : "Iniciar questionário"}
              </Button>
            </>
          )}
          {erroInicio && <Alert severity="error" sx={{ mt: 2 }}>{erroInicio}</Alert>}
        </Paper>

        <Divider sx={{ mb: 4 }} />
        <InstrucoesDoQuestionario />
      </Container>

      <Dialog open={confirmarNovo} onClose={() => setConfirmarNovo(false)} maxWidth="xs" fullWidth>
        <DialogTitle>Iniciar um novo questionário?</DialogTitle>
        <DialogContent>
          <DialogContentText>
            O questionário em andamento não será apagado: o link dele continua funcionando. Mas este navegador passará a
            oferecer o novo. Se o seu município já tem um questionário iniciado, continue o existente para não duplicar.
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setConfirmarNovo(false)}>Cancelar</Button>
          <Button variant="contained" onClick={iniciar}>Iniciar um novo</Button>
        </DialogActions>
      </Dialog>

      <Dialog open={criado !== null} onClose={() => undefined} disableEscapeKeyDown maxWidth="sm" fullWidth>
        <DialogTitle>Questionário iniciado</DialogTitle>
        <DialogContent>
          <DialogContentText sx={{ mb: 2 }}>
            Guarde este link. É por ele que você, ou outra pessoa da sua equipe, volta a este questionário depois.
          </DialogContentText>
          {criado && <LinkQuestionario publicId={criado.publicId} />}
          <Alert severity="warning" sx={{ mt: 2 }}>
            Quem tiver o link consegue ver e alterar as respostas. Compartilhe apenas com quem vai preencher.
          </Alert>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button variant="contained" onClick={() => criado && router.push(`/questionario/${criado.publicId}`)}>
            Começar a preencher
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
