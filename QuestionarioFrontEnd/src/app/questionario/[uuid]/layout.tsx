"use client";

import Cabecalho from "@/components/Cabecalho";
import CopiarLinkBotao from "@/components/CopiarLinkBotao";
import { QuestionarioProvider } from "@/components/QuestionarioContext";
import QuestionarioMenu from "@/components/QuestionarioMenu";
import QuestionarioStepper from "@/components/QuestionarioStepper";
import { getJson } from "@/lib/api";
import { interpretarPasso } from "@/lib/blocos";
import { mensagemDeErro, naoEncontrado } from "@/lib/erros";
import { formatarDataHora } from "@/lib/formatters";
import { definirQuestionarioAtual, lerQuestionarioAtual, limparQuestionarioAtual } from "@/lib/questionarioAtual";
import type { Pendencia, Questionario } from "@/types";
import { Alert, Box, Button, Chip, Container, Paper, Stack, Typography } from "@mui/material";
import Link from "next/link";
import { useParams } from "next/navigation";
import { type ReactNode, useCallback, useEffect, useMemo, useState } from "react";

type Estado =
  | { estado: "carregando" }
  | { estado: "ok"; questionario: Questionario; incompletos: number[] | null }
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
      // Blocos com pendência (obrigatório faltando ou número incoerente): salvos, mas ainda não "feitos".
      let incompletos: number[] | null = null;
      try {
        const pendencias = await getJson<Pendencia[]>(`/questionarios/${encodeURIComponent(uuid)}/pendencias`);
        incompletos = [...new Set(pendencias.map((p) => p.bloco))];
      } catch {
        // sem a lista, o menu só mostra "salvo"
      }
      setEstado({ estado: "ok", questionario, incompletos });
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
  const incompletos = estado.estado === "ok" ? estado.incompletos : null;
  const contexto = useMemo(
    () => ({ uuid, questionario, recarregarQuestionario: carregar }),
    [uuid, questionario, carregar],
  );
  const passoAtual = interpretarPasso(passo ?? "") ?? 1;

  const enviado = questionario?.status === "ENVIADO";
  const ehPassoNumerico = typeof passoAtual === "number";

  if (estado.estado === "nao-encontrado") {
    return (
      <Box sx={{ minHeight: "100vh", pb: 8 }}>
        <Cabecalho />
        <Container maxWidth="lg" sx={{ pt: 3 }}>
          <Button component={Link} href="/" size="small" sx={{ mb: 2 }}>← Voltar ao início</Button>
          <Alert severity="warning">
            Questionário não encontrado. Confira se o link foi copiado por inteiro ou peça o link a quem iniciou o
            questionário.
          </Alert>
        </Container>
      </Box>
    );
  }

  return (
    <QuestionarioProvider value={contexto}>
      <Box sx={{ minHeight: "100vh", display: { lg: "grid" }, gridTemplateColumns: { lg: "300px minmax(0, 1fr)" } }}>
        {/* Telas grandes: menu lateral fixo. */}
        <Box component="aside" sx={{ display: { xs: "none", lg: "block" }, position: "sticky", top: 0, height: "100vh" }}>
          <QuestionarioMenu uuid={uuid} passoAtual={passoAtual} situacao={questionario?.blocos ?? null} incompletos={incompletos} enviado={enviado} />
        </Box>

        <Box component="main" sx={{ minWidth: 0, pb: 8 }}>
          {/* Telas menores: cabeçalho e passos no alto, como antes. */}
          <Box sx={{ display: { xs: "block", lg: "none" } }}>
            <Cabecalho />
            <Container maxWidth="md" sx={{ pt: 2 }}>
              <QuestionarioStepper uuid={uuid} passoAtual={passoAtual} situacao={questionario?.blocos ?? null} incompletos={incompletos} />
            </Container>
          </Box>

          <Box sx={{ maxWidth: 1040, mx: "auto", px: { xs: 2, sm: 3, lg: 6 }, pt: { xs: 2, lg: 4 } }}>
            <Stack direction="row" justifyContent="space-between" alignItems="center" flexWrap="wrap" useFlexGap spacing={1.5}
              sx={{ borderBottom: 1, borderColor: "divider", pb: 2, mb: 4 }}>
              <Typography sx={{ fontSize: 11, fontWeight: 700, letterSpacing: "0.14em", color: "text.secondary" }}>
                QUESTIONÁRIO ÀS SECRETARIAS DE SAÚDE
              </Typography>
              <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap" useFlexGap>
                <Button component={Link} href="/" size="small" color="inherit">Instruções</Button>
                <CopiarLinkBotao uuid={uuid} />
                {ehPassoNumerico && !enviado && (
                  <Button variant="outlined" size="small" color="inherit" sx={{ borderColor: "divider" }}
                    onClick={() => window.dispatchEvent(new Event("qsms:salvar"))}>
                    Salvar rascunho
                  </Button>
                )}
              </Stack>
            </Stack>

            {passoAtual === 1 && (
              <Box sx={{ mb: 4 }}>
                <Typography sx={{ fontSize: 11, fontWeight: 700, letterSpacing: "0.14em", color: "primary.main", mb: 1.5 }}>
                  ESCUTA MUNICIPAL · 8 BLOCOS
                </Typography>
                <Typography variant="h3" component="h1"
                  sx={{ fontWeight: 800, letterSpacing: "-0.035em", lineHeight: 1.05, fontSize: { xs: 32, md: 46 }, maxWidth: 640 }}>
                  Planejar a rede começa por ouvir o município.
                </Typography>
                <Typography color="text.secondary" sx={{ mt: 2, maxWidth: 560 }}>
                  Prioridades de atenção à saúde, filas de regulação e pacientes sem vaga nas portas de urgência.
                </Typography>
                <Box sx={{ mt: 3, px: 2.5, py: 1.75, borderRadius: 2, bgcolor: "#EAF3E4", border: "1px solid #D3E5C8", color: "#1D4A3A", fontSize: 14 }}>
                  <strong>Somente dados agregados.</strong> Não informe nome, CPF, CNS, prontuário ou endereço de pacientes.
                  Quando não tiver um dado, marque “Não disponível”.
                </Box>
                <Stack direction="row" justifyContent="space-between" alignItems="center" flexWrap="wrap" useFlexGap spacing={1} sx={{ mt: 1.5 }}>
                  <Typography variant="caption" color="text.secondary">
                    Cada bloco é salvo ao clicar em Salvar; o que você digita também fica guardado neste navegador.
                  </Typography>
                  {questionario && (
                    <Stack direction="row" spacing={1} alignItems="center">
                      <Chip size="small" color={enviado ? "success" : "default"} label={enviado ? "Enviado" : "Rascunho"} />
                      <Typography variant="caption" color="text.secondary">
                        Iniciado em {formatarDataHora(questionario.criadoEm)}
                      </Typography>
                    </Stack>
                  )}
                </Stack>
              </Box>
            )}

            {/* Sem botão próprio: o "Tentar de novo" do bloco recarrega os dois (ver BlocoLayout). */}
            {estado.estado === "erro" && (
              <Alert severity="warning" sx={{ mb: 3 }}>
                Não foi possível carregar a situação dos blocos. Os campos continuam disponíveis.
              </Alert>
            )}

            <Paper variant="outlined" sx={{ p: { xs: 2.5, sm: 4 }, borderRadius: 1.5, borderColor: "#E3E1D8" }}>{children}</Paper>
          </Box>
        </Box>
      </Box>
    </QuestionarioProvider>
  );
}
