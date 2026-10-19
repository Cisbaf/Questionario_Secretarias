import { cookies } from "next/headers";
import { NextResponse } from "next/server";

const backendUrl = process.env.BACKEND_INTERNAL_URL ?? "http://localhost:8082/api";

/**
 * BFF: o navegador só fala com o Next (/api/...), que repassa ao Spring por BACKEND_INTERNAL_URL.
 * O token JWT do admin vem no cookie HTTP-only auth_token (criado pelo Spring no login) e segue ao backend como
 * Authorization; sem cookie, o repasse segue sem Authorization e o questionário continua público.
 */
export async function proxyFetch(req: Request, segments: string[]) {
  try {
    const cookieStore = await cookies();
    const token = cookieStore.get("auth_token")?.value;
    const incomingUrl = new URL(req.url);
    const path = segments.map(encodeURIComponent).join("/");
    const headers = new Headers();
    const contentType = req.headers.get("content-type");
    if (contentType) headers.set("Content-Type", contentType);
    if (token) headers.set("Authorization", `Bearer ${token}`);

    const hasBody = !["GET", "HEAD"].includes(req.method);
    const response = await fetch(`${backendUrl}/${path}${incomingUrl.search}`, {
      method: req.method,
      headers,
      body: hasBody ? await req.arrayBuffer() : undefined,
      cache: "no-store",
    });

    const body = response.status === 204 ? null : await response.arrayBuffer();
    const respostaHeaders: Record<string, string> = { "Content-Type": response.headers.get("content-type") ?? "application/json" };
    const disposicao = response.headers.get("content-disposition");
    if (disposicao) respostaHeaders["Content-Disposition"] = disposicao; // nome do arquivo do Excel
    const nextResponse = new NextResponse(body, { status: response.status, headers: respostaHeaders });
    const setCookie = response.headers.get("set-cookie");
    if (setCookie) nextResponse.headers.set("set-cookie", setCookie);
    return nextResponse;
  } catch (error) {
    // Site público: o motivo técnico (host, porta, stack) fica só no log do servidor, nunca na resposta.
    console.error("[proxy] backend indisponível:", error);
    return NextResponse.json({ message: "Backend indisponível" }, { status: 502 });
  }
}
