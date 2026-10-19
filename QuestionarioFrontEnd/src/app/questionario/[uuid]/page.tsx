import { redirect } from "next/navigation";

/** /questionario/{uuid} abre o primeiro bloco. O endereço de cada passo é /questionario/{uuid}/{1..8|revisao}. */
export default async function QuestionarioRaiz({ params }: { params: Promise<{ uuid: string }> }) {
  const { uuid } = await params;
  redirect(`/questionario/${encodeURIComponent(uuid)}/1`);
}
