"use client";

import { formatarDataHora } from "@/lib/formatters";
import { Alert, Box, Button, CircularProgress, Stack, Typography } from "@mui/material";
import Link from "next/link";

type Props = {
  uuid: string;
  numero: number;
  salvando: boolean;
  sujo: boolean;
  semArmazenamento: boolean;
  salvoEm: string | null;
  erro: string;
  onSalvar: () => void;
  onSalvarEAvancar: () => void;
};

/** Rodapé de cada bloco: situação do salvamento e os botões Anterior / Salvar / Salvar e avançar. */
export default function BarraDoBloco({ uuid, numero, salvando, sujo, semArmazenamento, salvoEm, erro, onSalvar, onSalvarEAvancar }: Props) {
  const anterior = numero > 1 ? `/questionario/${uuid}/${numero - 1}` : null;
  const ultimo = numero === 8;

  let situacao: string;
  if (salvando) situacao = "Salvando…";
  else if (sujo) {
    situacao = semArmazenamento
      ? "Alterações não salvas. Este navegador não deixou guardá-las: salve antes de sair."
      : "Alterações não salvas (guardadas neste navegador).";
  } else if (salvoEm) situacao = `Salvo em ${formatarDataHora(salvoEm)}`;
  else situacao = "Ainda não salvo";

  return (
    <Box sx={{ mt: 4 }}>
      {erro && <Alert severity="error" sx={{ mb: 2 }}>{erro}</Alert>}
      <Stack direction={{ xs: "column", sm: "row" }} spacing={1.5} alignItems={{ sm: "center" }} justifyContent="space-between">
        <Typography
          variant="body2"
          role="status"
          sx={{ color: sujo ? "warning.dark" : "text.secondary", fontWeight: sujo ? 600 : 400 }}
        >
          {situacao}
        </Typography>
        <Stack direction="row" spacing={1.5} flexWrap="wrap" useFlexGap>
          {anterior ? <Button component={Link} href={anterior}>Anterior</Button> : <Button disabled>Anterior</Button>}
          <Button
            variant="outlined"
            onClick={onSalvar}
            disabled={salvando}
            startIcon={salvando ? <CircularProgress size={16} /> : undefined}
          >
            Salvar
          </Button>
          <Button variant="contained" onClick={onSalvarEAvancar} disabled={salvando}>
            {ultimo ? "Salvar e ir para a revisão" : "Salvar e avançar"}
          </Button>
        </Stack>
      </Stack>
    </Box>
  );
}
