"use client";

import { ApiError, getJson, putJson } from "@/lib/api";
import { MENSAGEM_SEM_SERVIDOR, mensagemDeErro, naoEncontrado } from "@/lib/erros";
import { apagarRascunho, gravarRascunho, lerRascunho, type RascunhoLocal } from "@/lib/rascunhoLocal";
import type { Pendencia, RespostaDoBloco } from "@/types";
import { useSearchParams } from "next/navigation";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";

/**
 * Motor de um bloco do questionário: junta o que está no servidor, o rascunho do localStorage e o que a pessoa está
 * digitando. É construído uma vez e reaproveitado pelos 8 blocos; cada bloco só diz como converter seus campos.
 *
 * Regras (ver PLANO, "Desenho do salvamento por bloco"):
 *  - ao digitar, grava no localStorage com debounce de ~500 ms;
 *  - "Salvar" grava no localStorage de forma síncrona e SÓ DEPOIS chama o PUT com a versão em que a pessoa editou;
 *  - 2xx apaga o rascunho; qualquer erro (rede, 5xx, 400, 404, 409) MANTÉM o rascunho;
 *  - 409 de versão abre a escolha "recarregar do servidor" ou "sobrescrever" (D6).
 */

export type OpcoesBloco<T, R extends RespostaDoBloco> = {
  uuid: string;
  numero: number;
  /** Resposta do servidor -> valores do formulário. */
  deServidor: (resposta: R) => T;
  /** Valores do formulário -> corpo do PUT (inclui a version). */
  paraServidor: (valores: T, version: number | null) => unknown;
  /** Erros por campo (vazio = válido). Roda no cliente antes de enviar; o servidor valida de novo. */
  validar: (valores: T) => Record<string, string>;
  /** Chamado depois de um salvamento confirmado (o layout recarrega a situação dos blocos). */
  aoSalvar?: () => void;
};

export type Fase = "carregando" | "pronto" | "nao-encontrado" | "erro";

const ATRASO_GRAVACAO_LOCAL_MS = 500;

export function useBlocoQuestionario<T extends object, R extends RespostaDoBloco>(opcoes: OpcoesBloco<T, R>) {
  const { uuid, numero } = opcoes;
  const caminho = `/questionarios/${encodeURIComponent(uuid)}/blocos/${numero}`;

  // As funções do bloco mudam a cada render; guardá-las numa ref mantém os callbacks abaixo estáveis.
  const opcoesRef = useRef(opcoes);
  useEffect(() => {
    opcoesRef.current = opcoes;
  });

  const [fase, setFase] = useState<Fase>("carregando");
  const [erroCarga, setErroCarga] = useState("");
  const [valores, setValores] = useState<T | null>(null);
  const [servidor, setServidor] = useState<RespostaDoBloco>({ version: null, atualizadoEm: null });
  const [sujo, setSujo] = useState(false);
  const [offline, setOffline] = useState(false);
  const [decisaoRascunho, setDecisaoRascunho] = useState<RascunhoLocal<T> | null>(null);
  const [salvando, setSalvando] = useState(false);
  const [erroSalvar, setErroSalvar] = useState("");
  const [errosCampos, setErrosCampos] = useState<Record<string, string>>({});
  const [mostrarErros, setMostrarErros] = useState(false);
  const [conflito, setConflito] = useState("");
  const [semArmazenamento, setSemArmazenamento] = useState(false);
  // O que ainda falta neste bloco para poder enviar (vem do servidor, o mesmo que a tela de revisão usa).
  const [pendencias, setPendencias] = useState<Pendencia[]>([]);
  // Marca os campos em falta só depois que a pessoa pediu para ver (link da revisão) ou tentou salvar o bloco.
  const [tentouSalvar, setTentouSalvar] = useState(false);
  const pediuPendencias = useSearchParams().has("pendencias");
  const mostrarPendencias = tentouSalvar || pediuPendencias;

  // Espelhos síncronos do estado, para callbacks e para o que roda ao sair da página.
  const valoresRef = useRef<T | null>(null);
  const versaoBaseRef = useRef<number | null>(null);
  const sujoRef = useRef(false);
  const salvandoRef = useRef(false);
  const mostrarErrosRef = useRef(false);

  const aplicarValores = (proximos: T | null, marcarSujo: boolean) => {
    valoresRef.current = proximos;
    sujoRef.current = marcarSujo;
    setValores(proximos);
    setSujo(marcarSujo);
  };

  const carregarPendencias = useCallback(async () => {
    try {
      const todas = await getJson<Pendencia[]>(`/questionarios/${encodeURIComponent(uuid)}/pendencias`);
      setPendencias(todas.filter((p) => p.bloco === numero));
    } catch {
      // sem servidor: a lista só some; o resto do bloco continua funcionando
    }
  }, [uuid, numero]);

  const carregar = useCallback(async () => {
    setFase("carregando");
    setErroCarga("");
    const { deServidor } = opcoesRef.current;
    const rascunho = lerRascunho<T>(uuid, numero);
    try {
      const resposta = await getJson<R>(caminho);
      const doServidor = deServidor(resposta);
      setServidor({ version: resposta.version, atualizadoEm: resposta.atualizadoEm });
      versaoBaseRef.current = resposta.version;
      setOffline(false);
      void carregarPendencias();
      aplicarValores(doServidor, false);
      if (rascunho && rascunho.baseVersion === resposta.version
        && JSON.stringify(rascunho.data) === JSON.stringify(doServidor)) {
        apagarRascunho(uuid, numero); // sobra idêntica ao servidor: não há o que decidir
        setDecisaoRascunho(null);
      } else {
        setDecisaoRascunho(rascunho);
      }
      setFase("pronto");
    } catch (error) {
      if (naoEncontrado(error)) {
        setFase("nao-encontrado");
      } else if (rascunho) {
        // Servidor fora do ar, mas há rascunho: abre o formulário com ele, para a pessoa não ficar sem o que digitou.
        setOffline(true);
        setDecisaoRascunho(null);
        versaoBaseRef.current = rascunho.baseVersion;
        aplicarValores(rascunho.data, true);
        setFase("pronto");
      } else {
        setErroCarga(mensagemDeErro(error));
        setFase("erro");
      }
    }
  }, [uuid, numero, caminho, carregarPendencias]);

  useEffect(() => {
    void carregar();
  }, [carregar]);

  const persistirLocal = useCallback((): boolean => {
    if (!valoresRef.current) return true;
    return gravarRascunho(uuid, numero, versaoBaseRef.current, valoresRef.current);
  }, [uuid, numero]);

  // Debounce: enquanto a pessoa digita, o rascunho acompanha.
  useEffect(() => {
    if (!sujo || fase !== "pronto" || decisaoRascunho) return;
    const temporizador = setTimeout(() => setSemArmazenamento(!persistirLocal()), ATRASO_GRAVACAO_LOCAL_MS);
    return () => clearTimeout(temporizador);
  }, [valores, sujo, fase, decisaoRascunho, persistirLocal]);

  // Ao sair do bloco (trocar de passo) ou fechar a aba: grava o que ainda estava no debounce e avisa.
  useEffect(() => {
    const aoFechar = (evento: BeforeUnloadEvent) => {
      if (!sujoRef.current) return;
      persistirLocal();
      evento.preventDefault();
      evento.returnValue = "";
    };
    window.addEventListener("beforeunload", aoFechar);
    return () => {
      window.removeEventListener("beforeunload", aoFechar);
      if (sujoRef.current) persistirLocal();
    };
  }, [persistirLocal]);

  /** A pessoa mudou algum campo. */
  const alterar = useCallback((proximo: T | ((atual: T) => T)) => {
    const atual = valoresRef.current;
    if (!atual) return;
    const novo = typeof proximo === "function" ? (proximo as (atual: T) => T)(atual) : proximo;
    aplicarValores(novo, true);
    // Validação ao vivo: o erro aparece (e some) enquanto a pessoa digita, sem esperar o "Salvar".
    setErrosCampos(opcoesRef.current.validar(novo));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const salvar = useCallback(async (): Promise<boolean> => {
    const enviados = valoresRef.current;
    if (!enviados || salvandoRef.current) return false;
    const { validar, paraServidor, deServidor, aoSalvar } = opcoesRef.current;

    setTentouSalvar(true);
    // 1) local primeiro, sempre: mesmo com erro de validação ou de rede o que foi digitado fica guardado.
    const guardado = persistirLocal();
    setSemArmazenamento(!guardado);
    const sufixoGuardado = guardado ? " Suas alterações continuam guardadas neste navegador." : "";

    const erros = validar(enviados);
    mostrarErrosRef.current = true;
    setMostrarErros(true);
    setErrosCampos(erros);
    if (Object.keys(erros).length > 0) {
      setErroSalvar("Corrija os campos destacados para salvar.");
      return false;
    }

    salvandoRef.current = true;
    setSalvando(true);
    setErroSalvar("");
    try {
      // 2) só então o servidor, com a versão sobre a qual a pessoa editou.
      const resposta = await putJson<R>(caminho, paraServidor(enviados, versaoBaseRef.current));
      // 3) 2xx: o servidor tem tudo, então o rascunho local pode ir embora.
      apagarRascunho(uuid, numero);
      versaoBaseRef.current = resposta.version;
      setServidor({ version: resposta.version, atualizadoEm: resposta.atualizadoEm });
      setOffline(false);
      mostrarErrosRef.current = false;
      setMostrarErros(false);
      setErrosCampos({});
      if (valoresRef.current === enviados) {
        aplicarValores(deServidor(resposta), false); // mostra o que o servidor guardou (textos sem espaços nas pontas)
      } else {
        // A pessoa continuou digitando durante o salvamento: preserva o que ela digitou e o guarda de novo.
        setSemArmazenamento(!persistirLocal());
      }
      void carregarPendencias();
      aoSalvar?.();
      return true;
    } catch (error) {
      if (error instanceof ApiError && error.status === 400 && error.fields) {
        setErrosCampos(error.fields);
        mostrarErrosRef.current = true;
        setMostrarErros(true);
        setErroSalvar("Corrija os campos destacados para salvar.");
      } else if (error instanceof ApiError && error.status === 409 && error.code === "VERSAO_DESATUALIZADA") {
        setConflito(error.message);
      } else if (error instanceof ApiError && error.status === 404) {
        setErroSalvar("Questionário não encontrado. Confira o link.");
      } else if (error instanceof ApiError && error.status >= 400 && error.status < 500) {
        setErroSalvar(error.message);
      } else {
        setErroSalvar(`${MENSAGEM_SEM_SERVIDOR}${sufixoGuardado}`);
      }
      return false;
    } finally {
      salvandoRef.current = false;
      setSalvando(false);
    }
  }, [uuid, numero, caminho, persistirLocal, carregarPendencias]);

  /** Conflito de versão, opção 1: descarta o rascunho local e mostra o que está no servidor. */
  const recarregarDoServidor = useCallback(async () => {
    apagarRascunho(uuid, numero);
    setConflito("");
    setErroSalvar("");
    setErrosCampos({});
    mostrarErrosRef.current = false;
    setMostrarErros(false);
    await carregar();
  }, [uuid, numero, carregar]);

  /** Conflito de versão, opção 2: assume a versão atual do servidor e grava por cima com o que a pessoa digitou. */
  const sobrescrever = useCallback(async () => {
    setConflito("");
    try {
      const atual = await getJson<R>(caminho);
      versaoBaseRef.current = atual.version;
      setServidor({ version: atual.version, atualizadoEm: atual.atualizadoEm });
    } catch (error) {
      setErroSalvar(mensagemDeErro(error));
      return;
    }
    await salvar();
  }, [caminho, salvar]);

  /** Conflito de versão, opção 3: decidir depois. O rascunho continua guardado e a pessoa segue editando. */
  const dispensarConflito = useCallback(() => setConflito(""), []);

  const recuperarRascunho = useCallback(() => {
    const rascunho = decisaoRascunho;
    if (!rascunho) return;
    versaoBaseRef.current = rascunho.baseVersion; // continua valendo a versão em que a pessoa editou (D6)
    aplicarValores(rascunho.data, true);
    setDecisaoRascunho(null);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [decisaoRascunho]);

  const descartarRascunho = useCallback(() => {
    apagarRascunho(uuid, numero);
    setDecisaoRascunho(null);
  }, [uuid, numero]);

  const errosComPendencias = useMemo(() => {
    if (!mostrarPendencias) return errosCampos;
    const daPendencia: Record<string, string> = {};
    for (const p of pendencias) daPendencia[p.campo] ??= p.mensagem.replace(/^[\d.]+(, linha \d+)?:\s*/, "");
    return { ...daPendencia, ...errosCampos };
  }, [mostrarPendencias, pendencias, errosCampos]);

  return {
    fase,
    erroCarga,
    valores,
    alterar,
    sujo,
    offline,
    salvando,
    salvar,
    erroSalvar,
    // Erros digitados/do servidor sempre mandam; as pendências só preenchem os campos que ainda não têm erro.
    errosCampos: errosComPendencias,
    pendencias,
    conflito,
    recarregarDoServidor,
    sobrescrever,
    dispensarConflito,
    /** Rascunho local achado ao abrir o bloco, esperando a decisão Recuperar/Descartar. */
    decisaoRascunho,
    /** O servidor tem uma versão mais nova que a do rascunho (outra pessoa salvou depois). */
    rascunhoDesatualizado: decisaoRascunho ? decisaoRascunho.baseVersion !== servidor.version : false,
    recuperarRascunho,
    descartarRascunho,
    semArmazenamento,
    /** Data do último salvamento confirmado pelo servidor. */
    salvoEm: servidor.atualizadoEm,
    recarregar: carregar,
  };
}
