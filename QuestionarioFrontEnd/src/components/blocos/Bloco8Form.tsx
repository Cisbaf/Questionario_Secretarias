"use client";

import BlocoLayout from "@/components/BlocoLayout";
import { Marcaveis, Secao, Texto } from "@/components/campos/CamposBase";
import { useQuestionario } from "@/components/QuestionarioContext";
import { useBlocoQuestionario } from "@/hooks/useBlocoQuestionario";
import { DOCUMENTOS_ANEXAVEIS, type DocumentoAnexavel } from "@/lib/rotulos";
import { erroDeData } from "@/lib/datas";
import { textoOuNulo } from "@/lib/numeros";
import type { Bloco8Servidor } from "@/types";
import AnexosDocumento from "@/components/blocos/AnexosDocumento";
import { listarAnexos, FORMATOS_TEXTO, MAX_ARQUIVOS_POR_DOCUMENTO, MAX_MB } from "@/lib/anexos";
import type { Anexo } from "@/types";
import { Checkbox, FormControlLabel, FormHelperText, Typography } from "@mui/material";
import { useEffect, useState } from "react";

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
  const d = erroDeData(v.assinaturaData); if (d) erros.assinaturaData = d;
  return erros;
}

export default function Bloco8Form() {
  const { uuid, questionario, recarregarQuestionario } = useQuestionario();
  const enviado = questionario?.status === "ENVIADO";
  const [anexos, setAnexos] = useState<Anexo[]>([]);
  const [erroAnexos, setErroAnexos] = useState(false);
  useEffect(() => {
    let ativo = true;
    listarAnexos(uuid).then((l) => { if (ativo) setAnexos(l); }).catch(() => { if (ativo) setErroAnexos(true); });
    return () => { ativo = false; };
  }, [uuid]);
  const motor = useBlocoQuestionario<Bloco8Valores, Bloco8Servidor>({
    uuid, numero: 8, deServidor: deServidorBloco8, paraServidor: paraServidorBloco8, validar: validarBloco8,
    aoSalvar: () => void recarregarQuestionario(),
  });
  const { valores: v, alterar, errosCampos: e } = motor;
  const set = (parte: Partial<Bloco8Valores>) => alterar((a) => ({ ...a, ...parte }));
  // Documento com arquivo anexado conta como marcado mesmo que o bloco ainda não tenha sido salvo com essa marcação
  // (o servidor também mantém essa marcação ao salvar).
  const comArquivo = [...new Set(anexos.map((a) => a.documento))].filter((d): d is DocumentoAnexavel => d !== "PLANILHA_COLETA");
  const marcados = v ? DOCUMENTOS_ANEXAVEIS.map((d) => d.valor).filter((x) => v.documentos.includes(x) || comArquivo.includes(x)) : [];

  return (
    <BlocoLayout numero={8} motor={motor}>
      {v && (
        <>
          <Secao id="b8-obs" titulo="8.1 Há algo sobre a rede do seu município que a equipe de planejamento precisa saber e que não foi perguntado?">
            <Texto rotulo="Observações" multiline valor={v.observacoes} max={5000}
              onChange={(t) => set({ observacoes: t })} erro={e.observacoes} />
          </Secao>

          <Secao id="b8-docs" titulo="8.2 Documentos que podem ser anexados"
            ajuda={`Marque os documentos que o município pode enviar e anexe os arquivos logo abaixo de cada um (até ${MAX_ARQUIVOS_POR_DOCUMENTO} arquivos por documento, ${MAX_MB} MB cada: ${FORMATOS_TEXTO}). Quem já tem arquivo anexado fica marcado até o arquivo ser removido.`}>
            <Marcaveis rotulo="Documentos" opcoes={DOCUMENTOS_ANEXAVEIS} valor={marcados}
              travadas={comArquivo}
              onChange={(d) => set({ documentos: d })} erro={e.documentos} />
            {erroAnexos && <FormHelperText error>Não foi possível carregar os arquivos já anexados. Recarregue a página.</FormHelperText>}
            {DOCUMENTOS_ANEXAVEIS.filter((d) => marcados.includes(d.valor)).map((d) => (
              <AnexosDocumento key={d.valor} uuid={uuid} documento={d.valor} anexos={anexos} enviado={enviado} aoMudar={setAnexos} />
            ))}
          </Secao>

          <Secao id="b8-assinatura" titulo="Assinatura do(a) Secretário(a) Municipal de Saúde">
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

          <Typography variant="body2" color="text.secondary" sx={{ fontStyle: "italic" }}>
            Obrigado pela colaboração. As informações serão tratadas de forma agregada pelo Observatório Regional de Saúde e apresentadas aos municípios antes de compor a proposta regional.
          </Typography>
        </>
      )}
    </BlocoLayout>
  );
}
