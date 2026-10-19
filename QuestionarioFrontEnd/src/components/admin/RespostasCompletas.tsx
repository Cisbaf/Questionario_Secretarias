"use client";

import { formatarData } from "@/lib/formatters";
import {
  APARELHOS_IMAGEM, CENTRAIS_REGULACAO, DOCUMENTOS_ANEXAVEIS, IMPEDIMENTOS, LINHAS_CUIDADO, MOTIVOS_SEM_VAGA,
  NECESSIDADES_PACIENTE, ORIGENS_DADO, RESOLVE_SOZINHO, SIM_NAO_EM_PARTE, SIM_NAO_PARCIAL, SISTEMAS_REGULACAO,
  SITUACOES_CLINICAS, TEMAS_URGENCIA, TIPOS_FILA, TIPOS_LEITO, TIPOS_LEITO_PRIVADO, TIPOS_UNIDADE_URGENCIA, rotuloDe,
} from "@/lib/rotulos";
import type { Respostas } from "@/types";
import {
  Box, Chip, Paper, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Typography,
} from "@mui/material";
import type { ReactNode } from "react";

const NAO_INFORMADO = <Typography component="span" color="text.disabled" sx={{ fontStyle: "italic" }}>não informado</Typography>;
const NAO_DISPONIVEL = <Chip size="small" variant="outlined" label="Não disponível" />;

const num = (n: number | null | undefined, casas = 0): ReactNode =>
  n === null || n === undefined ? "—" : n.toLocaleString("pt-BR", { minimumFractionDigits: 0, maximumFractionDigits: casas });
const reais = (n: number | null | undefined): ReactNode =>
  n === null || n === undefined ? "—" : n.toLocaleString("pt-BR", { style: "currency", currency: "BRL" });
const txt = (s: string | null | undefined): ReactNode => (s && s.trim() ? s : "—");
const data = (s: string | null | undefined): ReactNode => formatarData(s) ?? "—";
const mesAno = (s: string | null | undefined): string | null => (s && /^\d{4}-\d{2}$/.test(s) ? `${s.slice(5)}/${s.slice(0, 4)}` : s ?? null);
const juntar = (itens: string[]) => itens.join("; ");

function Bloco({ numero, titulo, children }: { numero: number; titulo: string; children: ReactNode }) {
  return (
    <Paper variant="outlined" sx={{ p: { xs: 2, md: 3 }, mb: 3 }} component="section" aria-labelledby={`adm-b${numero}`}>
      <Typography id={`adm-b${numero}`} variant="h5" component="h2" sx={{ fontWeight: 800, mb: 2 }}>
        Bloco {numero} · {titulo}
      </Typography>
      {children}
    </Paper>
  );
}

function Sub({ children }: { children: ReactNode }) {
  return <Typography variant="subtitle1" component="h3" sx={{ fontWeight: 700, mt: 3, mb: 1, "&:first-of-type": { mt: 0 } }}>{children}</Typography>;
}

/** Pergunta e resposta em texto. */
function Campo({ rotulo, children }: { rotulo: string; children: ReactNode }) {
  return (
    <Box sx={{ mb: 1.5 }}>
      <Typography variant="caption" color="text.secondary" sx={{ fontWeight: 700, textTransform: "uppercase", letterSpacing: "0.04em" }}>{rotulo}</Typography>
      <Typography component="div" sx={{ whiteSpace: "pre-wrap" }}>{children === null || children === "" ? NAO_INFORMADO : children}</Typography>
    </Box>
  );
}

function Tabela({ colunas, linhas, vazio = "Nenhuma linha preenchida." }: { colunas: string[]; linhas: ReactNode[][]; vazio?: string }) {
  if (linhas.length === 0) return <Typography color="text.secondary" sx={{ fontStyle: "italic" }}>{vazio}</Typography>;
  return (
    <TableContainer>
      <Table size="small">
        <TableHead>
          <TableRow>{colunas.map((c) => <TableCell key={c}>{c}</TableCell>)}</TableRow>
        </TableHead>
        <TableBody>
          {linhas.map((l, i) => (
            <TableRow key={i}>{l.map((v, j) => <TableCell key={j} sx={{ verticalAlign: "top" }}>{v}</TableCell>)}</TableRow>
          ))}
        </TableBody>
      </Table>
    </TableContainer>
  );
}

/** Tabela com "Não disponível" no lugar das linhas quando a tabela inteira foi marcada assim. */
function TabelaOuNaoDisponivel({ naoDisponivel, ...resto }: { naoDisponivel: boolean; colunas: string[]; linhas: ReactNode[][] }) {
  return naoDisponivel ? <Box>{NAO_DISPONIVEL}</Box> : <Tabela {...resto} />;
}

const outro = (descricao: string | null | undefined, base = "Outro") => (descricao ? `${base}: ${descricao}` : base);

/** Todas as respostas dos 8 blocos em leitura, no mesmo roteiro do documento. */
export default function RespostasCompletas({ r }: { r: Respostas }) {
  const { bloco1: b1, bloco2: b2, bloco3: b3, bloco4: b4, bloco5: b5, bloco6: b6, bloco7: b7, bloco8: b8 } = r;

  return (
    <Box data-testid="respostas-completas">
      <Bloco numero={1} titulo="Identificação">
        <Box sx={{ display: "grid", gridTemplateColumns: { xs: "1fr", md: "1fr 1fr" }, columnGap: 4 }}>
          <Campo rotulo="Município">{b1.municipio}</Campo>
          <Campo rotulo="Secretário(a) Municipal de Saúde">{b1.secretario}</Campo>
          <Campo rotulo="Responsável pelo preenchimento">{b1.responsavel}</Campo>
          <Campo rotulo="Cargo / setor">{b1.cargoSetor}</Campo>
          <Campo rotulo="E-mail">{b1.email}</Campo>
          <Campo rotulo="Telefone / WhatsApp">{b1.telefone}</Campo>
          <Campo rotulo="Data do preenchimento">{formatarData(b1.dataPreenchimento)}</Campo>
        </Box>
      </Bloco>

      <Bloco numero={2} titulo="Prioridades de atenção à saúde">
        <Sub>2.1 Maiores prioridades de saúde</Sub>
        <Tabela colunas={["Nº", "Prioridade", "Qual problema ela responde", "O município resolve sozinho?"]}
          linhas={(b2.prioridades ?? []).map((p, i) => [i + 1, txt(p.titulo), txt(p.problema), rotuloDe(RESOLVE_SOZINHO, p.resolveSozinho) || "—"])
            .filter((l) => l.slice(1).some((v) => v !== "—"))} />
        <Sub>2.2 Grau de prioridade por linha de cuidado (1 a 5)</Sub>
        <Tabela colunas={["Linha de cuidado", "Grau"]}
          linhas={LINHAS_CUIDADO.map((l) => {
            const grau = b2.grausLinhaCuidado?.[l.valor] ?? null;
            const nome = l.valor === "OUTRA" ? outro(b2.linhaOutraDescricao, "Outra") : "detalhe" in l && l.detalhe ? `${l.rotulo} (${l.detalhe})` : l.rotulo;
            return [nome, num(grau)];
          }).filter((l, i) => LINHAS_CUIDADO[i].valor !== "OUTRA" || l[1] !== "—" || b2.linhaOutraDescricao)} />
        <Sub>2.3 Maiores gargalos de acesso</Sub>
        <Campo rotulo="Resposta">{b2.gargalos}</Campo>
        <Sub>2.4 Serviços para atender também os municípios vizinhos</Sub>
        <Campo rotulo="Resposta">{b2.servicosParaVizinhos}</Campo>
        <Sub>2.5 O que mais impede ampliar a oferta</Sub>
        <Campo rotulo="Marcados">
          {(b2.impedimentos ?? []).length === 0 ? null
            : juntar((b2.impedimentos ?? []).map((i) => (i === "OUTRO" ? outro(b2.impedimentoOutro) : rotuloDe(IMPEDIMENTOS, i))))}
        </Campo>
        <Sub>2.6 Ordem dos temas por urgência (1 = mais urgente)</Sub>
        <Tabela colunas={["Tema", "Ordem"]}
          linhas={TEMAS_URGENCIA.map((t) => [t.rotulo, num(b2.ordemTemas?.[t.valor] ?? null)])} />
      </Bloco>

      <Bloco numero={3} titulo="Fila da regulação ambulatorial">
        <Sub>3.1 e 3.2 Sistemas e fila</Sub>
        <Campo rotulo="Sistemas de regulação">
          {(b3.sistemas ?? []).length === 0 ? null
            : juntar((b3.sistemas ?? []).map((s) => (s === "PROPRIO" ? outro(b3.sistemaProprioNome, rotuloDe(SISTEMAS_REGULACAO, s))
              : s === "OUTRO" ? outro(b3.sistemaOutroNome, rotuloDe(SISTEMAS_REGULACAO, s)) : rotuloDe(SISTEMAS_REGULACAO, s))))}
        </Campo>
        <Campo rotulo="A fila é única ou por unidade?">{rotuloDe(TIPOS_FILA, b3.tipoFila) || null}</Campo>
        <Campo rotulo="Classificação de risco ou prioridade clínica?">{rotuloDe(SIM_NAO_EM_PARTE, b3.classificacaoRisco) || null}</Campo>
        <Campo rotulo="Última revisão da fila (mês/ano)">{mesAno(b3.ultimaRevisao)}</Campo>
        <Sub>3.3 Pacientes aguardando, por especialidade ou procedimento (extração: {data(b3.dataExtracao)})</Sub>
        <TabelaOuNaoDisponivel naoDisponivel={b3.filasNaoDisponivel}
          colunas={["Especialidade / exame", "SIGTAP", "Total aguardando", "Prioridade alta", "Espera média (dias)", "Maior espera (dias)", "Há mais de 180 dias"]}
          linhas={(b3.filas ?? []).map((f) => [txt(f.especialidade), txt(f.sigtap), num(f.totalAguardando), num(f.prioridadeAlta), num(f.esperaMediaDias, 2), num(f.maiorEsperaDias), num(f.aguardandoMais180)])} />
      </Bloco>

      <Bloco numero={4} titulo="Regulação hospitalar">
        <Sub>4.1 Solicitações de internação de urgência, por tipo de leito (extração: {data(b4.dataExtracaoLeitos)})</Sub>
        <TabelaOuNaoDisponivel naoDisponivel={b4.leitosNaoDisponivel}
          colunas={["Tipo de leito", "Solicitações", "Atendidas", "Canceladas ou óbito", "Tempo médio até a vaga (h)", "Aguardando"]}
          linhas={(b4.leitos ?? []).flatMap((l): ReactNode[][] => {
            const nome = l.tipo === "OUTRO" ? outro(b4.leitoOutroDescricao) : rotuloDe(TIPOS_LEITO, l.tipo);
            if (l.naoDisponivel) return [[nome, NAO_DISPONIVEL, NAO_DISPONIVEL, NAO_DISPONIVEL, NAO_DISPONIVEL, NAO_DISPONIVEL]];
            if ([l.solicitacoes, l.atendidas, l.canceladas, l.tempoMedioHoras, l.aguardando].every((v) => v === null)) return [];
            return [[nome, num(l.solicitacoes), num(l.atendidas), num(l.canceladas), num(l.tempoMedioHoras, 2), num(l.aguardando)]];
          })} />
        <Campo rotulo="Central(is) que recebe(m) os pedidos">
          {(b4.centrais ?? []).length === 0 ? null
            : juntar((b4.centrais ?? []).map((c) => (c === "OUTRA" ? outro(b4.centralOutra, "Outra") : rotuloDe(CENTRAIS_REGULACAO, c))))}
        </Campo>
        <Sub>4.2 Cirurgias eletivas e procedimentos programados (extração: {data(b4.dataExtracaoEletivas)})</Sub>
        <TabelaOuNaoDisponivel naoDisponivel={b4.eletivasNaoDisponivel}
          colunas={["Especialidade / procedimento", "SIGTAP", "Total aguardando", "Espera média (dias)", "Maior espera (dias)", "Origem do dado"]}
          linhas={(b4.eletivas ?? []).map((e) => [txt(e.especialidade), txt(e.sigtap), num(e.totalAguardando), num(e.esperaMediaDias, 2), num(e.maiorEsperaDias),
            e.origem === "OUTRO" ? outro(e.origemOutro) : rotuloDe(ORIGENS_DADO, e.origem) || "—"])} />
      </Bloco>

      <Bloco numero={5} titulo="Pacientes nas UPAs e prontos-socorros sem vaga">
        <Sub>5.1 Unidades de urgência (extração: {data(b5.dataExtracaoUnidades)})</Sub>
        <TabelaOuNaoDisponivel naoDisponivel={b5.unidadesNaoDisponivel}
          colunas={["Unidade", "CNES", "Tipo", "Leitos de observação", "Pacientes há mais de 24 h", "Destes, há mais de 72 h", "Ventilação mecânica"]}
          linhas={(b5.unidades ?? []).map((u) => [txt(u.nome), txt(u.cnes), rotuloDe(TIPOS_UNIDADE_URGENCIA, u.tipo) || "—", num(u.leitosObservacao), num(u.pacientes24h), num(u.pacientes72h), num(u.ventilacaoMecanica)])} />
        <Sub>5.2 Perfil dos pacientes retidos</Sub>
        <TabelaOuNaoDisponivel naoDisponivel={b5.perfilNaoDisponivel}
          colunas={["O que precisam", "Nº de pacientes", "Principais diagnósticos (CID-10)"]}
          linhas={(b5.perfil ?? []).flatMap((p): ReactNode[][] => {
            const nome = p.necessidade === "OUTRO" ? outro(b5.perfilOutroDescricao) : rotuloDe(NECESSIDADES_PACIENTE, p.necessidade);
            if (p.naoDisponivel) return [[nome, NAO_DISPONIVEL, NAO_DISPONIVEL]];
            if (p.pacientes === null && !p.diagnosticos) return [];
            return [[nome, num(p.pacientes), txt(p.diagnosticos)]];
          })} />
        <Sub>5.3 No período de referência</Sub>
        {b5.indicadoresNaoDisponivel ? <Box>{NAO_DISPONIVEL}</Box> : (
          <Tabela colunas={["Indicador", "Resposta"]} linhas={[
            ["Pacientes que ficaram mais de 24 h em observação aguardando vaga", num(b5.pacientesMais24h)],
            ["Tempo médio de permanência desses pacientes (horas)", num(b5.tempoMedioPermanenciaHoras, 2)],
            ["Óbitos de pacientes que aguardavam vaga ou procedimento", num(b5.obitosAguardandoVaga)],
            ["Pacientes que receberam alta da UPA sem o procedimento indicado", num(b5.altaSemProcedimento)],
            ["A unidade tem registro informatizado do tempo de permanência?", rotuloDe(SIM_NAO_PARCIAL, b5.registroInformatizado) || "—"],
          ]} />
        )}
        <Sub>5.4 Por que esses pacientes não conseguem vaga</Sub>
        <Campo rotulo="Marcados">
          {(b5.motivos ?? []).length === 0 ? null
            : juntar((b5.motivos ?? []).map((m) => (m === "OUTRO" ? outro(b5.motivoOutro) : rotuloDe(MOTIVOS_SEM_VAGA, m))))}
        </Campo>
      </Bloco>

      <Bloco numero={6} titulo="Seguimento após a urgência">
        <TabelaOuNaoDisponivel naoDisponivel={b6.tabelaNaoDisponivel}
          colunas={["Situação clínica", "Procedimento de seguimento", "Pacientes aguardando", "Espera média (dias)", "Onde é feito hoje"]}
          linhas={(b6.situacoes ?? []).flatMap((s): ReactNode[][] => {
            const def = SITUACOES_CLINICAS.find((x) => x.valor === s.situacao);
            const outra = s.situacao === "OUTRO";
            const situacao = outra ? outro(s.situacaoOutra) : def?.situacao ?? s.situacao;
            const procedimento = outra ? txt(s.procedimentoOutro) : def?.procedimento ?? "";
            if (s.naoDisponivel) return [[situacao, procedimento, NAO_DISPONIVEL, NAO_DISPONIVEL, NAO_DISPONIVEL]];
            if (s.pacientesAguardando === null && s.esperaMediaDias === null && !s.ondeFeito) return [];
            return [[situacao, procedimento, num(s.pacientesAguardando), num(s.esperaMediaDias, 2), txt(s.ondeFeito)]];
          })} />
      </Bloco>

      <Bloco numero={7} titulo="Financiamento e capacidade">
        <Sub>7.1 Repasses estaduais de custeio</Sub>
        <TabelaOuNaoDisponivel naoDisponivel={b7.repassesNaoDisponivel}
          colunas={["Unidade ou serviço", "CNES", "Ato (número)", "Ato (data)", "Valor mensal", "Vigência (até)", "Repasse em dia?", "Observação"]}
          linhas={(b7.repasses ?? []).map((x) => [txt(x.unidade), txt(x.cnes), txt(x.atoNumero), data(x.atoData), reais(x.valorMensal), data(x.vigenciaAte),
            rotuloDe(SIM_NAO_PARCIAL, x.repasseEmDia) || "—", txt(x.observacao)])} />
        <Sub>7.2 Leitos de hospitais privados e filantrópicos</Sub>
        <TabelaOuNaoDisponivel naoDisponivel={b7.leitosPrivadosNaoDisponivel}
          colunas={["Hospital", "CNES", "Tipo de leito", "Já contratados pelo SUS", "Poderiam ser contratados", "Valor por diária", "Observação"]}
          linhas={(b7.leitosPrivados ?? []).map((x) => [txt(x.hospital), txt(x.cnes), rotuloDe(TIPOS_LEITO_PRIVADO, x.tipoLeito) || "—", num(x.contratadosSus), num(x.poderiamSerContratados), reais(x.valorDiaria), txt(x.observacao)])} />
        <Sub>7.3 Aparelhos de imagem cadastrados no CNES</Sub>
        <TabelaOuNaoDisponivel naoDisponivel={b7.aparelhosNaoDisponivel}
          colunas={["Estabelecimento", "CNES", "Aparelho", "Quantidade no CNES", "Em uso de fato", "Atende o SUS?", "Correção necessária"]}
          linhas={(b7.aparelhos ?? []).map((x) => [txt(x.estabelecimento), txt(x.cnes), rotuloDe(APARELHOS_IMAGEM, x.aparelho) || "—", num(x.quantidadeCnes), num(x.emUsoDeFato), rotuloDe(SIM_NAO_PARCIAL, x.atendeSus) || "—", txt(x.correcaoNecessaria)])} />
      </Bloco>

      <Bloco numero={8} titulo="Observações finais">
        <Sub>8.1 Algo que a equipe de planejamento precisa saber</Sub>
        <Campo rotulo="Resposta">{b8.observacoes}</Campo>
        <Sub>8.2 Documentos que podem ser anexados</Sub>
        <Campo rotulo="Marcados">{(b8.documentos ?? []).length === 0 ? null : juntar((b8.documentos ?? []).map((d) => rotuloDe(DOCUMENTOS_ANEXAVEIS, d)))}</Campo>
        <Sub>Assinatura</Sub>
        <Campo rotulo="Secretário(a) Municipal de Saúde">{b8.assinaturaNome}</Campo>
        <Campo rotulo="Data">{formatarData(b8.assinaturaData)}</Campo>
        <Campo rotulo="Declaração de que as informações são verdadeiras">{b8.declaracao ? "Marcada" : "Não marcada"}</Campo>
      </Bloco>
    </Box>
  );
}
