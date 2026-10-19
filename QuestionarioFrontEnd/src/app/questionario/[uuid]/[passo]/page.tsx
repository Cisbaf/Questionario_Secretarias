"use client";

import Bloco1Form from "@/components/blocos/Bloco1Form";
import Bloco2Form from "@/components/blocos/Bloco2Form";
import Bloco3Form from "@/components/blocos/Bloco3Form";
import Bloco4Form from "@/components/blocos/Bloco4Form";
import Bloco5Form from "@/components/blocos/Bloco5Form";
import Bloco6Form from "@/components/blocos/Bloco6Form";
import Bloco7Form from "@/components/blocos/Bloco7Form";
import Bloco8Form from "@/components/blocos/Bloco8Form";
import RevisaoEnvio from "@/components/RevisaoEnvio";
import { interpretarPasso } from "@/lib/blocos";
import { Alert, Button } from "@mui/material";
import Link from "next/link";
import { useParams } from "next/navigation";

export default function PassoPage() {
  const { uuid, passo } = useParams<{ uuid: string; passo: string }>();
  const atual = interpretarPasso(passo);

  if (atual === null) {
    return (
      <Alert severity="warning" action={<Button component={Link} href={`/questionario/${uuid}/1`} color="inherit" size="small">Ir ao Bloco 1</Button>}>
        Passo não encontrado.
      </Alert>
    );
  }
  if (atual === 1) return <Bloco1Form key={uuid} />;
  if (atual === 2) return <Bloco2Form key={uuid} />;
  if (atual === 3) return <Bloco3Form key={uuid} />;
  if (atual === 4) return <Bloco4Form key={uuid} />;
  if (atual === 5) return <Bloco5Form key={uuid} />;
  if (atual === 6) return <Bloco6Form key={uuid} />;
  if (atual === 7) return <Bloco7Form key={uuid} />;
  if (atual === 8) return <Bloco8Form key={uuid} />;
  return <RevisaoEnvio key={uuid} />;
}
