"use client";

import { copiarTexto } from "@/lib/clipboard";
import CheckIcon from "@mui/icons-material/Check";
import ContentCopyIcon from "@mui/icons-material/ContentCopy";
import { Box, Button, FormHelperText, TextField } from "@mui/material";
import { useEffect, useRef, useState } from "react";

/** Mostra o link do questionário (montado com o endereço do site) e o botão "Copiar link". */
export default function LinkQuestionario({ publicId }: { publicId: string }) {
  const [link, setLink] = useState("");
  const [copiado, setCopiado] = useState(false);
  const [falhou, setFalhou] = useState(false);
  const temporizador = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(() => {
    setLink(`${window.location.origin}/questionario/${publicId}`);
  }, [publicId]);

  useEffect(() => () => {
    if (temporizador.current) clearTimeout(temporizador.current);
  }, []);

  async function copiar() {
    const ok = await copiarTexto(link);
    setCopiado(ok);
    setFalhou(!ok);
    if (temporizador.current) clearTimeout(temporizador.current);
    temporizador.current = setTimeout(() => setCopiado(false), 2500);
  }

  return (
    <Box>
      <Box sx={{ display: "flex", gap: 1, alignItems: "stretch", flexDirection: { xs: "column", sm: "row" } }}>
        <TextField
          fullWidth
          size="small"
          label="Link do questionário"
          value={link}
          slotProps={{ input: { readOnly: true } }}
          onFocus={(event) => event.target.select()}
        />
        <Button
          variant="contained"
          onClick={copiar}
          disabled={!link}
          startIcon={copiado ? <CheckIcon /> : <ContentCopyIcon />}
          sx={{ flexShrink: 0, minWidth: 160 }}
        >
          {copiado ? "Link copiado" : "Copiar link"}
        </Button>
      </Box>
      <Box aria-live="polite">
        {falhou && (
          <FormHelperText error>
            Não foi possível copiar automaticamente. Selecione o link acima e copie com Ctrl+C.
          </FormHelperText>
        )}
      </Box>
    </Box>
  );
}
