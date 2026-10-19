"use client";

import BlocoLayout from "@/components/BlocoLayout";
import { useQuestionario } from "@/components/QuestionarioContext";
import { useBlocoQuestionario } from "@/hooks/useBlocoQuestionario";
import {
  IMPEDIMENTOS, LINHAS_CUIDADO, RESOLVE_SOZINHO, TEMAS_URGENCIA, rotuloDoTema,
  type Impedimento, type LinhaCuidado, type ResolveSozinho, type TemaUrgencia,
} from "@/lib/rotulos";
import type { Bloco2Servidor } from "@/types";
import ArrowDownwardIcon from "@mui/icons-material/ArrowDownward";
import ArrowUpwardIcon from "@mui/icons-material/ArrowUpward";
import {
  Alert, Box, Button, Checkbox, Divider, FormControl, FormControlLabel, FormGroup, FormHelperText, FormLabel,
  IconButton, Radio, RadioGroup, Stack, TextField, Typography,
} from "@mui/material";
import { z } from "zod";

export const TOTAL_PRIORIDADES = 5;
export const MAX_IMPEDIMENTOS = 3;
const NOTAS = [1, 2, 3, 4, 5] as const;

export type PrioridadeValores = { titulo: string; problema: string; resolveSozinho: ResolveSozinho | "" };

/**
 * Campos do Bloco 2 como o formulário os edita. Os nomes são os mesmos do JSON do servidor para que o erro por campo
 * de uma resposta 400 (ex.: "grausLinhaCuidado.ONCOLOGIA") caia no lugar certo.
 */
export type Bloco2Valores = {
  prioridades: PrioridadeValores[];
  grausLinhaCuidado: Record<LinhaCuidado, number | null>;
  linhaOutraDescricao: string;
  gargalos: string;
  servicosParaVizinhos: string;
  impedimentos: Impedimento[];
  impedimentoOutro: string;
  /** 2.6: sempre uma permutação dos 6 temas, do mais urgente ao menos urgente. */
  ordemTemas: TemaUrgencia[];
  /** Falso até a pessoa mexer na ordem ou confirmá-la: a ordem padrão não vale como resposta. */
  ordemDefinida: boolean;
};

const ORDEM_PADRAO: TemaUrgencia[] = TEMAS_URGENCIA.map((t) => t.valor);

// ---------- validação (mesmas regras do backend: Bloco2Request + Bloco2Validacao) ----------

const maximo = (n: number) => `Máximo de ${n} caracteres`;

const esquema = z.object({
  prioridades: z.array(z.object({
    titulo: z.string().max(300, maximo(300)),
    problema: z.string().max(500, maximo(500)),
    resolveSozinho: z.string(),
  })),
  grausLinhaCuidado: z.record(z.string(), z.number().int().min(1, "Use uma nota de 1 a 5").max(5, "Use uma nota de 1 a 5").nullable()),
  linhaOutraDescricao: z.string().max(150, maximo(150)),
  gargalos: z.string().max(5000, maximo(5000)),
  servicosParaVizinhos: z.string().max(5000, maximo(5000)),
  impedimentos: z.array(z.string()),
  impedimentoOutro: z.string().max(150, maximo(150)),
});

/** "prioridades",0,"titulo" -> "prioridades[0].titulo"; "grausLinhaCuidado","ONCOLOGIA" -> "grausLinhaCuidado.ONCOLOGIA". */
function caminhoDoCampo(caminho: PropertyKey[]): string {
  return caminho.reduce<string>((texto, parte) => {
    if (typeof parte === "number") return `${texto}[${parte}]`;
    return texto === "" ? String(parte) : `${texto}.${String(parte)}`;
  }, "");
}

export function validarBloco2(valores: Bloco2Valores): Record<string, string> {
  const erros: Record<string, string> = {};
  const resultado = esquema.safeParse(valores);
  if (!resultado.success) {
    for (const problema of resultado.error.issues) {
      const campo = caminhoDoCampo(problema.path);
      if (!(campo in erros)) erros[campo] = problema.message;
    }
  }
  if (valores.grausLinhaCuidado.OUTRA !== null && valores.linhaOutraDescricao.trim() === "") {
    erros.linhaOutraDescricao ??= "Descreva a outra linha de cuidado";
  }
  if (valores.impedimentos.length > MAX_IMPEDIMENTOS) {
    erros.impedimentos = `Marque no máximo ${MAX_IMPEDIMENTOS} opções`;
  }
  if (valores.impedimentos.includes("OUTRO") && valores.impedimentoOutro.trim() === "") {
    erros.impedimentoOutro ??= "Descreva o outro impedimento";
  }
  return erros;
}

// ---------- conversão servidor <-> formulário ----------

function notasVazias(): Record<LinhaCuidado, number | null> {
  return Object.fromEntries(LINHAS_CUIDADO.map((l) => [l.valor, null])) as Record<LinhaCuidado, number | null>;
}

export function deServidorBloco2(r: Bloco2Servidor): Bloco2Valores {
  const prioridades: PrioridadeValores[] = Array.from({ length: TOTAL_PRIORIDADES }, (_, i) => ({
    titulo: r.prioridades?.[i]?.titulo ?? "",
    problema: r.prioridades?.[i]?.problema ?? "",
    resolveSozinho: r.prioridades?.[i]?.resolveSozinho ?? "",
  }));
  const graus = notasVazias();
  for (const linha of LINHAS_CUIDADO) graus[linha.valor] = r.grausLinhaCuidado?.[linha.valor] ?? null;

  // 2.6: só vale como "definida" se o servidor tem as 6 posições; ordem parcial volta ao padrão.
  const posicoes = TEMAS_URGENCIA.map((t) => ({ tema: t.valor, posicao: r.ordemTemas?.[t.valor] ?? null }));
  const completa = posicoes.every((p) => p.posicao !== null);
  const ordemTemas = completa
    ? [...posicoes].sort((a, b) => (a.posicao as number) - (b.posicao as number)).map((p) => p.tema)
    : [...ORDEM_PADRAO];

  return {
    prioridades,
    grausLinhaCuidado: graus,
    linhaOutraDescricao: r.linhaOutraDescricao ?? "",
    gargalos: r.gargalos ?? "",
    servicosParaVizinhos: r.servicosParaVizinhos ?? "",
    impedimentos: IMPEDIMENTOS.map((i) => i.valor).filter((valor) => r.impedimentos?.includes(valor)),
    impedimentoOutro: r.impedimentoOutro ?? "",
    ordemTemas,
    ordemDefinida: completa,
  };
}

const vazioParaNulo = (valor: string) => (valor.trim() === "" ? null : valor.trim());

export function paraServidorBloco2(v: Bloco2Valores, version: number | null) {
  const graus: Partial<Record<LinhaCuidado, number>> = {};
  for (const linha of LINHAS_CUIDADO) {
    const nota = v.grausLinhaCuidado[linha.valor];
    if (nota !== null) graus[linha.valor] = nota;
  }
  const ordemTemas: Partial<Record<TemaUrgencia, number>> = {};
  if (v.ordemDefinida) v.ordemTemas.forEach((tema, indice) => { ordemTemas[tema] = indice + 1; });
  return {
    prioridades: v.prioridades.map((p) => ({
      titulo: vazioParaNulo(p.titulo),
      problema: vazioParaNulo(p.problema),
      resolveSozinho: p.resolveSozinho === "" ? null : p.resolveSozinho,
    })),
    grausLinhaCuidado: graus,
    linhaOutraDescricao: vazioParaNulo(v.linhaOutraDescricao),
    gargalos: vazioParaNulo(v.gargalos),
    servicosParaVizinhos: vazioParaNulo(v.servicosParaVizinhos),
    impedimentos: v.impedimentos,
    impedimentoOutro: v.impedimentos.includes("OUTRO") ? vazioParaNulo(v.impedimentoOutro) : null,
    ordemTemas,
    version,
  };
}

// ---------- formulário ----------

function Secao({ id, titulo, ajuda, nota, children }: { id: string; titulo: string; ajuda?: string; nota?: string; children: React.ReactNode }) {
  return (
    <Box component="section" aria-labelledby={id} sx={{ mb: 5 }}>
      <Typography id={id} variant="h6" component="h3" sx={{ mb: 0.5 }}>{titulo}</Typography>
      {nota && <Typography variant="body2" color="text.secondary" sx={{ mb: 1, fontStyle: "italic" }}>{nota}</Typography>}
      {ajuda && <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>{ajuda}</Typography>}
      {children}
    </Box>
  );
}

export default function Bloco2Form() {
  const { uuid, recarregarQuestionario } = useQuestionario();
  const motor = useBlocoQuestionario<Bloco2Valores, Bloco2Servidor>({
    uuid,
    numero: 2,
    deServidor: deServidorBloco2,
    paraServidor: paraServidorBloco2,
    validar: validarBloco2,
    aoSalvar: () => void recarregarQuestionario(),
  });
  const { valores, alterar, errosCampos } = motor;

  const mudarPrioridade = (indice: number, parte: Partial<PrioridadeValores>) =>
    alterar((atual) => ({
      ...atual,
      prioridades: atual.prioridades.map((p, i) => (i === indice ? { ...p, ...parte } : p)),
    }));

  const mudarNota = (linha: LinhaCuidado, nota: number | null) =>
    alterar((atual) => ({ ...atual, grausLinhaCuidado: { ...atual.grausLinhaCuidado, [linha]: nota } }));

  const alternarImpedimento = (impedimento: Impedimento, marcado: boolean) =>
    alterar((atual) => {
      const sem = atual.impedimentos.filter((i) => i !== impedimento);
      if (!marcado) return { ...atual, impedimentos: sem };
      if (sem.length >= MAX_IMPEDIMENTOS) return atual; // a 4ª opção já fica desabilitada; isto é só uma garantia
      // mantém a ordem do documento
      return { ...atual, impedimentos: IMPEDIMENTOS.map((i) => i.valor).filter((v) => v === impedimento || sem.includes(v)) };
    });

  const moverTema = (indice: number, direcao: -1 | 1) =>
    alterar((atual) => {
      const destino = indice + direcao;
      if (destino < 0 || destino >= atual.ordemTemas.length) return atual;
      const ordem = [...atual.ordemTemas];
      [ordem[indice], ordem[destino]] = [ordem[destino], ordem[indice]];
      return { ...atual, ordemTemas: ordem, ordemDefinida: true };
    });

  const textoLongo = (nome: "gargalos" | "servicosParaVizinhos", rotulo: string) => (
    <TextField
      label={rotulo}
      multiline
      minRows={4}
      fullWidth
      value={valores?.[nome] ?? ""}
      onChange={(e) => alterar((atual) => ({ ...atual, [nome]: e.target.value }))}
      error={Boolean(errosCampos[nome])}
      helperText={errosCampos[nome] ?? `${(valores?.[nome] ?? "").length} / 5000`}
 slotProps={{ htmlInput: { maxLength: 5000 } }}
    />
  );

  return (
    <BlocoLayout numero={2} motor={motor}>
      {valores && (
        <>
          {/* 2.1 */}
          <Secao
            id="s21"
            titulo="2.1 Quais são as cinco maiores prioridades de saúde do seu município hoje?"
            ajuda="Liste em ordem de importância. Pense no que mais pesa na vida da população e na gestão, e não apenas na falta de serviço."
          >
            <Stack spacing={2}>
              {valores.prioridades.map((p, i) => {
                const n = i + 1;
                return (
                  <Box
                    key={n}
                    role="group"
                    aria-label={`Prioridade ${n}`}
                    sx={{ border: 1, borderColor: "divider", borderRadius: 1, p: 2 }}
                  >
                    <Typography variant="subtitle2" sx={{ mb: 1.5 }}>Prioridade {n}</Typography>
                    <Box sx={{ display: "grid", gap: 2, gridTemplateColumns: { xs: "1fr", md: "1fr 1fr" } }}>
                      <TextField
                        label={`Prioridade ${n}`}
                        value={p.titulo}
                        onChange={(e) => mudarPrioridade(i, { titulo: e.target.value })}
                        error={Boolean(errosCampos[`prioridades[${i}].titulo`])}
                        helperText={errosCampos[`prioridades[${i}].titulo`]}
 slotProps={{ htmlInput: { maxLength: 300 } }}
                        multiline
                        minRows={2}
                        fullWidth
                      />
                      <TextField
                        label={`Qual problema a prioridade ${n} responde`}
                        value={p.problema}
                        onChange={(e) => mudarPrioridade(i, { problema: e.target.value })}
                        error={Boolean(errosCampos[`prioridades[${i}].problema`])}
                        helperText={errosCampos[`prioridades[${i}].problema`]}
 slotProps={{ htmlInput: { maxLength: 500 } }}
                        multiline
                        minRows={2}
                        fullWidth
                      />
                    </Box>
                    <FormControl sx={{ mt: 1.5 }}>
                      <FormLabel id={`resolve-${n}`} sx={{ fontSize: 14 }}>O município resolve sozinho?</FormLabel>
                      <Stack direction="row" alignItems="center" flexWrap="wrap" useFlexGap>
                        <RadioGroup
                          row
                          aria-labelledby={`resolve-${n}`}
                          value={p.resolveSozinho}
                          onChange={(e) => mudarPrioridade(i, { resolveSozinho: e.target.value as ResolveSozinho })}
                        >
                          {RESOLVE_SOZINHO.map((o) => (
                            <FormControlLabel
                              key={o.valor}
                              value={o.valor}
                              control={<Radio slotProps={{ input: { "aria-label": `Prioridade ${n}: município resolve sozinho — ${o.rotulo}` } }} />}
                              label={o.rotulo}
                            />
                          ))}
                        </RadioGroup>
                        {p.resolveSozinho !== "" && (
                          <Button size="small" onClick={() => mudarPrioridade(i, { resolveSozinho: "" })} aria-label={`Limpar a resposta da prioridade ${n}`}>
                            Limpar
                          </Button>
                        )}
                      </Stack>
                    </FormControl>
                  </Box>
                );
              })}
            </Stack>
            {errosCampos.prioridades && <FormHelperText error sx={{ mt: 1 }}>{errosCampos.prioridades}</FormHelperText>}
          </Secao>

          <Divider sx={{ mb: 5 }} />

          {/* 2.2 */}
          <Secao
            id="s22"
            titulo="2.2 Grau de prioridade por linha de cuidado"
            ajuda="Marque de 1 (baixa prioridade) a 5 (prioridade máxima) para o seu município. Pense nas necessidades da população do seu município e no que, hoje, ele não consegue garantir."
          >
            <Box sx={{ border: 1, borderColor: "divider", borderRadius: 1 }}>
              {LINHAS_CUIDADO.map((linha, i) => {
                const nota = valores.grausLinhaCuidado[linha.valor];
                const ehOutra = linha.valor === "OUTRA";
                return (
                  <Box
                    key={linha.valor}
                    role="group"
                    aria-label={ehOutra ? "Outra linha de cuidado" : linha.rotulo}
                    sx={{
                      display: "grid",
                      gap: { xs: 1, md: 3 },
                      alignItems: "center",
                      gridTemplateColumns: { xs: "1fr", md: "minmax(0, 1fr) auto" },
                      p: 1.5,
                      bgcolor: i % 2 ? "action.hover" : "transparent",
                    }}
                  >
                    {ehOutra ? (
                      <TextField
                        label="Outra linha de cuidado (descreva)"
                        size="small"
                        fullWidth
                        value={valores.linhaOutraDescricao}
                        onChange={(e) => alterar((atual) => ({ ...atual, linhaOutraDescricao: e.target.value }))}
                        error={Boolean(errosCampos.linhaOutraDescricao)}
                        helperText={errosCampos.linhaOutraDescricao}
 slotProps={{ htmlInput: { maxLength: 150 } }}
                      />
                    ) : (
                      <Box>
                        <Typography variant="body1" sx={{ fontWeight: 500 }}>{linha.rotulo}</Typography>
                        {"detalhe" in linha && (
                          <Typography variant="caption" color="text.secondary">({linha.detalhe})</Typography>
                        )}
                      </Box>
                    )}
                    <Stack direction="row" alignItems="center" flexWrap="wrap" useFlexGap>
                      <RadioGroup
                        row
                        aria-label={`Nota de ${ehOutra ? "outra linha de cuidado" : linha.rotulo}`}
                        value={nota === null ? "" : String(nota)}
                        onChange={(e) => mudarNota(linha.valor, Number(e.target.value))}
                      >
                        {NOTAS.map((n) => (
                          <FormControlLabel
                            key={n}
                            value={String(n)}
                            labelPlacement="bottom"
                            control={
                              <Radio
                                size="small"
                                slotProps={{ input: { "aria-label": `${ehOutra ? "Outra linha" : linha.rotulo}: nota ${n}` } }}
                              />
                            }
                            label={String(n)}
                            sx={{ mx: 0.5, minWidth: 40 }}
                          />
                        ))}
                      </RadioGroup>
                      <Button
                        size="small"
                        sx={{ visibility: nota === null ? "hidden" : "visible" }}
                        tabIndex={nota === null ? -1 : 0}
                        onClick={() => mudarNota(linha.valor, null)}
                        aria-label={`Limpar a nota de ${ehOutra ? "outra linha" : linha.rotulo}`}
                      >
                        Limpar
                      </Button>
                    </Stack>
                  </Box>
                );
              })}
            </Box>
            {Object.keys(errosCampos).filter((k) => k.startsWith("grausLinhaCuidado.")).map((k) => (
              <FormHelperText key={k} error>{k.replace("grausLinhaCuidado.", "")}: {errosCampos[k]}</FormHelperText>
            ))}
            {errosCampos.grausLinhaCuidado && <FormHelperText error sx={{ mt: 1 }}>{errosCampos.grausLinhaCuidado}</FormHelperText>}
          </Secao>

          <Divider sx={{ mb: 5 }} />

          {/* 2.3 */}
          <Secao id="s23" titulo="2.3 Quais especialidades, exames ou cirurgias são hoje os maiores gargalos de acesso?">
            {textoLongo("gargalos", "Gargalos de acesso")}
          </Secao>

          {/* 2.4 */}
          <Secao
            id="s24"
            titulo="2.4 Que serviços o seu município tem, ou poderia ter, para atender também os municípios vizinhos?"
            ajuda="Ex.: policlínica, centro de imagem, aparelho de tomografia ou ressonância com horário ocioso, centro cirúrgico ocioso, leitos que poderiam ser regulados regionalmente."
          >
            {textoLongo("servicosParaVizinhos", "Serviços para atender os municípios vizinhos")}
          </Secao>

          <Divider sx={{ mb: 5 }} />

          {/* 2.5 */}
          <Secao id="s25" titulo="2.5 O que mais impede o município de ampliar a oferta? (marque até três)">
            <FormControl error={Boolean(errosCampos.impedimentos)} component="fieldset" variant="standard" sx={{ width: "100%" }}>
              <FormGroup aria-label="Impedimentos para ampliar a oferta">
                {IMPEDIMENTOS.map((o) => {
                  const marcado = valores.impedimentos.includes(o.valor);
                  const bloqueado = !marcado && valores.impedimentos.length >= MAX_IMPEDIMENTOS;
                  return (
                    <FormControlLabel
                      key={o.valor}
                      disabled={bloqueado}
                      control={<Checkbox checked={marcado} onChange={(e) => alternarImpedimento(o.valor, e.target.checked)} />}
                      label={o.rotulo}
                    />
                  );
                })}
              </FormGroup>
              <FormHelperText>
                {errosCampos.impedimentos ?? `${valores.impedimentos.length} de ${MAX_IMPEDIMENTOS} marcadas`}
              </FormHelperText>
            </FormControl>
            {valores.impedimentos.includes("OUTRO") && (
              <TextField
                label="Outro impedimento (descreva)"
                fullWidth
                sx={{ mt: 2 }}
                value={valores.impedimentoOutro}
                onChange={(e) => alterar((atual) => ({ ...atual, impedimentoOutro: e.target.value }))}
                error={Boolean(errosCampos.impedimentoOutro)}
                helperText={errosCampos.impedimentoOutro}
 slotProps={{ htmlInput: { maxLength: 150 } }}
              />
            )}
          </Secao>

          <Divider sx={{ mb: 5 }} />

          {/* 2.6 */}
          <Secao
            id="s26"
            titulo="2.6 Ordene os temas abaixo pela urgência para o seu município (1 = mais urgente)"
            ajuda="Use as setas para subir ou descer cada tema."
          >
            {!valores.ordemDefinida && (
              <Alert
                severity="info"
                sx={{ mb: 2 }}
                action={
                  <Button color="inherit" size="small" onClick={() => alterar((atual) => ({ ...atual, ordemDefinida: true }))}>
                    Confirmar esta ordem
                  </Button>
                }
              >
                A ordem abaixo é só a do documento e ainda não conta como resposta. Mexa nas setas ou confirme-a.
              </Alert>
            )}
            <Box component="ol" aria-label="Temas por urgência" sx={{ listStyle: "none", p: 0, m: 0, border: 1, borderColor: "divider", borderRadius: 1 }}>
              {valores.ordemTemas.map((tema, indice) => (
                <Box
                  component="li"
                  key={tema}
                  sx={{
                    position: "relative", display: "flex", alignItems: "center", gap: 1.5, p: 1.5,
                    borderTop: indice === 0 ? 0 : 1, borderColor: "divider",
                    opacity: valores.ordemDefinida ? 1 : 0.7,
                  }}
                >
                  <Typography
                    component="span"
                    aria-hidden
                    sx={{ width: 28, height: 28, borderRadius: "50%", bgcolor: "primary.main", color: "primary.contrastText", display: "grid", placeItems: "center", fontWeight: 700, flexShrink: 0 }}
                  >
                    {indice + 1}
                  </Typography>
                  <Typography sx={{ flex: 1 }}>
                    <Box component="span" sx={{ position: "absolute", width: "1px", height: "1px", overflow: "hidden", clip: "rect(0 0 0 0)" }}>
                      {`Posição ${indice + 1}: `}
                    </Box>
                    {rotuloDoTema(tema)}
                  </Typography>
                  <IconButton
                    size="small"
                    onClick={() => moverTema(indice, -1)}
                    disabled={indice === 0}
                    aria-label={`Subir: ${rotuloDoTema(tema)}`}
                  >
                    <ArrowUpwardIcon fontSize="small" />
                  </IconButton>
                  <IconButton
                    size="small"
                    onClick={() => moverTema(indice, 1)}
                    disabled={indice === valores.ordemTemas.length - 1}
                    aria-label={`Descer: ${rotuloDoTema(tema)}`}
                  >
                    <ArrowDownwardIcon fontSize="small" />
                  </IconButton>
                </Box>
              ))}
            </Box>
            {valores.ordemDefinida && (
              <Button size="small" sx={{ mt: 1 }} onClick={() => alterar((atual) => ({ ...atual, ordemTemas: [...ORDEM_PADRAO], ordemDefinida: false }))}>
                Limpar a ordem
              </Button>
            )}
            {errosCampos.ordemTemas && <FormHelperText error>{errosCampos.ordemTemas}</FormHelperText>}
          </Secao>
        </>
      )}
    </BlocoLayout>
  );
}
