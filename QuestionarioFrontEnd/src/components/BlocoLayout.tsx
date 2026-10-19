"use client";

import BarraDoBloco from "@/components/BarraDoBloco";
import ConflitoVersaoDialog from "@/components/ConflitoVersaoDialog";
import RascunhoRecuperarDialog from "@/components/RascunhoRecuperarDialog";
import { useQuestionario } from "@/components/QuestionarioContext";
import type { useBlocoQuestionario } from "@/hooks/useBlocoQuestionario";
import { BLOCOS, PASSO_REVISAO } from "@/lib/blocos";
import type { RespostaDoBloco } from "@/types";
import { Alert, Box, Button, CircularProgress, Stack, Typography } from "@mui/material";
import Link from "next/link";
import { useRouter } from "next/navigation";
import type { ReactNode } from "react";

/**
 * O que a moldura precisa do motor. Fica de fora tudo que depende dos campos do bloco (alterar, os valores
 * tipados), por isso qualquer bloco, com qualquer conjunto de campos, encaixa aqui sem conversão.
 */
export type MotorDoBloco = Omit<
  ReturnType<typeof useBlocoQuestionario<object, RespostaDoBloco>>,
  "alterar" | "valores" | "decisaoRascunho"
> & {
  valores: object | null;
  decisaoRascunho: { savedLocallyAt: string } | null;
};

type Props = {
  numero: number;
  motor: MotorDoBloco;
  /** Os campos do bloco. Só são montados depois que o bloco carregou. */
  children: ReactNode;
};

/**
 * Moldura comum dos 8 blocos: título, carregando/erro, aviso de servidor fora do ar, diálogos de rascunho e de
 * conflito, e a barra Anterior / Salvar / Salvar e avançar. Cada bloco só fornece os seus campos.
 */
export default function BlocoLayout({ numero, motor, children }: Props) {
  const { uuid, questionario, recarregarQuestionario } = useQuestionario();
  const enviado = questionario?.status === "ENVIADO";
  const router = useRouter();
  const info = BLOCOS[numero - 1];

  /** Recarrega o bloco e, junto, a situação dos blocos do Stepper (o servidor pode ter voltado). */
  function tentarDeNovo() {
    void motor.recarregar();
    void recarregarQuestionario();
  }

  async function salvarEAvancar() {
    if (await motor.salvar()) {
      router.push(`/questionario/${uuid}/${numero === 8 ? PASSO_REVISAO : numero + 1}`);
    }
  }

  return (
    <Box>
      <Typography variant="h5" component="h2" sx={{ mb: 3 }}>
        Bloco {numero} — {info.titulo}
      </Typography>

      {motor.fase === "carregando" && (
        <Stack direction="row" spacing={2} alignItems="center">
          <CircularProgress size={20} />
          <Typography>Abrindo o bloco…</Typography>
        </Stack>
      )}

      {motor.fase === "nao-encontrado" && (
        <Alert severity="warning">Questionário não encontrado. Confira se o link foi copiado por inteiro.</Alert>
      )}

      {motor.fase === "erro" && (
        <Alert severity="error" action={<Button color="inherit" size="small" onClick={tentarDeNovo}>Tentar de novo</Button>}>
          {motor.erroCarga}
        </Alert>
      )}

      {motor.fase === "pronto" && motor.valores && (
        <>
          {motor.offline && (
            <Alert
              severity="warning"
              sx={{ mb: 3 }}
              action={<Button color="inherit" size="small" onClick={tentarDeNovo}>Tentar reconectar</Button>}
            >
              Sem conexão com o servidor. Você está vendo as alterações guardadas neste navegador; elas só serão
              salvas quando a conexão voltar.
            </Alert>
          )}

          {enviado && (
            <Alert severity="info" sx={{ mb: 3 }} data-testid="aviso-somente-leitura">
              Este questionário já foi enviado e não pode mais ser alterado. Você está apenas consultando.
            </Alert>
          )}

          <Box component="fieldset" disabled={enviado} sx={{ border: 0, p: 0, m: 0, minWidth: 0 }}>
            {children}
          </Box>

          {enviado ? (
            <Stack direction="row" spacing={1.5} justifyContent="space-between" sx={{ mt: 4 }}>
              <Button component={Link} href={numero > 1 ? `/questionario/${uuid}/${numero - 1}` : `/questionario/${uuid}/${PASSO_REVISAO}`}>
                {numero > 1 ? "Anterior" : "Ir à revisão"}
              </Button>
              <Button component={Link} href={`/questionario/${uuid}/${numero === 8 ? PASSO_REVISAO : numero + 1}`}>
                {numero === 8 ? "Ir à revisão" : "Próximo"}
              </Button>
            </Stack>
          ) : (
          <BarraDoBloco
            uuid={uuid}
            numero={numero}
            salvando={motor.salvando}
            sujo={motor.sujo}
            semArmazenamento={motor.semArmazenamento}
            salvoEm={motor.salvoEm}
            erro={motor.erroSalvar}
            onSalvar={() => void motor.salvar()}
            onSalvarEAvancar={() => void salvarEAvancar()}
          />
          )}
        </>
      )}

      {motor.decisaoRascunho && (
        <RascunhoRecuperarDialog
          guardadoEm={motor.decisaoRascunho.savedLocallyAt}
          desatualizado={motor.rascunhoDesatualizado}
          onRecuperar={motor.recuperarRascunho}
          onDescartar={motor.descartarRascunho}
        />
      )}

      {motor.conflito && (
        <ConflitoVersaoDialog
          onRecarregar={() => void motor.recarregarDoServidor()}
          onSobrescrever={() => void motor.sobrescrever()}
          onDecidirDepois={motor.dispensarConflito}
        />
      )}
    </Box>
  );
}
