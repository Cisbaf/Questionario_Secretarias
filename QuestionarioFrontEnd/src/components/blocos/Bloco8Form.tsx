"use client";

import BlocoLayout from "@/components/BlocoLayout";
import { Marcaveis, Secao, Texto } from "@/components/campos/CamposBase";
import { useQuestionario } from "@/components/QuestionarioContext";
import { useBlocoQuestionario } from "@/hooks/useBlocoQuestionario";
import { DOCUMENTOS_ANEXAVEIS, type DocumentoAnexavel } from "@/lib/rotulos";
import { textoOuNulo } from "@/lib/numeros";
import type { Bloco8Servidor } from "@/types";
import { Checkbox, FormControlLabel, FormHelperText } from "@mui/material";

export type Bloco8Valores = {
  observacoes: string;
  documentos: DocumentoAnexavel[];
  assinaturaNome: string;
  assinaturaData: string;
  declaracao: boolean;
};

export function deServidorBloco8(r: Bloco8Servidor): Bloco8Valores {
  return {
    observacoes: r.observacoes ?? "",
    documentos: DOCUMENTOS_ANEXAVEIS.map((d) => d.valor).filter((x) => r.documentos?.includes(x)),
    assinaturaNome: r.assinaturaNome ?? "",
    assinaturaData: r.assinaturaData ?? "",
    declaracao: r.declaracao === true,
  };
}

export function paraServidorBloco8(v: Bloco8Valores, version: number | null) {
  return {
    observacoes: textoOuNulo(v.observacoes),
    documentos: v.documentos,
    assinaturaNome: textoOuNulo(v.assinaturaNome),
    assinaturaData: textoOuNulo(v.assinaturaData),
    declaracao: v.declaracao,
    version,
  };
}

export function validarBloco8(v: Bloco8Valores): Record<string, string> {
  const erros: Record<string, string> = {};
  if (v.observacoes.trim().length > 5000) erros.observacoes = "Máximo de 5000 caracteres";
  if (v.assinaturaNome.trim().length > 150) erros.assinaturaNome = "Máximo de 150 caracteres";
  return erros;
}

export default function Bloco8Form() {
  const { uuid, recarregarQuestionario } = useQuestionario();
  const motor = useBlocoQuestionario<Bloco8Valores, Bloco8Servidor>({
    uuid, numero: 8, deServidor: deServidorBloco8, paraServidor: paraServidorBloco8, validar: validarBloco8,
    aoSalvar: () => void recarregarQuestionario(),
  });
  const { valores: v, alterar, errosCampos: e } = motor;
  const set = (parte: Partial<Bloco8Valores>) => alterar((a) => ({ ...a, ...parte }));

  return (
    <BlocoLayout numero={8} motor={motor}>
      {v && (
        <>
          <Secao id="b8-obs" titulo="Observações finais" ajuda="Algo que o questionário não perguntou e que o CISBAF deveria saber.">
            <Texto rotulo="Observações" multiline valor={v.observacoes} max={5000}
              onChange={(t) => set({ observacoes: t })} erro={e.observacoes} />
          </Secao>

          <Secao id="b8-docs" titulo="Documentos que o município pode enviar depois"
            ajuda="Só marque o que o município pode mandar à parte; o envio de arquivos não é feito neste site.">
            <Marcaveis rotulo="Documentos" opcoes={DOCUMENTOS_ANEXAVEIS} valor={v.documentos}
              onChange={(d) => set({ documentos: d })} erro={e.documentos} />
          </Secao>

          <Secao id="b8-assinatura" titulo="Assinatura">
            <Texto rotulo="Nome do(a) secretário(a) de Saúde" valor={v.assinaturaNome} max={150}
              onChange={(t) => set({ assinaturaNome: t })} erro={e.assinaturaNome} />
            <Texto rotulo="Data" tipo="date" valor={v.assinaturaData}
              onChange={(t) => set({ assinaturaData: t })} erro={e.assinaturaData} />
            <FormControlLabel
              control={<Checkbox checked={v.declaracao} onChange={(ev) => set({ declaracao: ev.target.checked })} />}
              label="Declaro que as informações prestadas são verdadeiras."
            />
            {e.declaracao && <FormHelperText error>{e.declaracao}</FormHelperText>}
          </Secao>
        </>
      )}
    </BlocoLayout>
  );
}
