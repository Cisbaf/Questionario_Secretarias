"use client";

import { Button, Dialog, DialogActions, DialogContent, DialogContentText, DialogTitle, Stack, Typography } from "@mui/material";

type Props = {
  onRecarregar: () => void;
  onSobrescrever: () => void;
  onDecidirDepois: () => void;
};

/** 409 de versão (D6): outra pessoa, ou outra aba, salvou este bloco depois. As alterações locais continuam guardadas. */
export default function ConflitoVersaoDialog({ onRecarregar, onSobrescrever, onDecidirDepois }: Props) {
  return (
    <Dialog open maxWidth="sm" fullWidth onClose={onDecidirDepois}>
      <DialogTitle>Este bloco foi alterado por outra pessoa</DialogTitle>
      <DialogContent>
        <DialogContentText sx={{ mb: 2 }}>
          Alguém salvou este bloco depois de você abri-lo. O que você digitou continua guardado neste navegador. Escolha
          o que fazer:
        </DialogContentText>
        <Stack spacing={1.5}>
          <Typography variant="body2">
            <strong>Carregar a versão do servidor:</strong> mostra o que a outra pessoa salvou e descarta as suas
            alterações.
          </Typography>
          <Typography variant="body2">
            <strong>Sobrescrever:</strong> grava as suas alterações por cima do que está no servidor.
          </Typography>
        </Stack>
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2, flexWrap: "wrap", gap: 1 }}>
        <Button onClick={onDecidirDepois}>Decidir depois</Button>
        <Button variant="outlined" onClick={onRecarregar}>Carregar a versão do servidor</Button>
        <Button variant="contained" color="warning" onClick={onSobrescrever}>Sobrescrever</Button>
      </DialogActions>
    </Dialog>
  );
}
