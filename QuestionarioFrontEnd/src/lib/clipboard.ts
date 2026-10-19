/** Copia o texto; devolve false se o navegador não deixar (o chamador mostra como copiar na mão). */
export async function copiarTexto(texto: string): Promise<boolean> {
  try {
    if (navigator.clipboard && window.isSecureContext) {
      await navigator.clipboard.writeText(texto);
      return true;
    }
  } catch {
    // cai no método antigo abaixo
  }
  try {
    // Sem HTTPS (ex.: acesso pelo IP da rede interna) a API moderna não existe; o método antigo ainda funciona.
    const campo = document.createElement("textarea");
    campo.value = texto;
    campo.setAttribute("readonly", "");
    campo.style.position = "fixed";
    campo.style.opacity = "0";
    document.body.appendChild(campo);
    campo.select();
    const copiou = document.execCommand("copy");
    document.body.removeChild(campo);
    return copiou;
  } catch {
    return false;
  }
}
