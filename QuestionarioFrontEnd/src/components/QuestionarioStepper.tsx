"use client";

import { BLOCOS, PASSO_REVISAO } from "@/lib/blocos";
import { assinarRascunhos, blocosComRascunho } from "@/lib/rascunhoLocal";
import type { BlocoSituacao } from "@/types";
import { Box, Step, StepButton, Stepper, Typography } from "@mui/material";
import { useRouter } from "next/navigation";
import { useEffect, useSyncExternalStore } from "react";

type Props = {
  uuid: string;
  /** 1..8 ou "revisao" */
  passoAtual: number | typeof PASSO_REVISAO;
  /** Situação dos blocos no servidor (null se ainda não carregou ou o servidor está fora do ar). */
  situacao: BlocoSituacao[] | null;
  /** Blocos com pendência no servidor (null: não foi possível saber). */
  incompletos: number[] | null;
};

type Estado = "nao-salvo" | "salvo" | "andamento" | "pendente" | "em-breve";

const ROTULOS: Record<Estado, string> = {
  "nao-salvo": "Não iniciado",
  salvo: "Completo",
  andamento: "Em andamento",
  pendente: "Alterações não salvas",
  "em-breve": "Em breve",
};
const CORES: Record<Estado, string> = {
  "nao-salvo": "text.secondary",
  salvo: "success.main",
  andamento: "text.secondary",
  pendente: "warning.dark",
  "em-breve": "text.disabled",
};

// O StepButton do MUI tem margem negativa (-24px -16px) para ampliar a área de clique; isso faz o conteúdo passar da
// caixa e criava barras de rolagem (horizontal e vertical) mesmo sobrando espaço. Sem a margem, só rola se faltar largura.
const BOTAO_SEM_MARGEM = { m: 0, px: 1, py: 1 } as const;

/** Os 8 blocos mais a revisão, com a situação de cada um: "Salvo", "Alterações não salvas" ou "Não iniciado". */
export default function QuestionarioStepper({ uuid, passoAtual, situacao, incompletos }: Props) {
  const router = useRouter();
  // Quais blocos têm rascunho local: muda ao digitar (nesta aba) e também quando outra aba do navegador grava.
  const pendentesTexto = useSyncExternalStore(
    assinarRascunhos,
    () => blocosComRascunho(uuid).join(","),
    () => "",
  );
  const pendentes = pendentesTexto ? pendentesTexto.split(",").map(Number) : [];

  const indiceAtual = passoAtual === PASSO_REVISAO ? BLOCOS.length : passoAtual - 1;

  useEffect(() => {
    document.getElementById(`passo-${passoAtual}`)?.scrollIntoView({ inline: "center", block: "nearest" });
  }, [passoAtual]);

  function estadoDoBloco(numero: number): Estado {
    if (pendentes.includes(numero)) return "pendente";
    if (situacao?.find((b) => b.numero === numero)?.version != null) {
      return (incompletos ?? []).includes(numero) ? "andamento" : "salvo";
    }
    return "nao-salvo";
  }

  return (
    // Telas grandes (lg para cima): os 9 passos cabem na largura e não há barra de rolagem. Abaixo disso o Stepper
    // mantém uma largura mínima e rola na horizontal (celular/tablet).
    <Box sx={{ overflowX: { xs: "auto", lg: "visible" }, overflowY: { xs: "hidden", lg: "visible" }, pb: { xs: 1, lg: 0 } }}>
      <Stepper nonLinear activeStep={indiceAtual} sx={{ minWidth: { xs: 980, lg: 0 } }} aria-label="Passos do questionário">
        {BLOCOS.map((bloco) => {
          const estado = estadoDoBloco(bloco.numero);
          return (
            <Step key={bloco.numero} completed={estado === "salvo"}>
              <StepButton
                id={`passo-${bloco.numero}`}
                sx={BOTAO_SEM_MARGEM}
                aria-label={`Bloco ${bloco.numero}: ${bloco.titulo}. ${ROTULOS[estado]}`}
                onClick={() => router.push(`/questionario/${uuid}/${bloco.numero}`)}
                optional={
                  <Typography variant="caption" sx={{ color: CORES[estado], fontWeight: estado === "pendente" ? 700 : 400 }}>
                    {ROTULOS[estado]}
                  </Typography>
                }
              >
                Bloco {bloco.numero}
              </StepButton>
            </Step>
          );
        })}
        <Step>
          <StepButton
            id={`passo-${PASSO_REVISAO}`}
            sx={BOTAO_SEM_MARGEM}
            aria-label="Revisão e envio"
            onClick={() => router.push(`/questionario/${uuid}/${PASSO_REVISAO}`)}
            optional={<Typography variant="caption" sx={{ color: CORES["nao-salvo"] }}>Conferir e enviar</Typography>}
          >
            Revisão e envio
          </StepButton>
        </Step>
      </Stepper>
    </Box>
  );
}
