/** Os 8 blocos do documento, na ordem. {@code etapa}: em que etapa do plano cada bloco entra (`pronto`: os blocos 1 e 2 já existem). */
export type InfoBloco = { numero: number; titulo: string; descricao: string; nota?: string; introducao?: string[]; etapa: string; pronto: boolean };

export const BLOCOS: InfoBloco[] = [
  { numero: 1, titulo: "Identificação", descricao: "Identifique o município e a equipe responsável pelo preenchimento, e envie a planilha de coleta preenchida.", etapa: "E03", pronto: true },
  { numero: 2, titulo: "Prioridades de atenção à saúde", descricao: "Conte quais são as maiores prioridades de saúde do município e o que trava a solução de cada uma.", nota: "Resposta do(a) secretário(a).", etapa: "E04", pronto: true },
  { numero: 3, titulo: "Fila da regulação ambulatorial", descricao: "Informe as filas de consultas e exames especializados que passam pela regulação.", etapa: "E05", pronto: true },
  { numero: 4, titulo: "Regulação hospitalar", descricao: "Informe os pedidos de leito hospitalar e as cirurgias e procedimentos eletivos que aguardam vaga.", etapa: "E06", pronto: true },
  { numero: 5, titulo: "UPAs e prontos-socorros sem vaga", descricao: "Informe como estão as UPAs e prontos-socorros: pacientes aguardando vaga, perfil e motivos.", introducao: ["Queremos conhecer quem são os pacientes que ficam retidos nas portas de urgência à espera de leito ou de procedimento. Para isso, pedimos um censo: na data combinada (ver quadro inicial), às 8 h, cada unidade registra todos os pacientes que estão em observação ou internados aguardando transferência há mais de 24 horas.", "O registro paciente a paciente, sem identificação, vai na aba “4_Censo_UPA” da planilha. Aqui pedimos o resumo por unidade e a visão do gestor."], etapa: "E07", pronto: true },
  { numero: 6, titulo: "Seguimento após a urgência", descricao: "Informe quantos pacientes aguardam o procedimento de seguimento depois da urgência.", introducao: ["Muitos pacientes saem da UPA ou do hospital precisando de um procedimento que demora: a cirurgia da fratura, o cateterismo depois do infarto, a biópsia ou a quimioterapia, a fístula para hemodiálise, o leito de cuidados paliativos. Informe o que o município conseguir levantar."], etapa: "E08", pronto: true },
  { numero: 7, titulo: "Financiamento e capacidade", descricao: "Informe repasses às unidades, leitos em hospitais privados e filantrópicos e aparelhos de imagem.", nota: "Estas perguntas ajudam a separar, nas contas do plano, o dinheiro que já chega ao município do que seria recurso novo, e a conferir dados de cadastro que aparecem nas bases nacionais.", etapa: "E09", pronto: true },
  { numero: 8, titulo: "Observações finais", descricao: "Deixe observações, indique documentos que podem ser enviados depois e assine.", etapa: "E10", pronto: true },
];

export const PASSO_REVISAO = "revisao";

/** "1".."8" ou "revisao"; qualquer outra coisa não é um passo. */
export function interpretarPasso(passo: string): number | typeof PASSO_REVISAO | null {
  if (passo === PASSO_REVISAO) return PASSO_REVISAO;
  return /^[1-8]$/.test(passo) ? Number(passo) : null;
}
