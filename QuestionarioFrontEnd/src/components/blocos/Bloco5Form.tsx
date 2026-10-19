"use client";

import BlocoLayout from "@/components/BlocoLayout";
import { Escolha, Marcaveis, NaoDisponivel, Secao, TabelaDinamica, TabelaFixa, Texto } from "@/components/campos/CamposBase";
import { useQuestionario } from "@/components/QuestionarioContext";
import { useBlocoQuestionario } from "@/hooks/useBlocoQuestionario";
import {
  MOTIVOS_SEM_VAGA, NECESSIDADES_PACIENTE, SIM_NAO_PARCIAL, TIPOS_UNIDADE_URGENCIA, type MotivoSemVaga,
} from "@/lib/rotulos";
import { deNumero, erroDeNumero, numeroOuNulo, textoOuNulo } from "@/lib/numeros";
import {
  dinamicasDeServidor, dinamicasParaServidor, fixasDeServidor, fixasParaServidor, marcarNaoDisp, num, validarLinhas,
  type CampoSpec, type FixasValores, type Linha,
} from "@/lib/tabelas";
import type { Bloco5Servidor } from "@/types";
import { Alert } from "@mui/material";

export const MAX_UNIDADES = 30;
const CHAVES_PERFIL = NECESSIDADES_PACIENTE.map((n) => n.valor);

export const UNIDADE_SPEC: CampoSpec[] = [
  { nome: "nome", rotulo: "Nome da unidade", tipo: "texto", max: 200, largo: true },
  { nome: "cnes", rotulo: "CNES (7 números)", tipo: "cnes" },
  { nome: "tipo", rotulo: "Tipo", tipo: "opcao", opcoes: TIPOS_UNIDADE_URGENCIA },
  { nome: "leitosObservacao", rotulo: "Leitos de observação", tipo: "inteiro" },
  { nome: "pacientes24h", rotulo: "Pacientes há mais de 24 h aguardando vaga", tipo: "inteiro" },
  { nome: "pacientes72h", rotulo: "Pacientes há mais de 72 h aguardando vaga", tipo: "inteiro" },
  { nome: "ventilacaoMecanica", rotulo: "Em ventilação mecânica", tipo: "inteiro" },
];

export const PERFIL_SPEC: CampoSpec[] = [
  { nome: "pacientes", rotulo: "Pacientes aguardando", tipo: "inteiro" },
  { nome: "diagnosticos", rotulo: "Principais diagnósticos (sem nome de paciente)", tipo: "texto", max: 300, largo: true },
];

export type Bloco5Valores = {
  unidades: Linha[];
  unidadesNaoDisponivel: boolean;
  dataExtracaoUnidades: string;
  perfil: FixasValores;
  perfilOutroDescricao: string;
  perfilNaoDisponivel: boolean;
  pacientesMais24h: string;
  tempoMedioPermanenciaHoras: string;
  obitosAguardandoVaga: string;
  altaSemProcedimento: string;
  registroInformatizado: string;
  indicadoresNaoDisponivel: boolean;
  motivos: MotivoSemVaga[];
  motivoOutro: string;
};

export function deServidorBloco5(r: Bloco5Servidor): Bloco5Valores {
  return {
    unidades: dinamicasDeServidor(UNIDADE_SPEC, r.unidades as unknown as Record<string, unknown>[]),
    unidadesNaoDisponivel: r.unidadesNaoDisponivel === true,
    dataExtracaoUnidades: r.dataExtracaoUnidades ?? "",
    perfil: fixasDeServidor(PERFIL_SPEC, CHAVES_PERFIL, "necessidade", r.perfil as unknown as Record<string, unknown>[]),
    perfilOutroDescricao: r.perfilOutroDescricao ?? "",
    perfilNaoDisponivel: r.perfilNaoDisponivel === true,
    pacientesMais24h: deNumero(r.pacientesMais24h, "inteiro"),
    tempoMedioPermanenciaHoras: deNumero(r.tempoMedioPermanenciaHoras, "decimal"),
    obitosAguardandoVaga: deNumero(r.obitosAguardandoVaga, "inteiro"),
    altaSemProcedimento: deNumero(r.altaSemProcedimento, "inteiro"),
    registroInformatizado: r.registroInformatizado ?? "",
    indicadoresNaoDisponivel: r.indicadoresNaoDisponivel === true,
    motivos: MOTIVOS_SEM_VAGA.map((m) => m.valor).filter((x) => r.motivos?.includes(x)),
    motivoOutro: r.motivoOutro ?? "",
  };
}

export function paraServidorBloco5(v: Bloco5Valores, version: number | null) {
  const ind = v.indicadoresNaoDisponivel;
  return {
    unidades: v.unidadesNaoDisponivel ? [] : dinamicasParaServidor(UNIDADE_SPEC, v.unidades),
    unidadesNaoDisponivel: v.unidadesNaoDisponivel,
    dataExtracaoUnidades: textoOuNulo(v.dataExtracaoUnidades),
    perfil: fixasParaServidor(PERFIL_SPEC, CHAVES_PERFIL, "necessidade", v.perfilNaoDisponivel
      ? { ...v.perfil, linhas: v.perfil.linhas.map((l) => Object.fromEntries(Object.keys(l).map((k) => [k, ""]))) } : v.perfil),
    perfilOutroDescricao: textoOuNulo(v.perfilOutroDescricao),
    perfilNaoDisponivel: v.perfilNaoDisponivel,
    pacientesMais24h: ind ? null : numeroOuNulo(v.pacientesMais24h, "inteiro"),
    tempoMedioPermanenciaHoras: ind ? null : numeroOuNulo(v.tempoMedioPermanenciaHoras, "decimal"),
    obitosAguardandoVaga: ind ? null : numeroOuNulo(v.obitosAguardandoVaga, "inteiro"),
    altaSemProcedimento: ind ? null : numeroOuNulo(v.altaSemProcedimento, "inteiro"),
    registroInformatizado: v.registroInformatizado || null,
    indicadoresNaoDisponivel: ind,
    motivos: v.motivos,
    motivoOutro: v.motivos.includes("OUTRO") ? textoOuNulo(v.motivoOutro) : null,
    version,
  };
}

export function validarBloco5(v: Bloco5Valores): Record<string, string> {
  const erros: Record<string, string> = {};
  if (v.perfilOutroDescricao.trim().length > 150) erros.perfilOutroDescricao = "Máximo de 150 caracteres";
  if (v.motivoOutro.trim().length > 300) erros.motivoOutro = "Máximo de 300 caracteres";
  if (!v.unidadesNaoDisponivel) Object.assign(erros, validarLinhas(UNIDADE_SPEC, v.unidades, "unidades"));
  if (!v.perfilNaoDisponivel) {
    Object.assign(erros, validarLinhas(PERFIL_SPEC, v.perfil.linhas.map((l, i) => (v.perfil.naoDisp[i] ? Object.fromEntries(Object.keys(l).map((k) => [k, ""])) : l)), "perfil"));
  }
  if (!v.indicadoresNaoDisponivel) {
    const e1 = erroDeNumero(v.pacientesMais24h, "inteiro"); if (e1) erros.pacientesMais24h = e1;
    const e2 = erroDeNumero(v.tempoMedioPermanenciaHoras, "decimal"); if (e2) erros.tempoMedioPermanenciaHoras = e2;
    const e3 = erroDeNumero(v.obitosAguardandoVaga, "inteiro"); if (e3) erros.obitosAguardandoVaga = e3;
    const e4 = erroDeNumero(v.altaSemProcedimento, "inteiro"); if (e4) erros.altaSemProcedimento = e4;
  }
  return erros;
}

export function avisosBloco5(v: Bloco5Valores): string[] {
  if (v.unidadesNaoDisponivel) return [];
  const avisos: string[] = [];
  v.unidades.forEach((u, i) => {
    const a = num(u.pacientes24h);
    const b = num(u.pacientes72h);
    if (a !== null && b !== null && b > a) avisos.push(`Unidade ${i + 1}: pacientes há mais de 72 h é maior que os há mais de 24 h`);
  });
  return avisos;
}

export default function Bloco5Form() {
  const { uuid, recarregarQuestionario } = useQuestionario();
  const motor = useBlocoQuestionario<Bloco5Valores, Bloco5Servidor>({
    uuid, numero: 5, deServidor: deServidorBloco5, paraServidor: paraServidorBloco5, validar: validarBloco5,
    aoSalvar: () => void recarregarQuestionario(),
  });
  const { valores: v, alterar, errosCampos: e } = motor;
  const set = (parte: Partial<Bloco5Valores>) => alterar((a) => ({ ...a, ...parte }));
  const ind = v?.indicadoresNaoDisponivel ?? false;

  return (
    <BlocoLayout numero={5} motor={motor}>
      {v && (
        <>
          <Alert severity="info" sx={{ mb: 4 }}>
            Não escreva nome, CPF, prontuário ou qualquer dado que identifique um paciente. Aqui só entram números e
            descrições gerais (LGPD).
          </Alert>

          <Secao id="b5-unidades" titulo="5.1 Unidades de urgência"
            ajuda={`Até ${MAX_UNIDADES} unidades. Os números vêm do levantamento da data abaixo.`}>
            <Texto rotulo="Data do levantamento" tipo="date" valor={v.dataExtracaoUnidades}
              onChange={(t) => set({ dataExtracaoUnidades: t })} erro={e.dataExtracaoUnidades} />
            <NaoDisponivel marcado={v.unidadesNaoDisponivel} onChange={(m) => set({ unidadesNaoDisponivel: m })}
              rotulo="Não disponível (não temos esses números)" />
            <TabelaDinamica nome="unidades" titulo="Unidade" specs={UNIDADE_SPEC} linhas={v.unidades}
              onChange={(l) => set({ unidades: l })} limite={MAX_UNIDADES} erros={e}
              desabilitado={v.unidadesNaoDisponivel} avisos={avisosBloco5(v)} />
          </Secao>

          <Secao id="b5-perfil" titulo="5.2 Do que os pacientes aguardando vaga precisam"
            ajuda="Conte os pacientes que estão aguardando vaga no levantamento, por necessidade.">
            <NaoDisponivel marcado={v.perfilNaoDisponivel} onChange={(m) => set({ perfilNaoDisponivel: m })}
              rotulo="Não disponível (não temos esses números)" />
            <TabelaFixa
              nome="perfil" specs={PERFIL_SPEC}
              infos={NECESSIDADES_PACIENTE.map((n) => ({ chave: n.valor, titulo: n.rotulo }))}
              linhas={v.perfil.linhas} naoDisp={v.perfil.naoDisp}
              onChange={(linhas) => set({ perfil: { ...v.perfil, linhas } })}
              onNaoDisp={(i, m) => set({ perfil: marcarNaoDisp(PERFIL_SPEC, v.perfil, i, m) })}
              erros={e} desabilitado={v.perfilNaoDisponivel}
              extra={(chave) => chave === "OUTRO" ? (
                <Texto rotulo="Qual é a outra necessidade?" valor={v.perfilOutroDescricao} max={150}
                  onChange={(t) => set({ perfilOutroDescricao: t })} erro={e.perfilOutroDescricao} desabilitado={v.perfilNaoDisponivel} />
              ) : null}
            />
          </Secao>

          <Secao id="b5-indicadores" titulo="5.3 Indicadores">
            <NaoDisponivel marcado={ind} onChange={(m) => set({ indicadoresNaoDisponivel: m })}
              rotulo="Não disponível (não temos esses números)" />
            <div style={{ display: "grid", gap: 16, gridTemplateColumns: "repeat(auto-fit, minmax(240px, 1fr))", marginTop: 8 }}>
              <Texto rotulo="Pacientes há mais de 24 h aguardando vaga" valor={v.pacientesMais24h} desabilitado={ind}
                onChange={(t) => set({ pacientesMais24h: t })} erro={e.pacientesMais24h} />
              <Texto rotulo="Tempo médio de permanência (horas)" valor={v.tempoMedioPermanenciaHoras} desabilitado={ind}
                onChange={(t) => set({ tempoMedioPermanenciaHoras: t })} erro={e.tempoMedioPermanenciaHoras} />
              <Texto rotulo="Óbitos aguardando vaga no período" valor={v.obitosAguardandoVaga} desabilitado={ind}
                onChange={(t) => set({ obitosAguardandoVaga: t })} erro={e.obitosAguardandoVaga} />
              <Texto rotulo="Altas sem o procedimento necessário" valor={v.altaSemProcedimento} desabilitado={ind}
                onChange={(t) => set({ altaSemProcedimento: t })} erro={e.altaSemProcedimento} />
            </div>
            <Escolha rotulo="O registro dessas permanências é informatizado?" opcoes={SIM_NAO_PARCIAL}
              valor={v.registroInformatizado} onChange={(t) => set({ registroInformatizado: t })} erro={e.registroInformatizado} />
          </Secao>

          <Secao id="b5-motivos" titulo="5.4 Por que o paciente fica sem vaga">
            <Marcaveis rotulo="Marque os motivos mais frequentes" opcoes={MOTIVOS_SEM_VAGA} valor={v.motivos}
              onChange={(m) => set({ motivos: m })} erro={e.motivos} />
            {v.motivos.includes("OUTRO") && (
              <Texto rotulo="Qual é o outro motivo?" valor={v.motivoOutro} max={300}
                onChange={(t) => set({ motivoOutro: t })} erro={e.motivoOutro} />
            )}
          </Secao>
        </>
      )}
    </BlocoLayout>
  );
}
