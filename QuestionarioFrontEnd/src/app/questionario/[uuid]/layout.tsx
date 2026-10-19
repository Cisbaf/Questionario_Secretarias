"use client";

import Cabecalho from "@/components/Cabecalho";
import CopiarLinkBotao from "@/components/CopiarLinkBotao";
import { QuestionarioProvider } from "@/components/QuestionarioContext";
import QuestionarioStepper from "@/components/QuestionarioStepper";
import { getJson } from "@/lib/api";
import { interpretarPasso } from "@/lib/blocos";
import { mensagemDeErro, naoEncontrado } from "@/lib/erros";
import { formatarDataHora } from "@/lib/formatters";
import { definirQuestionarioAtual, lerQuestionarioAtual, limparQuestionarioAtual } from "@/lib/questionarioAtual";
import type { Questionario } from "@/types";
import { Alert, Box, Button, Chip, Container, Paper, Stack, Typography } from "@mui/material";
import Link from "next/link";
import { useParams } from "next/navigation";
import { type ReactNode, useCallback, useEffect, useMemo, useState } from "react";

type Estado =
  | { estado: "carregando" }
  | { estado: "ok"; questionario: Questionario }
  | { estado: "nao-encontrado" }
  | { estado: "erro"; mensagem: string };

/**
 * Moldura do questionário: carrega o questionário uma vez (situação dos blocos), mostra o Stepper e o botão "Copiar
 * link" em todos os passos. Se o servidor estiver fora do ar o passo ainda abre, para o rascunho local poder ser
 * recuperado (ver useBlocoQuestionario).
 */
export default function QuestionarioLayout({ children }: { children: ReactNode }) {
  const { uuid, passo } = useParams<{ uuid: string; passo?: string }>();
  const [estado, setEstado] = useState<Estado>({ estado: "carregando" });

  const carregar = useCallback(async () => {
    try {
      const questionario = await getJson<Questionario>(`/questionarios/${encodeURIComponent(uuid)}`);
      // Enviado não é mais "o que estou preenchendo": a página inicial não deve oferecer "Continuar".
      if (questionario.status === "ENVIADO") {
        if (lerQuestionarioAtual() === questionario.publicId) limparQuestionarioAtual();
      } else {
        definirQuestionarioAtual(questionario.publicId);
      }
      setEstado({ estado: "ok", questionario });
    } catch (error) {
      if (naoEncontrado(error)) {
        // Só esquece o "atual" se o servidor disse que ele não existe; falha de rede não apaga nada.
        if (lerQuestionarioAtual() === uuid) limparQuestionarioAtual();
        setEstado({ estado: "nao-encontrado" });
      } else {
        setEstado({ estado: "erro", mensagem: mensagemDeErro(error) });
      }
    }
  }, [uuid]);

  useEffect(() => {
    void carregar();
  }, [carregar]);

  const questionario = estado.estado === "ok" ? estado.questionario : null;
  const contexto = useMemo(
    () => ({ uuid, questionario, recarregarQuestionario: carregar }),
    [uuid, questionario, carregar],
  );
  const passoAtual = interpretarPasso(passo ?? "") ?? 1;

  return (
    <Box sx={{ minHeight: "100vh", pb: 8 }}>
      <Cabecalho />
      <Container maxWidth="lg" sx={{ pt: 3 }}>
        {estado.estado === "nao-encontrado" ? (
          <>
            <Button component={Link} href="/" size="small" sx={{ mb: 2 }}>← Voltar ao início</Button>
            <Alert severity="warning">
              Questionário não encontrado. Confira se o link foi copiado por inteiro ou peça o link a quem iniciou o
              questionário.
            </Alert>
          </>
        ) : (
          <QuestionarioProvider value={contexto}>
            <Stack direction="row" justifyContent="space-between" alignItems="center" flexWrap="wrap" useFlexGap spacing={1}>
              <Button component={Link} href="/" size="small">← Instruções</Button>
              <CopiarLinkBotao uuid={uuid} />
            </Stack>

            <Typography variant="h4" component="h1" sx={{ mt: 1, mb: 0.5 }}>Questionário em preenchimento</Typography>
            {questionario && (
              <Stack direction="row" spacing={1} alignItems="center" sx={{ mb: 2 }}>
                <Chip
                  size="small"
                  color={questionario.status === "ENVIADO" ? "success" : "default"}
                  label={questionario.status === "ENVIADO" ? "Enviado" : "Rascunho"}
                />
                <Typography variant="body2" color="text.secondary">
                  Iniciado em {formatarDataHora(questionario.criadoEm)}
                </Typography>
              </Stack>
            )}

            {/* Sem botão próprio: o "Tentar de novo" do bloco recarrega os dois (ver BlocoLayout). */}
            {estado.estado === "erro" && (
              <Alert severity="warning" sx={{ my: 2 }}>
                Não foi possível carregar a situação dos blocos. Os campos continuam disponíveis.
              </Alert>
            )}

            <Box sx={{ my: 3 }}>
              <QuestionarioStepper uuid={uuid} passoAtual={passoAtual} situacao={questionario?.blocos ?? null} />
            </Box>

            <Paper variant="outlined" sx={{ p: { xs: 2, sm: 4 } }}>{children}</Paper>
          </QuestionarioProvider>
        )}
      </Container>
    </Box>
  );
}
