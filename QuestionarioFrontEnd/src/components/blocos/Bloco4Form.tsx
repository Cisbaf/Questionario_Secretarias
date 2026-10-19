"use client";

import BlocoLayout from "@/components/BlocoLayout";
import { Marcaveis, NaoDisponivel, Secao, TabelaDinamica, TabelaFixa, Texto } from "@/components/campos/CamposBase";
import { useQuestionario } from "@/components/QuestionarioContext";
import { useBlocoQuestionario } from "@/hooks/useBlocoQuestionario";
import { CENTRAIS_REGULACAO, ORIGENS_DADO, TIPOS_LEITO, type CentralRegulacao } from "@/lib/rotulos";
import { erroDeData } from "@/lib/datas";
import { textoOuNulo } from "@/lib/numeros";
import {
  dinamicasDeServidor, dinamicasParaServidor, fixasDeServidor, fixasParaServidor, marcarNaoDisp, num,
  validarLinhas, type CampoSpec, type FixasValores, type Linha,
} from "@/lib/tabelas";
import type { Bloco4Servidor } from "@/types";

export const MAX_ELETIVAS = 10;
const CHAVES_LEITO = TIPOS_LEITO.map((t) => t.valor);

export const LEITO_SPEC: CampoSpec[] = [
  { nome: "solicitacoes", rotulo: "Solicitações no período", tipo: "inteiro" },
  { nome: "atendidas", rotulo: "Atendidas (vaga concedida)", tipo: "inteiro" },
  { nome: "canceladas", rotulo: "Canceladas ou óbito antes da vaga", tipo: "inteiro" },
  { nome: "tempoMedioHoras", rotulo: "Tempo médio até a vaga (horas)", tipo: "decimal" },
  { nome: "aguardando", rotulo: "Aguardando na data da extração", tipo: "inteiro" },
];

export const ELETIVA_SPEC: CampoSpec[] = [
  { nome: "especialidade", rotulo: "Especialidade / procedimento", tipo: "texto", max: 200, largo: true },
  { nome: "sigtap", rotulo: "Código SIGTAP (10 números)", tipo: "sigtap" },
  { nome: "totalAguardando", rotulo: "Total aguardando", tipo: "inteiro" },
  { nome: "esperaMediaDias", rotulo: "Espera média (dias)", tipo: "decimal" },
  { nome: "maiorEsperaDias", rotulo: "Maior espera (dias)", tipo: "inteiro" },
  { nome: "origem", rotulo: "Origem do dado", tipo: "opcao", opcoes: ORIGENS_DADO },
  { nome: "origemOutro", rotulo: "Qual é a origem?", tipo: "texto", max: 150, soSe: { campo: "origem", valor: "OUTRO" } },
];

export type Bloco4Valores = {
  leitos: FixasValores;
  leitoOutroDescricao: string;
  leitosNaoDisponivel: boolean;
  dataExtracaoLeitos: string;
  centrais: CentralRegulacao[];
  centralOutra: string;
  eletivas: Linha[];
  eletivasNaoDisponivel: boolean;
  dataExtracaoEletivas: string;
};

export function deServidorBloco4(r: Bloco4Servidor): Bloco4Valores {
  return {
    leitos: fixasDeServidor(LEITO_SPEC, CHAVES_LEITO, "tipo", r.leitos as unknown as Record<string, unknown>[]),
    leitoOutroDescricao: r.leitoOutroDescricao ?? "",
    leitosNaoDisponivel: r.leitosNaoDisponivel === true,
    dataExtracaoLeitos: r.dataExtracaoLeitos ?? "",
    centrais: CENTRAIS_REGULACAO.map((c) => c.valor).filter((v) => r.centrais?.includes(v)),
    centralOutra: r.centralOutra ?? "",
    eletivas: dinamicasDeServidor(ELETIVA_SPEC, r.eletivas as unknown as Record<string, unknown>[]),
    eletivasNaoDisponivel: r.eletivasNaoDisponivel === true,
    dataExtracaoEletivas: r.dataExtracaoEletivas ?? "",
  };
}

export function paraServidorBloco4(v: Bloco4Valores, version: number | null) {
  return {
    leitos: v.leitosNaoDisponivel
      ? fixasParaServidor(LEITO_SPEC, CHAVES_LEITO, "tipo", { linhas: v.leitos.linhas.map((l) => Object.fromEntries(Object.keys(l).map((k) => [k, ""]))), naoDisp: v.leitos.naoDisp })
      : fixasParaServidor(LEITO_SPEC, CHAVES_LEITO, "tipo", v.leitos),
    leitoOutroDescricao: textoOuNulo(v.leitoOutroDescricao),
    leitosNaoDisponivel: v.leitosNaoDisponivel,
    dataExtracaoLeitos: textoOuNulo(v.dataExtracaoLeitos),
    centrais: v.centrais,
    centralOutra: v.centrais.includes("OUTRA") ? textoOuNulo(v.centralOutra) : null,
    eletivas: v.eletivasNaoDisponivel ? [] : dinamicasParaServidor(ELETIVA_SPEC, v.eletivas),
    eletivasNaoDisponivel: v.eletivasNaoDisponivel,
    dataExtracaoEletivas: textoOuNulo(v.dataExtracaoEletivas),
    version,
  };
}

export function validarBloco4(v: Bloco4Valores): Record<string, string> {
  const erros: Record<string, string> = {};
  if (v.leitoOutroDescricao.trim().length > 150) erros.leitoOutroDescricao = "Máximo de 150 caracteres";
  if (v.centralOutra.trim().length > 150) erros.centralOutra = "Máximo de 150 caracteres";
  const d1 = erroDeData(v.dataExtracaoLeitos); if (d1) erros.dataExtracaoLeitos = d1;
  const d2 = erroDeData(v.dataExtracaoEletivas); if (d2) erros.dataExtracaoEletivas = d2;
  if (!v.leitosNaoDisponivel) {
    Object.assign(erros, validarLinhas(LEITO_SPEC, v.leitos.linhas.map((l, i) => (v.leitos.naoDisp[i] ? Object.fromEntries(Object.keys(l).map((k) => [k, ""])) : l)), "leitos"));
  }
  if (!v.eletivasNaoDisponivel) Object.assign(erros, validarLinhas(ELETIVA_SPEC, v.eletivas, "eletivas"));
  return erros;
}

export function avisosLeitos(v: Bloco4Valores): string[] {
  if (v.leitosNaoDisponivel) return [];
  const avisos: string[] = [];
  v.leitos.linhas.forEach((l, i) => {
    if (v.leitos.naoDisp[i]) return;
    const sol = num(l.solicitacoes);
    const fim = (num(l.atendidas) ?? 0) + (num(l.canceladas) ?? 0);
    if (sol !== null && fim > sol) avisos.push(`${TIPOS_LEITO[i].rotulo}: atendidas + canceladas é maior que as solicitações`);
  });
  return avisos;
}

export function avisosEletivas(v: Bloco4Valores): string[] {
  if (v.eletivasNaoDisponivel) return [];
  const avisos: string[] = [];
  v.eletivas.forEach((l, i) => {
    const media = num(l.esperaMediaDias, "decimal");
    const maior = num(l.maiorEsperaDias);
    if (media !== null && maior !== null && media > maior) avisos.push(`Linha ${i + 1}: a espera média é maior que a maior espera`);
  });
  return avisos;
}

export default function Bloco4Form() {
  const { uuid, recarregarQuestionario } = useQuestionario();
  const motor = useBlocoQuestionario<Bloco4Valores, Bloco4Servidor>({
    uuid, numero: 4, deServidor: deServidorBloco4, paraServidor: paraServidorBloco4, validar: validarBloco4,
    aoSalvar: () => void recarregarQuestionario(),
  });
  const { valores: v, alterar, errosCampos: e } = motor;
  const set = (parte: Partial<Bloco4Valores>) => alterar((a) => ({ ...a, ...parte }));

  return (
    <BlocoLayout numero={4} motor={motor}>
      {v && (
        <>
          <Secao id="b4-leitos" titulo="4.1 Solicitações de internação de urgência no período de referência, por tipo de leito"
            nota="Pedidos feitos pelas UPAs, prontos-socorros e hospitais do município à central de regulação (municipal, estadual ou regional)."
            ajuda="Por tipo de leito, no período dos números extraídos. Se não tem o dado de um tipo, marque “Não disponível” nele.">
            <Texto rotulo="Data em que os números foram extraídos" tipo="date" valor={v.dataExtracaoLeitos}
              onChange={(t) => set({ dataExtracaoLeitos: t })} erro={e.dataExtracaoLeitos} />
            <NaoDisponivel marcado={v.leitosNaoDisponivel} onChange={(m) => set({ leitosNaoDisponivel: m })}
              rotulo="Não disponível (não temos nenhum desses números)" />
            <TabelaFixa
              nome="leitos" specs={LEITO_SPEC}
              infos={TIPOS_LEITO.map((t) => ({ chave: t.valor, titulo: t.rotulo }))}
              linhas={v.leitos.linhas} naoDisp={v.leitos.naoDisp}
              onChange={(linhas) => set({ leitos: { ...v.leitos, linhas } })}
              onNaoDisp={(i, m) => set({ leitos: marcarNaoDisp(LEITO_SPEC, v.leitos, i, m) })}
              erros={e} desabilitado={v.leitosNaoDisponivel} avisos={avisosLeitos(v)}
              extra={(chave) => chave === "OUTRO" ? (
                <Texto rotulo="Qual é o outro tipo de leito?" valor={v.leitoOutroDescricao} max={150}
                  onChange={(t) => set({ leitoOutroDescricao: t })} erro={e.leitoOutroDescricao} desabilitado={v.leitosNaoDisponivel} />
              ) : null}
            />
          </Secao>

          <Secao id="b4-centrais" titulo="Central(is) que recebe(m) os pedidos">
            <Marcaveis rotulo="Os pedidos de leito vão para…" opcoes={CENTRAIS_REGULACAO} valor={v.centrais}
              onChange={(c) => set({ centrais: c })} erro={e.centrais} />
            {v.centrais.includes("OUTRA") && (
              <Texto rotulo="Qual é a outra central?" valor={v.centralOutra} max={150}
                onChange={(t) => set({ centralOutra: t })} erro={e.centralOutra} />
            )}
          </Secao>

          <Secao id="b4-eletivas" titulo="4.2 Fila de cirurgias eletivas e procedimentos hospitalares programados"
            nota="Inclua o que está no Opera Fácil, no SER, no SISREG e em controles próprios. Indique a origem do dado."
            ajuda={`Até ${MAX_ELETIVAS} linhas, as de maior fila.`}>
            <Texto rotulo="Data em que os números foram extraídos" tipo="date" valor={v.dataExtracaoEletivas}
              onChange={(t) => set({ dataExtracaoEletivas: t })} erro={e.dataExtracaoEletivas} />
            <NaoDisponivel marcado={v.eletivasNaoDisponivel} onChange={(m) => set({ eletivasNaoDisponivel: m })}
              rotulo="Não disponível (não temos esses números)" />
            <TabelaDinamica nome="eletivas" titulo="Linha" specs={ELETIVA_SPEC} linhas={v.eletivas}
              onChange={(l) => set({ eletivas: l })} limite={MAX_ELETIVAS} erros={e}
              desabilitado={v.eletivasNaoDisponivel} avisos={avisosEletivas(v)} />
          </Secao>
        </>
      )}
    </BlocoLayout>
  );
}

