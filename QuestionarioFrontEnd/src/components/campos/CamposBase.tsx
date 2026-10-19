"use client";

import { campoVisivel, filtrarDigitacao, type CampoSpec, type Linha } from "@/lib/tabelas";
import {
  Alert, Box, Button, Card, Checkbox, FormControl, FormControlLabel, FormGroup, FormHelperText, FormLabel, IconButton,
  MenuItem, Radio, RadioGroup, Stack, TextField, Typography,
} from "@mui/material";
import DeleteOutlineIcon from "@mui/icons-material/DeleteOutline";
import AddIcon from "@mui/icons-material/Add";
import type { ReactNode } from "react";

export function Secao({ id, titulo, ajuda, children }: { id: string; titulo: string; ajuda?: string; children: ReactNode }) {
  return (
    <Box component="section" aria-labelledby={id} sx={{ mb: 5 }}>
      <Typography id={id} variant="h6" component="h3" sx={{ mb: 0.5 }}>{titulo}</Typography>
      {ajuda && <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>{ajuda}</Typography>}
      {children}
    </Box>
  );
}

type Opcao = { valor: string; rotulo: string };

/** Uma escolha entre opções (Sim/Não/Parcial, tipo de fila…). */
export function Escolha({ rotulo, opcoes, valor, onChange, erro, desabilitado }: {
  rotulo: string; opcoes: readonly Opcao[]; valor: string; onChange: (v: string) => void; erro?: string; desabilitado?: boolean;
}) {
  return (
    <FormControl error={!!erro} disabled={desabilitado} sx={{ mb: 2, display: "block" }}>
      <FormLabel>{rotulo}</FormLabel>
      <RadioGroup row value={valor} onChange={(e) => onChange(e.target.value)}>
        {opcoes.map((o) => <FormControlLabel key={o.valor} value={o.valor} control={<Radio />} label={o.rotulo} />)}
      </RadioGroup>
      {erro && <FormHelperText>{erro}</FormHelperText>}
    </FormControl>
  );
}

/** Várias opções marcáveis, mantidas na ordem do documento. */
export function Marcaveis<T extends string>({ rotulo, opcoes, valor, onChange, erro, desabilitado }: {
  rotulo: string; opcoes: readonly { valor: T; rotulo: string }[]; valor: T[]; onChange: (v: T[]) => void;
  erro?: string; desabilitado?: boolean;
}) {
  return (
    <FormControl error={!!erro} disabled={desabilitado} sx={{ mb: 2, display: "block" }}>
      <FormLabel>{rotulo}</FormLabel>
      <FormGroup>
        {opcoes.map((o) => (
          <FormControlLabel
            key={o.valor}
            label={o.rotulo}
            control={
              <Checkbox
                checked={valor.includes(o.valor)}
                onChange={(e) =>
                  onChange(opcoes.map((x) => x.valor).filter((v) => (v === o.valor ? e.target.checked : valor.includes(v))))}
              />
            }
          />
        ))}
      </FormGroup>
      {erro && <FormHelperText>{erro}</FormHelperText>}
    </FormControl>
  );
}

export function Texto({ rotulo, valor, onChange, erro, max, multiline, desabilitado, tipo, ajuda, minimo }: {
  rotulo: string; valor: string; onChange: (v: string) => void; erro?: string; max?: number; multiline?: boolean;
  desabilitado?: boolean; tipo?: "date" | "month"; ajuda?: string; minimo?: number;
}) {
  return (
    <TextField
      label={rotulo}
      value={valor}
      onChange={(e) => onChange(e.target.value)}
      error={!!erro}
      helperText={erro ?? ajuda}
      disabled={desabilitado}
      fullWidth
      multiline={multiline}
      minRows={multiline ? (minimo ?? 4) : undefined}
      type={tipo}
      slotProps={{
        htmlInput: max ? { maxLength: max + 50 } : undefined,
        inputLabel: tipo ? { shrink: true } : undefined,
      }}
      sx={{ mb: 2 }}
    />
  );
}

/** Marca "não disponível / não tenho este dado": desliga e limpa a tabela ao lado. */
export function NaoDisponivel({ marcado, onChange, rotulo = "Não disponível" }: {
  marcado: boolean; onChange: (v: boolean) => void; rotulo?: string;
}) {
  return (
    <FormControlLabel
      control={<Checkbox checked={marcado} onChange={(e) => onChange(e.target.checked)} />}
      label={rotulo}
    />
  );
}

function CampoDaLinha({ c, linha, onChange, erro, desabilitado }: {
  c: CampoSpec; linha: Linha; onChange: (nome: string, v: string) => void; erro?: string; desabilitado?: boolean;
}) {
  const comum = {
    label: c.rotulo, value: linha[c.nome] ?? "", error: !!erro, helperText: erro, disabled: desabilitado, fullWidth: true,
  };
  if (c.tipo === "opcao") {
    return (
      <TextField select {...comum} onChange={(e) => onChange(c.nome, e.target.value)}>
        <MenuItem value=""><em>Escolha…</em></MenuItem>
        {c.opcoes?.map((o) => <MenuItem key={o.valor} value={o.valor}>{o.rotulo}</MenuItem>)}
      </TextField>
    );
  }
  if (c.tipo === "data") {
    return (
      <TextField {...comum} type="date" slotProps={{ inputLabel: { shrink: true } }}
        onChange={(e) => onChange(c.nome, e.target.value)} />
    );
  }
  const numerico = c.tipo === "inteiro" || c.tipo === "decimal" || c.tipo === "dinheiro" || c.tipo === "sigtap" || c.tipo === "cnes";
  return (
    <TextField
      {...comum}
      onChange={(e) => onChange(c.nome, filtrarDigitacao(c, e.target.value))}
      slotProps={{ htmlInput: { inputMode: numerico ? (c.tipo === "inteiro" || c.tipo === "sigtap" || c.tipo === "cnes" ? "numeric" : "decimal") : "text" } }}
    />
  );
}

const GRADE = { display: "grid", gap: 2, gridTemplateColumns: { xs: "1fr", sm: "1fr 1fr", lg: "1fr 1fr 1fr" } } as const;

function CamposDaLinha({ specs, linha, chave, prefixo, erros, onChange, desabilitado }: {
  specs: readonly CampoSpec[]; linha: Linha; chave?: string; prefixo: string; erros: Record<string, string>;
  onChange: (nome: string, v: string) => void; desabilitado?: boolean;
}) {
  return (
    <Box sx={GRADE}>
      {specs.filter((c) => campoVisivel(c, linha, chave)).map((c) => (
        <Box key={c.nome} sx={c.largo ? { gridColumn: { sm: "1 / -1" } } : undefined}>
          <CampoDaLinha c={c} linha={linha} onChange={onChange} erro={erros[`${prefixo}.${c.nome}`]} desabilitado={desabilitado} />
        </Box>
      ))}
    </Box>
  );
}

/** Tabela de tamanho livre (até {@code limite} linhas). Cada linha é um cartão, para nunca precisar de rolagem lateral. */
export function TabelaDinamica({ nome, titulo, specs, linhas, onChange, limite, erros, desabilitado, avisos }: {
  nome: string; titulo: string; specs: readonly CampoSpec[]; linhas: Linha[]; onChange: (l: Linha[]) => void;
  limite: number; erros: Record<string, string>; desabilitado?: boolean; avisos?: string[];
}) {
  const mudar = (i: number, campo: string, valor: string) =>
    onChange(linhas.map((l, j) => (j === i ? { ...l, [campo]: valor } : l)));
  const vazia = Object.fromEntries(specs.map((c) => [c.nome, ""]));
  return (
    <Stack spacing={2}>
      {erros[nome] && <Alert severity="error">{erros[nome]}</Alert>}
      {linhas.map((linha, i) => (
        <Card key={i} variant="outlined" sx={{ p: 2, opacity: desabilitado ? 0.6 : 1 }} data-testid={`${nome}-${i}`}>
          <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 1.5 }}>
            <Typography variant="subtitle2" component="h4">{titulo} {i + 1}</Typography>
            <IconButton
              aria-label={`Remover ${titulo.toLowerCase()} ${i + 1}`}
              disabled={desabilitado}
              onClick={() => onChange(linhas.length === 1 ? [{ ...vazia }] : linhas.filter((_, j) => j !== i))}
            >
              <DeleteOutlineIcon />
            </IconButton>
          </Stack>
          <CamposDaLinha specs={specs} linha={linha} prefixo={`${nome}[${i}]`} erros={erros}
            onChange={(c, v) => mudar(i, c, v)} desabilitado={desabilitado} />
        </Card>
      ))}
      {avisos && avisos.length > 0 && (
        <Alert severity="warning">
          <Typography variant="body2" sx={{ fontWeight: 600 }}>Confira estes números (não impede de salvar, mas impede de enviar):</Typography>
          <ul style={{ margin: "4px 0 0", paddingLeft: 20 }}>{avisos.map((a) => <li key={a}>{a}</li>)}</ul>
        </Alert>
      )}
      <Box>
        <Button startIcon={<AddIcon />} disabled={desabilitado || linhas.length >= limite}
          onClick={() => onChange([...linhas, { ...vazia }])}>
          Adicionar {titulo.toLowerCase()}
        </Button>
        {linhas.length >= limite && <FormHelperText>Limite de {limite} linhas.</FormHelperText>}
      </Box>
    </Stack>
  );
}

export type LinhaFixaInfo = { chave: string; titulo: string; subtitulo?: string };

/** Tabela de linhas fixas (uma por tipo de leito, etc.), cada uma com o seu "Não disponível". */
export function TabelaFixa({ nome, specs, infos, linhas, naoDisp, onChange, onNaoDisp, erros, desabilitado, avisos, extra }: {
  nome: string; specs: readonly CampoSpec[]; infos: readonly LinhaFixaInfo[]; linhas: Linha[]; naoDisp?: boolean[];
  onChange: (l: Linha[]) => void; onNaoDisp?: (i: number, v: boolean) => void; erros: Record<string, string>;
  desabilitado?: boolean; avisos?: string[]; extra?: (chave: string) => ReactNode;
}) {
  const mudar = (i: number, campo: string, valor: string) =>
    onChange(linhas.map((l, j) => (j === i ? { ...l, [campo]: valor } : l)));
  return (
    <Stack spacing={2}>
      {erros[nome] && <Alert severity="error">{erros[nome]}</Alert>}
      {infos.map((info, i) => {
        const off = desabilitado || !!naoDisp?.[i];
        return (
          <Card key={info.chave} variant="outlined" sx={{ p: 2 }} data-testid={`${nome}-${info.chave}`}>
            <Stack direction={{ xs: "column", sm: "row" }} justifyContent="space-between" alignItems={{ sm: "center" }} sx={{ mb: 1.5 }}>
              <Box>
                <Typography variant="subtitle2" component="h4">{info.titulo}</Typography>
                {info.subtitulo && <Typography variant="body2" color="text.secondary">{info.subtitulo}</Typography>}
              </Box>
              {onNaoDisp && (
                <NaoDisponivel marcado={!!naoDisp?.[i]} onChange={(v) => onNaoDisp(i, v)} />
              )}
            </Stack>
            {extra?.(info.chave)}
            <CamposDaLinha specs={specs} linha={linhas[i]} chave={info.chave} prefixo={`${nome}[${i}]`} erros={erros}
              onChange={(c, v) => mudar(i, c, v)} desabilitado={off} />
          </Card>
        );
      })}
      {avisos && avisos.length > 0 && (
        <Alert severity="warning">
          <Typography variant="body2" sx={{ fontWeight: 600 }}>Confira estes números (não impede de salvar, mas impede de enviar):</Typography>
          <ul style={{ margin: "4px 0 0", paddingLeft: 20 }}>{avisos.map((a) => <li key={a}>{a}</li>)}</ul>
        </Alert>
      )}
    </Stack>
  );
}
