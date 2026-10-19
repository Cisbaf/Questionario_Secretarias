"use client";

import BlocoLayout from "@/components/BlocoLayout";
import { NaoDisponivel, Secao, TabelaDinamica } from "@/components/campos/CamposBase";
import { useQuestionario } from "@/components/QuestionarioContext";
import { useBlocoQuestionario } from "@/hooks/useBlocoQuestionario";
import { APARELHOS_IMAGEM, SIM_NAO_PARCIAL, TIPOS_LEITO_PRIVADO } from "@/lib/rotulos";
import {
  dinamicasDeServidor, dinamicasParaServidor, num, validarLinhas, type CampoSpec, type Linha,
} from "@/lib/tabelas";
import type { Bloco7Servidor } from "@/types";

export const MAX_LINHAS = 20;

export const REPASSE_SPEC: CampoSpec[] = [
  { nome: "unidade", rotulo: "Unidade ou hospital", tipo: "texto", max: 200, largo: true },
  { nome: "cnes", rotulo: "CNES (7 números)", tipo: "cnes" },
  { nome: "atoNumero", rotulo: "Número do ato (portaria, resolução, termo)", tipo: "texto", max: 100 },
  { nome: "atoData", rotulo: "Data do ato", tipo: "data" },
  { nome: "valorMensal", rotulo: "Valor mensal (R$)", tipo: "dinheiro" },
  { nome: "vigenciaAte", rotulo: "Vigência (até)", tipo: "data", futura: true },
  { nome: "repasseEmDia", rotulo: "Repasse em dia?", tipo: "opcao", opcoes: SIM_NAO_PARCIAL },
  { nome: "observacao", rotulo: "Observação", tipo: "texto", max: 500, largo: true },
];

export const LEITO_PRIVADO_SPEC: CampoSpec[] = [
  { nome: "hospital", rotulo: "Hospital", tipo: "texto", max: 200, largo: true },
  { nome: "cnes", rotulo: "CNES (7 números)", tipo: "cnes" },
  { nome: "tipoLeito", rotulo: "Tipo de leito", tipo: "opcao", opcoes: TIPOS_LEITO_PRIVADO },
  { nome: "contratadosSus", rotulo: "Já contratados pelo SUS", tipo: "inteiro" },
  { nome: "poderiamSerContratados", rotulo: "Poderiam ser contratados", tipo: "inteiro" },
  { nome: "valorDiaria", rotulo: "Valor da diária (R$)", tipo: "dinheiro" },
  { nome: "observacao", rotulo: "Observação", tipo: "texto", max: 500, largo: true },
];

export const APARELHO_SPEC: CampoSpec[] = [
  { nome: "estabelecimento", rotulo: "Estabelecimento", tipo: "texto", max: 200, largo: true },
  { nome: "cnes", rotulo: "CNES (7 números)", tipo: "cnes" },
  { nome: "aparelho", rotulo: "Aparelho", tipo: "opcao", opcoes: APARELHOS_IMAGEM },
  { nome: "quantidadeCnes", rotulo: "Quantidade no CNES", tipo: "inteiro" },
  { nome: "emUsoDeFato", rotulo: "Em uso de fato", tipo: "inteiro" },
  { nome: "atendeSus", rotulo: "Atende o SUS?", tipo: "opcao", opcoes: SIM_NAO_PARCIAL },
  { nome: "correcaoNecessaria", rotulo: "Correção necessária no CNES", tipo: "texto", max: 500, largo: true },
];

export type Bloco7Valores = {
  repasses: Linha[];
  repassesNaoDisponivel: boolean;
  leitosPrivados: Linha[];
  leitosPrivadosNaoDisponivel: boolean;
  aparelhos: Linha[];
  aparelhosNaoDisponivel: boolean;
};

const lista = (r: unknown) => r as Record<string, unknown>[];

export function deServidorBloco7(r: Bloco7Servidor): Bloco7Valores {
  return {
    repasses: dinamicasDeServidor(REPASSE_SPEC, lista(r.repasses)),
    repassesNaoDisponivel: r.repassesNaoDisponivel === true,
    leitosPrivados: dinamicasDeServidor(LEITO_PRIVADO_SPEC, lista(r.leitosPrivados)),
    leitosPrivadosNaoDisponivel: r.leitosPrivadosNaoDisponivel === true,
    aparelhos: dinamicasDeServidor(APARELHO_SPEC, lista(r.aparelhos)),
    aparelhosNaoDisponivel: r.aparelhosNaoDisponivel === true,
  };
}

export function paraServidorBloco7(v: Bloco7Valores, version: number | null) {
  return {
    repasses: v.repassesNaoDisponivel ? [] : dinamicasParaServidor(REPASSE_SPEC, v.repasses),
    repassesNaoDisponivel: v.repassesNaoDisponivel,
    leitosPrivados: v.leitosPrivadosNaoDisponivel ? [] : dinamicasParaServidor(LEITO_PRIVADO_SPEC, v.leitosPrivados),
    leitosPrivadosNaoDisponivel: v.leitosPrivadosNaoDisponivel,
    aparelhos: v.aparelhosNaoDisponivel ? [] : dinamicasParaServidor(APARELHO_SPEC, v.aparelhos),
    aparelhosNaoDisponivel: v.aparelhosNaoDisponivel,
    version,
  };
}

export function validarBloco7(v: Bloco7Valores): Record<string, string> {
  const erros: Record<string, string> = {};
  if (!v.repassesNaoDisponivel) Object.assign(erros, validarLinhas(REPASSE_SPEC, v.repasses, "repasses"));
  if (!v.leitosPrivadosNaoDisponivel) Object.assign(erros, validarLinhas(LEITO_PRIVADO_SPEC, v.leitosPrivados, "leitosPrivados"));
  if (!v.aparelhosNaoDisponivel) Object.assign(erros, validarLinhas(APARELHO_SPEC, v.aparelhos, "aparelhos"));
  return erros;
}

export function avisosAparelhos(v: Bloco7Valores): string[] {
  if (v.aparelhosNaoDisponivel) return [];
  const avisos: string[] = [];
  v.aparelhos.forEach((a, i) => {
    const uso = num(a.emUsoDeFato);
    const qtd = num(a.quantidadeCnes);
    if (uso !== null && qtd !== null && uso > qtd) avisos.push(`Aparelho ${i + 1}: em uso de fato é maior que a quantidade no CNES`);
  });
  return avisos;
}

export default function Bloco7Form() {
  const { uuid, recarregarQuestionario } = useQuestionario();
  const motor = useBlocoQuestionario<Bloco7Valores, Bloco7Servidor>({
    uuid, numero: 7, deServidor: deServidorBloco7, paraServidor: paraServidorBloco7, validar: validarBloco7,
    aoSalvar: () => void recarregarQuestionario(),
  });
  const { valores: v, alterar, errosCampos: e } = motor;
  const set = (parte: Partial<Bloco7Valores>) => alterar((a) => ({ ...a, ...parte }));

  return (
    <BlocoLayout numero={7} motor={motor}>
      {v && (
        <>
          <Secao id="b7-repasses" titulo="7.1 Repasses estaduais de custeio que o município recebe hoje"
            nota="Inclua o cofinanciamento de hospitais, maternidades, UPAs e policlínicas e a contrapartida estadual do SAMU. Informe o ato (resolução SES ou deliberação CIB-RJ) e, se possível, anexe o documento."
            ajuda={`Até ${MAX_LINHAS} linhas.`}>
            <NaoDisponivel marcado={v.repassesNaoDisponivel} onChange={(m) => set({ repassesNaoDisponivel: m })}
              rotulo="Não disponível (não temos essa informação)" />
            <TabelaDinamica nome="repasses" titulo="Repasse" specs={REPASSE_SPEC} linhas={v.repasses}
              onChange={(l) => set({ repasses: l })} limite={MAX_LINHAS} erros={e} desabilitado={v.repassesNaoDisponivel} />
          </Secao>

          <Secao id="b7-leitos" titulo="7.2 Leitos de hospitais privados e filantrópicos no município"
            nota="Leitos que o município já contrata pelo SUS e leitos que poderiam ser contratados, por tipo (clínico, cirúrgico, UTI adulto, pediátrico, obstétrico, outro)."
            ajuda={`Até ${MAX_LINHAS} linhas. Uma linha por hospital e tipo de leito.`}>
            <NaoDisponivel marcado={v.leitosPrivadosNaoDisponivel} onChange={(m) => set({ leitosPrivadosNaoDisponivel: m })}
              rotulo="Não disponível (não temos essa informação)" />
            <TabelaDinamica nome="leitosPrivados" titulo="Leito" specs={LEITO_PRIVADO_SPEC} linhas={v.leitosPrivados}
              onChange={(l) => set({ leitosPrivados: l })} limite={MAX_LINHAS} erros={e}
              desabilitado={v.leitosPrivadosNaoDisponivel} />
          </Secao>

          <Secao id="b7-aparelhos" titulo="7.3 Conferência dos aparelhos de imagem cadastrados no CNES"
            nota="Confira se os tomógrafos e as ressonâncias que o CNES mostra no município existem, estão em uso e atendem o SUS. Aponte cadastros a corrigir, como aparelhos repetidos ou tomógrafos odontológicos registrados como tomógrafo de uso geral."
            ajuda={`Até ${MAX_LINHAS} linhas.`}>
            <NaoDisponivel marcado={v.aparelhosNaoDisponivel} onChange={(m) => set({ aparelhosNaoDisponivel: m })}
              rotulo="Não disponível (não temos essa informação)" />
            <TabelaDinamica nome="aparelhos" titulo="Aparelho" specs={APARELHO_SPEC} linhas={v.aparelhos}
              onChange={(l) => set({ aparelhos: l })} limite={MAX_LINHAS} erros={e}
              desabilitado={v.aparelhosNaoDisponivel} avisos={avisosAparelhos(v)} />
          </Secao>
        </>
      )}
    </BlocoLayout>
  );
}
