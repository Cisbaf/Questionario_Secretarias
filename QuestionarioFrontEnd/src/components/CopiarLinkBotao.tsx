"use client";

import { copiarTexto } from "@/lib/clipboard";
import CheckIcon from "@mui/icons-material/Check";
import ContentCopyIcon from "@mui/icons-material/ContentCopy";
import { Button } from "@mui/material";
import { useEffect, useRef, useState } from "react";

/** Botão compacto "Copiar link", sempre à mão no topo do questionário (D11). */
export default function CopiarLinkBotao({ uuid }: { uuid: string }) {
  const [copiado, setCopiado] = useState(false);
  const [falhou, setFalhou] = useState(false);
  const temporizador = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(() => () => {
    if (temporizador.current) clearTimeout(temporizador.current);
  }, []);

  async function copiar() {
    const ok = await copiarTexto(`${window.location.origin}/questionario/${uuid}`);
    setCopiado(ok);
    setFalhou(!ok);
    if (temporizador.current) clearTimeout(temporizador.current);
    temporizador.current = setTimeout(() => {
      setCopiado(false);
      setFalhou(false);
    }, 2500);
  }

  return (
    <Button
      variant="outlined"
      size="small"
      color={falhou ? "error" : "primary"}
      onClick={copiar}
      startIcon={copiado ? <CheckIcon /> : <ContentCopyIcon />}
    >
      {falhou ? "Não foi possível copiar" : copiado ? "Link copiado" : "Copiar link"}
    </Button>
  );
}
