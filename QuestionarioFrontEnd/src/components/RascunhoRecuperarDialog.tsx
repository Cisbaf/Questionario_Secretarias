"use client";

import { formatarDataHora } from "@/lib/formatters";
import { Alert, Button, Dialog, DialogActions, DialogContent, DialogContentText, DialogTitle } from "@mui/material";

type Props = {
  /** Quando as alterações foram guardadas neste navegador ("AAAA-MM-DDTHH:mm:ss"). */
  guardadoEm: string;
  /** O servidor já tem uma versão mais nova que a do rascunho (outra pessoa salvou depois). */
  desatualizado: boolean;
  onRecuperar: () => void;
  onDescartar: () => void;
};

/** Aparece ao abrir um bloco que tem rascunho no localStorage. Só fecha com uma das duas escolhas. */
export default function RascunhoRecuperarDialog({ guardadoEm, desatualizado, onRecuperar, onDescartar }: Props) {
  const quando = formatarDataHora(guardadoEm);
  return (
    <Dialog open maxWidth="xs" fullWidth onClose={() => undefined} disableEscapeKeyDown>
      <DialogTitle>Há alterações não enviadas</DialogTitle>
      <DialogContent>
        <DialogContentText sx={{ mb: desatualizado ? 2 : 0 }}>
          Este bloco tem alterações feitas {quando ? `em ${quando}` : "antes"} que ainda não foram salvas no servidor.
          Deseja recuperá-las ou descartá-las?
        </DialogContentText>
        {desatualizado && (
          <Alert severity="warning">
            Outra pessoa salvou este bloco depois dessas alterações. Se você recuperar e salvar, o sistema vai pedir para
            escolher entre carregar a versão do servidor e sobrescrevê-la.
          </Alert>
        )}
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2 }}>
        <Button onClick={onDescartar}>Descartar</Button>
        <Button variant="contained" onClick={onRecuperar}>Recuperar</Button>
      </DialogActions>
    </Dialog>
  );
}
