"use client";

import { BLOCOS, PASSO_REVISAO } from "@/lib/blocos";
import { assinarRascunhos, blocosComRascunho } from "@/lib/rascunhoLocal";
import type { BlocoSituacao } from "@/types";
import CheckIcon from "@mui/icons-material/Check";
import { Box, Typography } from "@mui/material";
import Link from "next/link";
import { useSyncExternalStore } from "react";

type Props = {
  uuid: string;
  passoAtual: number | typeof PASSO_REVISAO;
  situacao: BlocoSituacao[] | null;
  /** Blocos com pendência no servidor (null: não foi possível saber). */
  incompletos: number[] | null;
  enviado: boolean;
};

const VERDE_FUNDO = "#1D4A3A";
const VERDE_ATIVO = "rgba(255,255,255,0.10)";

/** Menu lateral (telas grandes): a marca, os 8 blocos e a revisão, cada um com a sua situação. */
export default function QuestionarioMenu({ uuid, passoAtual, situacao, incompletos, enviado }: Props) {
  const pendentesTexto = useSyncExternalStore(
    assinarRascunhos,
    () => blocosComRascunho(uuid).join(","),
    () => "",
  );
  const pendentes = pendentesTexto ? pendentesTexto.split(",").map(Number) : [];
  const salvo = (n: number) => situacao?.find((b) => b.numero === n)?.version != null;
  // "Feito" = salvo e sem nenhuma pendência; salvo com pendência é "em andamento".
  const feito = (n: number) => salvo(n) && !(incompletos ?? []).includes(n);

  const itens = [
    ...BLOCOS.map((b) => ({
      id: b.numero as number | typeof PASSO_REVISAO, rotulo: b.titulo, numero: String(b.numero),
      estado: pendentes.includes(b.numero) ? "pendente" : feito(b.numero) ? "salvo" : salvo(b.numero) ? "andamento" : "vazio",
    })),
    { id: PASSO_REVISAO as number | typeof PASSO_REVISAO, rotulo: "Revisão e envio", numero: "9", estado: enviado ? "salvo" : "vazio" },
  ];

  return (
    <Box
      component="nav"
      aria-label="Passos do questionário"
      sx={{ bgcolor: VERDE_FUNDO, color: "#E7F0EB", display: "flex", flexDirection: "column", height: "100%", p: 3, overflowY: "auto" }}
    >
      <Box sx={{ display: "flex", alignItems: "center", gap: 1.5, mb: 5 }}>
        <Box sx={{ bgcolor: "#fff", borderRadius: 1.5, p: 0.5, display: "flex" }}>
          <Box component="img" src="/cisbaf.png" alt="CISBAF" sx={{ height: 40, width: 40, objectFit: "contain" }} />
        </Box>
        <Box>
          <Typography sx={{ fontWeight: 800, fontSize: 22, lineHeight: 1.1, letterSpacing: "-0.02em", color: "#fff" }}>CISBAF</Typography>
          <Typography sx={{ fontSize: 9.5, fontWeight: 700, letterSpacing: "0.08em", textTransform: "uppercase", color: "#BFD3C8" }}>
            Observatório Regional de Saúde
          </Typography>
        </Box>
      </Box>

      <Typography sx={{ fontSize: 11, fontWeight: 700, letterSpacing: "0.14em", color: "#9DB8AA", mb: 1.5, px: 1 }}>
        PLANEJAMENTO REGIONAL
      </Typography>

      <Box component="ol" sx={{ listStyle: "none", m: 0, p: 0, display: "grid", gap: 0.5 }}>
        {itens.map((item) => {
          const ativo = item.id === passoAtual;
          return (
            <li key={item.id}>
              <Box
                component={Link}
                href={`/questionario/${uuid}/${item.id}`}
                aria-current={ativo ? "step" : undefined}
                aria-label={`${item.id === PASSO_REVISAO ? "" : `Bloco ${item.numero}: `}${item.rotulo}. ${item.estado === "salvo" ? "Completo" : item.estado === "andamento" ? "Em andamento, com pendências" : item.estado === "pendente" ? "Alterações não salvas" : "Não iniciado"}`}
                sx={{
                  display: "flex", alignItems: "center", gap: 1.5, px: 1.5, py: 1.25, borderRadius: 2,
                  bgcolor: ativo ? VERDE_ATIVO : "transparent", color: ativo ? "#fff" : "#BFD3C8",
                  "&:hover": { bgcolor: ativo ? VERDE_ATIVO : "rgba(255,255,255,0.06)", color: "#fff" },
                }}
              >
                <Box
                  sx={{
                    flex: "none", width: 26, height: 26, borderRadius: "50%", display: "grid", placeItems: "center",
                    fontSize: 12, fontWeight: 700,
                    bgcolor: ativo ? "#CFE3A4" : item.estado === "salvo" ? "#3E7A5F" : "transparent",
                    color: ativo ? "#1D4A3A" : "#E7F0EB",
                    border: ativo || item.estado === "salvo" ? "none" : "1.5px solid rgba(255,255,255,0.35)",
                    ...(item.estado === "pendente" && !ativo ? { borderColor: "#F5B700", color: "#F5B700" } : {}),
                    ...(item.estado === "andamento" && !ativo ? { borderColor: "#CFE3A4", color: "#CFE3A4", borderStyle: "dashed" } : {}),
                  }}
                >
                  {item.estado === "salvo" && !ativo ? <CheckIcon sx={{ fontSize: 16 }} /> : item.numero}
                </Box>
                <Typography sx={{ fontSize: 14, fontWeight: ativo ? 700 : 500, lineHeight: 1.3 }}>{item.rotulo}</Typography>
              </Box>
            </li>
          );
        })}
      </Box>

      <Box sx={{ mt: "auto", pt: 4, px: 1 }}>
        <Typography sx={{ fontWeight: 700, fontSize: 14, color: "#fff" }}>Baixada Fluminense</Typography>
        <Typography sx={{ fontSize: 12, color: "#9DB8AA" }}>Uma visão conjunta da rede de saúde.</Typography>
      </Box>
    </Box>
  );
}
