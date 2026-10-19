"use client";

import BlocoLayout from "@/components/BlocoLayout";
import { NaoDisponivel, Secao, TabelaFixa } from "@/components/campos/CamposBase";
import { useQuestionario } from "@/components/QuestionarioContext";
import { useBlocoQuestionario } from "@/hooks/useBlocoQuestionario";
import { SITUACOES_CLINICAS } from "@/lib/rotulos";
import {
  fixasDeServidor, fixasParaServidor, marcarNaoDisp, validarLinhas, type CampoSpec, type FixasValores,
} from "@/lib/tabelas";
import type { Bloco6Servidor } from "@/types";

const CHAVES = SITUACOES_CLINICAS.map((s) => s.valor);

export const SEGUIMENTO_SPEC: CampoSpec[] = [
  { nome: "situacaoOutra", rotulo: "Qual é a situação?", tipo: "texto", max: 200, soNaChave: "OUTRO" },
  { nome: "procedimentoOutro", rotulo: "Qual é o procedimento de seguimento?", tipo: "texto", max: 200, soNaChave: "OUTRO" },
  { nome: "pacientesAguardando", rotulo: "Pacientes aguardando", tipo: "inteiro" },
  { nome: "esperaMediaDias", rotulo: "Espera média (dias)", tipo: "decimal" },
  { nome: "ondeFeito", rotulo: "Onde o procedimento é feito hoje", tipo: "texto", max: 300, largo: true },
];

export type Bloco6Valores = { situacoes: FixasValores; tabelaNaoDisponivel: boolean };

const zerar = (l: Record<string, string>) => Object.fromEntries(Object.keys(l).map((k) => [k, ""]));

export function deServidorBloco6(r: Bloco6Servidor): Bloco6Valores {
  return {
    situacoes: fixasDeServidor(SEGUIMENTO_SPEC, CHAVES, "situacao", r.situacoes as unknown as Record<string, unknown>[]),
    tabelaNaoDisponivel: r.tabelaNaoDisponivel === true,
  };
}

export function paraServidorBloco6(v: Bloco6Valores, version: number | null) {
  const situacoes = v.tabelaNaoDisponivel ? { ...v.situacoes, linhas: v.situacoes.linhas.map(zerar) } : v.situacoes;
  return {
    situacoes: fixasParaServidor(SEGUIMENTO_SPEC, CHAVES, "situacao", situacoes),
    tabelaNaoDisponivel: v.tabelaNaoDisponivel,
    version,
  };
}

export function validarBloco6(v: Bloco6Valores): Record<string, string> {
  if (v.tabelaNaoDisponivel) return {};
  const linhas = v.situacoes.linhas.map((l, i) => (v.situacoes.naoDisp[i] ? zerar(l) : l));
  return validarLinhas(SEGUIMENTO_SPEC, linhas, "situacoes", CHAVES);
}

export default function Bloco6Form() {
  const { uuid, recarregarQuestionario } = useQuestionario();
  const motor = useBlocoQuestionario<Bloco6Valores, Bloco6Servidor>({
    uuid, numero: 6, deServidor: deServidorBloco6, paraServidor: paraServidorBloco6, validar: validarBloco6,
    aoSalvar: () => void recarregarQuestionario(),
  });
  const { valores: v, alterar, errosCampos: e } = motor;
  const set = (parte: Partial<Bloco6Valores>) => alterar((a) => ({ ...a, ...parte }));

  return (
    <BlocoLayout numero={6} motor={motor}>
      {v && (
        <Secao id="b6-tabela" titulo="Pacientes que precisam de seguimento depois da urgência"
          ajuda="Depois que o paciente sai da UPA ou do hospital, quantos estão esperando o procedimento de seguimento? Preencha só as situações que o município acompanha.">
          <NaoDisponivel marcado={v.tabelaNaoDisponivel} onChange={(m) => set({ tabelaNaoDisponivel: m })}
            rotulo="Não disponível (não temos esses números)" />
          <TabelaFixa
            nome="situacoes" specs={SEGUIMENTO_SPEC}
            infos={SITUACOES_CLINICAS.map((s) => ({ chave: s.valor, titulo: s.situacao, subtitulo: s.procedimento || undefined }))}
            linhas={v.situacoes.linhas} naoDisp={v.situacoes.naoDisp}
            onChange={(linhas) => set({ situacoes: { ...v.situacoes, linhas } })}
            onNaoDisp={(i, m) => set({ situacoes: marcarNaoDisp(SEGUIMENTO_SPEC, v.situacoes, i, m) })}
            erros={e} desabilitado={v.tabelaNaoDisponivel}
          />
        </Secao>
      )}
    </BlocoLayout>
  );
}
