"use client";

import BlocoLayout from "@/components/BlocoLayout";
import { useQuestionario } from "@/components/QuestionarioContext";
import { useBlocoQuestionario } from "@/hooks/useBlocoQuestionario";
import type { Bloco1Servidor } from "@/types";
import { Autocomplete, Box, TextField } from "@mui/material";
import { z } from "zod";

/** Campos do Bloco 1 como o formulário os edita: tudo texto, vazio = não preenchido. */
export type Bloco1Valores = {
  municipio: string;
  secretario: string;
  responsavel: string;
  cargoSetor: string;
  email: string;
  telefone: string;
  dataPreenchimento: string;
};

/** Sugestões para o campo Município. O campo é texto livre (D10): quem digitar outro nome também é aceito. */
const MUNICIPIOS_SUGERIDOS = [
  "Belford Roxo", "Duque de Caxias", "Itaguaí", "Japeri", "Magé", "Mesquita",
  "Nilópolis", "Nova Iguaçu", "Paracambi", "Queimados", "São João de Meriti", "Seropédica",
];

const maximo = (n: number) => `Máximo de ${n} caracteres`;
const texto150 = z.string().max(150, maximo(150));

// Mesmas regras do backend (Bloco1Request): formato e tamanho. Nada é obrigatório no rascunho (D5).
const esquema = z.object({
  municipio: texto150,
  secretario: texto150,
  responsavel: texto150,
  cargoSetor: texto150,
  email: z.string().max(150, maximo(150))
    .refine((v) => v.trim() === "" || /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v.trim()), "E-mail inválido"),
  telefone: z.string().max(30, maximo(30))
    .regex(/^[0-9 ()+-]*$/, "Use apenas números, espaços, parênteses, + e -"),
  dataPreenchimento: z.string().refine((v) => v === "" || /^\d{4}-\d{2}-\d{2}$/.test(v), "Data inválida"),
});

export function validarBloco1(valores: Bloco1Valores): Record<string, string> {
  const resultado = esquema.safeParse(valores);
  if (resultado.success) return {};
  const erros: Record<string, string> = {};
  for (const problema of resultado.error.issues) {
    const campo = String(problema.path[0]);
    if (!(campo in erros)) erros[campo] = problema.message;
  }
  return erros;
}

export const deServidorBloco1 = (r: Bloco1Servidor): Bloco1Valores => ({
  municipio: r.municipio ?? "",
  secretario: r.secretario ?? "",
  responsavel: r.responsavel ?? "",
  cargoSetor: r.cargoSetor ?? "",
  email: r.email ?? "",
  telefone: r.telefone ?? "",
  dataPreenchimento: r.dataPreenchimento ?? "",
});

const vazioParaNulo = (valor: string) => (valor.trim() === "" ? null : valor.trim());

export const paraServidorBloco1 = (v: Bloco1Valores, version: number | null) => ({
  municipio: vazioParaNulo(v.municipio),
  secretario: vazioParaNulo(v.secretario),
  responsavel: vazioParaNulo(v.responsavel),
  cargoSetor: vazioParaNulo(v.cargoSetor),
  email: vazioParaNulo(v.email),
  telefone: vazioParaNulo(v.telefone),
  dataPreenchimento: v.dataPreenchimento === "" ? null : v.dataPreenchimento,
  version,
});

export default function Bloco1Form() {
  const { uuid, recarregarQuestionario } = useQuestionario();
  const motor = useBlocoQuestionario<Bloco1Valores, Bloco1Servidor>({
    uuid,
    numero: 1,
    deServidor: deServidorBloco1,
    paraServidor: paraServidorBloco1,
    validar: validarBloco1,
    aoSalvar: () => void recarregarQuestionario(),
  });
  const { valores, alterar, errosCampos } = motor;

  const campo = (nome: keyof Bloco1Valores) => ({
    value: valores?.[nome] ?? "",
    onChange: (evento: { target: { value: string } }) => alterar((atual) => ({ ...atual, [nome]: evento.target.value })),
    error: Boolean(errosCampos[nome]),
    fullWidth: true,
  });

  return (
    <BlocoLayout numero={1} motor={motor}>
      <Box sx={{ display: "grid", gap: 3, gridTemplateColumns: { xs: "1fr", sm: "1fr 1fr" } }}>
        <Autocomplete
          freeSolo
          options={MUNICIPIOS_SUGERIDOS}
          inputValue={valores?.municipio ?? ""}
          onInputChange={(_, texto) => alterar((atual) => ({ ...atual, municipio: texto }))}
          renderInput={(parametros) => (
            <TextField
              {...parametros}
              label="Município"
              error={Boolean(errosCampos.municipio)}
              helperText={errosCampos.municipio ?? "Escolha na lista ou escreva o nome por extenso (ex.: Nova Iguaçu)."}
            />
          )}
        />
        <TextField
          label="Secretário(a) municipal de saúde"
          {...campo("secretario")}
          helperText={errosCampos.secretario}
        />
        <TextField
          label="Responsável pelo preenchimento"
          {...campo("responsavel")}
          helperText={errosCampos.responsavel}
        />
        <TextField label="Cargo / setor" {...campo("cargoSetor")} helperText={errosCampos.cargoSetor} />
        <TextField
          label="E-mail"
          {...campo("email")}
          slotProps={{ htmlInput: { inputMode: "email", autoComplete: "email" } }}
          helperText={errosCampos.email}
        />
        <TextField
          label="Telefone / WhatsApp"
          {...campo("telefone")}
          slotProps={{ htmlInput: { inputMode: "tel", autoComplete: "tel" } }}
          helperText={errosCampos.telefone ?? "Com DDD."}
        />
        <TextField
          label="Data do preenchimento"
          type="date"
          {...campo("dataPreenchimento")}
          slotProps={{ inputLabel: { shrink: true } }}
          helperText={errosCampos.dataPreenchimento}
        />
      </Box>
    </BlocoLayout>
  );
}
