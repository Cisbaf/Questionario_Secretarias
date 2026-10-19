"use client";

import BlocoLayout from "@/components/BlocoLayout";
import { Escolha, Marcaveis, NaoDisponivel, Secao, SeletorMesAno, TabelaDinamica, Texto } from "@/components/campos/CamposBase";
import { useQuestionario } from "@/components/QuestionarioContext";
import { useBlocoQuestionario } from "@/hooks/useBlocoQuestionario";
import { SIM_NAO_EM_PARTE, SISTEMAS_REGULACAO, TIPOS_FILA, type SistemaRegulacao } from "@/lib/rotulos";
import {
  dinamicasDeServidor, dinamicasParaServidor, num, validarLinhas, type CampoSpec, type Linha,
} from "@/lib/tabelas";
import { erroDeData, erroDeMes } from "@/lib/datas";
import { textoOuNulo } from "@/lib/numeros";
import type { Bloco3Servidor } from "@/types";

export const MAX_FILAS = 15;

export const FILA_SPEC: CampoSpec[] = [
  { nome: "especialidade", rotulo: "Especialidade / exame", tipo: "texto", max: 200, largo: true },
  { nome: "sigtap", rotulo: "Código SIGTAP (10 números)", tipo: "sigtap" },
  { nome: "totalAguardando", rotulo: "Total aguardando", tipo: "inteiro" },
  { nome: "prioridadeAlta", rotulo: "Com prioridade alta", tipo: "inteiro" },
  { nome: "esperaMediaDias", rotulo: "Espera média (dias)", tipo: "decimal" },
  { nome: "maiorEsperaDias", rotulo: "Maior espera (dias)", tipo: "inteiro" },
  { nome: "aguardandoMais180", rotulo: "Aguardando há mais de 180 dias", tipo: "inteiro" },
];

export type Bloco3Valores = {
  sistemas: SistemaRegulacao[];
  sistemaProprioNome: string;
  sistemaOutroNome: string;
  tipoFila: string;
  classificacaoRisco: string;
  ultimaRevisao: string;
  dataExtracao: string;
  filasNaoDisponivel: boolean;
  filas: Linha[];
};

export function deServidorBloco3(r: Bloco3Servidor): Bloco3Valores {
  return {
    sistemas: SISTEMAS_REGULACAO.map((s) => s.valor).filter((v) => r.sistemas?.includes(v)),
    sistemaProprioNome: r.sistemaProprioNome ?? "",
    sistemaOutroNome: r.sistemaOutroNome ?? "",
    tipoFila: r.tipoFila ?? "",
    classificacaoRisco: r.classificacaoRisco ?? "",
    ultimaRevisao: r.ultimaRevisao ?? "",
    dataExtracao: r.dataExtracao ?? "",
    filasNaoDisponivel: r.filasNaoDisponivel === true,
    filas: dinamicasDeServidor(FILA_SPEC, r.filas as unknown as Record<string, unknown>[]),
  };
}

export function paraServidorBloco3(v: Bloco3Valores, version: number | null) {
  return {
    sistemas: v.sistemas,
    sistemaProprioNome: v.sistemas.includes("PROPRIO") ? textoOuNulo(v.sistemaProprioNome) : null,
    sistemaOutroNome: v.sistemas.includes("OUTRO") ? textoOuNulo(v.sistemaOutroNome) : null,
    tipoFila: v.tipoFila || null,
    classificacaoRisco: v.classificacaoRisco || null,
    ultimaRevisao: textoOuNulo(v.ultimaRevisao),
    dataExtracao: textoOuNulo(v.dataExtracao),
    filasNaoDisponivel: v.filasNaoDisponivel,
    filas: v.filasNaoDisponivel ? [] : dinamicasParaServidor(FILA_SPEC, v.filas),
    version,
  };
}

export function validarBloco3(v: Bloco3Valores): Record<string, string> {
  const erros: Record<string, string> = {};
  if (v.sistemaProprioNome.trim().length > 150) erros.sistemaProprioNome = "Máximo de 150 caracteres";
  if (v.sistemaOutroNome.trim().length > 150) erros.sistemaOutroNome = "Máximo de 150 caracteres";
  const eMes = erroDeMes(v.ultimaRevisao); if (eMes) erros.ultimaRevisao = eMes;
  const eData = erroDeData(v.dataExtracao); if (eData) erros.dataExtracao = eData;
  if (!v.filasNaoDisponivel) Object.assign(erros, validarLinhas(FILA_SPEC, v.filas, "filas"));
  return erros;
}

/** Coerências do 3.3 (só avisam ao salvar; bloqueiam o envio). */
export function avisosBloco3(v: Bloco3Valores): string[] {
  if (v.filasNaoDisponivel) return [];
  const avisos: string[] = [];
  v.filas.forEach((f, i) => {
    const total = num(f.totalAguardando);
    const l = `Fila ${i + 1}: `;
    const alta = num(f.prioridadeAlta);
    if (alta !== null && total !== null && alta > total) avisos.push(`${l}com prioridade alta é maior que o total aguardando`);
    const mais = num(f.aguardandoMais180);
    if (mais !== null && total !== null && mais > total) avisos.push(`${l}aguardando há mais de 180 dias é maior que o total`);
    const media = num(f.esperaMediaDias, "decimal");
    const maior = num(f.maiorEsperaDias);
    if (media !== null && maior !== null && media > maior) avisos.push(`${l}a espera média é maior que a maior espera`);
  });
  return avisos;
}

export default function Bloco3Form() {
  const { uuid, recarregarQuestionario } = useQuestionario();
  const motor = useBlocoQuestionario<Bloco3Valores, Bloco3Servidor>({
    uuid, numero: 3, deServidor: deServidorBloco3, paraServidor: paraServidorBloco3, validar: validarBloco3,
    aoSalvar: () => void recarregarQuestionario(),
  });
  const { valores: v, alterar, errosCampos: e } = motor;
  const set = (parte: Partial<Bloco3Valores>) => alterar((a) => ({ ...a, ...parte }));

  return (
    <BlocoLayout numero={3} motor={motor}>
      {v && (
        <>
          <Secao id="b3-sistemas" titulo="3.1 Quais sistemas o município usa para regular consultas e exames? (marque todos)">
            <Marcaveis rotulo="Sistemas utilizados" opcoes={SISTEMAS_REGULACAO} valor={v.sistemas}
              onChange={(s) => set({ sistemas: s })} erro={e.sistemas} />
            {v.sistemas.includes("PROPRIO") && (
              <Texto rotulo="Qual é o sistema próprio?" valor={v.sistemaProprioNome} max={150}
                onChange={(t) => set({ sistemaProprioNome: t })} erro={e.sistemaProprioNome} />
            )}
            {v.sistemas.includes("OUTRO") && (
              <Texto rotulo="Qual é o outro sistema?" valor={v.sistemaOutroNome} max={150}
                onChange={(t) => set({ sistemaOutroNome: t })} erro={e.sistemaOutroNome} />
            )}
          </Secao>

          <Secao id="b3-fila" titulo="3.2 Sobre a fila">
            <Escolha rotulo="A fila é única no município ou cada unidade mantém a sua?" opcoes={TIPOS_FILA} valor={v.tipoFila} onChange={(t) => set({ tipoFila: t })} erro={e.tipoFila} />
            <Escolha rotulo="A fila tem classificação de risco ou prioridade clínica?" opcoes={SIM_NAO_EM_PARTE} valor={v.classificacaoRisco}
              onChange={(t) => set({ classificacaoRisco: t })} erro={e.classificacaoRisco} />
            <SeletorMesAno rotulo="Quando a fila foi revisada pela última vez (contato com o paciente, retirada de duplicados e de quem já foi atendido)?" valor={v.ultimaRevisao}
              onChange={(t) => set({ ultimaRevisao: t })} erro={e.ultimaRevisao} />
          </Secao>

          <Secao id="b3-tabela" titulo="3.3 Pacientes aguardando consulta ou exame, por especialidade ou procedimento"
            nota="Liste as 15 maiores filas. Tempo de espera em dias, contado da data da solicitação até a data da extração. Se houver código SIGTAP, informe.">
            <Texto rotulo="Data de extração dos dados da tabela 3.3" tipo="date" valor={v.dataExtracao}
              onChange={(t) => set({ dataExtracao: t })} erro={e.dataExtracao} />
            <NaoDisponivel marcado={v.filasNaoDisponivel} onChange={(m) => set({ filasNaoDisponivel: m })}
              rotulo="Não disponível (não temos esses números)" />
            <TabelaDinamica nome="filas" titulo="Fila" specs={FILA_SPEC} linhas={v.filas}
              onChange={(l) => set({ filas: l })} limite={MAX_FILAS} erros={e} desabilitado={v.filasNaoDisponivel}
              avisos={avisosBloco3(v)} />
          </Secao>
        </>
      )}
    </BlocoLayout>
  );
}

