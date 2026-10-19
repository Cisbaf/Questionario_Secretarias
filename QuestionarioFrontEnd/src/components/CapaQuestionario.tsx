import { A_DEFINIR, formatarData, formatarPeriodo } from "@/lib/formatters";
import type { Levantamento } from "@/types";
import { Box, Paper, Typography } from "@mui/material";

const TITULO_PADRAO = "Questionário às Secretarias Municipais de Saúde";

/** Título e os quatro campos da capa do documento (período, data do censo, prazo e para onde enviar). */
export function CabecalhoDoQuestionario({ levantamento }: { levantamento: Levantamento | null }) {
  return (
    <Box sx={{ mb: 3 }}>
      <Typography variant="h4" component="h1" gutterBottom>
        {levantamento?.titulo ?? TITULO_PADRAO}
      </Typography>
      <Typography variant="h6" component="p" sx={{ fontWeight: 600 }}>
        Prioridades de atenção à saúde, filas de regulação e pacientes sem vaga nas portas de urgência
      </Typography>
      <Typography color="text.secondary" sx={{ fontStyle: "italic" }}>
        Levantamento para o planejamento regional da Baixada Fluminense
      </Typography>
    </Box>
  );
}

function Campo({ rotulo, valor }: { rotulo: string; valor: string | null }) {
  return (
    <Box>
      <Typography variant="caption" color="text.secondary" sx={{ fontWeight: 700, textTransform: "uppercase", letterSpacing: "0.04em" }}>
        {rotulo}
      </Typography>
      <Typography sx={valor ? undefined : { color: "text.disabled", fontStyle: "italic" }}>
        {valor ?? A_DEFINIR}
      </Typography>
    </Box>
  );
}

export function QuadroDeDatas({ levantamento }: { levantamento: Levantamento }) {
  return (
    <Paper variant="outlined" sx={{ p: 2.5, mb: 3 }}>
      <Box sx={{ display: "grid", gap: 2.5, gridTemplateColumns: { xs: "1fr", sm: "1fr 1fr", md: "repeat(4, 1fr)" } }}>
        <Campo rotulo="Período de referência" valor={levantamento.periodoReferenciaInicio || levantamento.periodoReferenciaFim ? formatarPeriodo(levantamento.periodoReferenciaInicio, levantamento.periodoReferenciaFim) : null} />
        <Campo rotulo="Data do censo nas UPAs (Bloco 5)" valor={formatarData(levantamento.dataCensoUpa)} />
        <Campo rotulo="Prazo de devolução" valor={formatarData(levantamento.prazoDevolucao)} />
        <Campo rotulo="Enviar para" valor={levantamento.enviarPara} />
      </Box>
    </Paper>
  );
}

/** Os textos "Por que estamos pedindo estas informações" e "Como preencher", do documento. */
export function InstrucoesDoQuestionario() {
  return (
    <Box>
      <Typography variant="h6" component="h2" gutterBottom>
        Por que estamos pedindo estas informações
      </Typography>
      <Typography sx={{ mb: 2 }}>
        A equipe técnica do CISBAF está preparando uma proposta para a organização da rede de atenção à saúde da Baixada
        Fluminense. Os sistemas nacionais já registram leitos, internações, óbitos e financiamento, mas deixam de fora
        uma parte importante da demanda. O SIH registra apenas a internação que ocorreu; o paciente que passa dias na
        sala de observação da UPA aguardando vaga, ou que está há meses na fila de uma consulta ou cirurgia, não aparece
        em nenhuma base pública. Também não há base pública com os repasses estaduais de custeio por município e por
        unidade.
      </Typography>
      <Typography sx={{ mb: 2 }}>
        Este questionário busca justamente essa informação, que só os municípios têm. Depois de consolidadas, as
        respostas vão subsidiar a proposta inicial: quais linhas de cuidado priorizar, onde faltam leitos e serviços e
        como organizar a regulação entre os municípios. A proposta será apresentada aos secretários para discussão antes
        de seguir às instâncias de pactuação.
      </Typography>

      <Typography variant="h6" component="h2" gutterBottom sx={{ mt: 4 }}>
        Como preencher
      </Typography>
      <Box component="ul" sx={{ pl: 3, m: 0, "& li": { mb: 1.25 } }}>
        <li>
          <Typography>
            O questionário tem oito blocos. O Bloco 2 deve ser respondido pelo(a) próprio(a) secretário(a); os demais
            podem ser preenchidos pelas equipes de regulação, urgência e planejamento.
          </Typography>
        </li>
        <li>
          <Typography>
            <strong>Envie apenas dados agregados ou sem identificação.</strong> Não informe nome, CPF, Cartão Nacional
            de Saúde, número de prontuário ou endereço de paciente (Lei nº 13.709/2018, LGPD).
          </Typography>
        </li>
        <li>
          <Typography>
            Quando a informação não existir ou não puder ser obtida, indique &ldquo;não disponível&rdquo;. Isso também é
            um dado importante para o planejamento.
          </Typography>
        </li>
        <li>
          <Typography>Informe sempre a data em que cada dado foi extraído do sistema.</Typography>
        </li>
        <li>
          <Typography>
            Ao iniciar, o sistema gera um link do seu questionário. Guarde-o: é por ele que você, ou outra pessoa da
            sua equipe, volta ao questionário depois.
          </Typography>
        </li>
      </Box>
    </Box>
  );
}
