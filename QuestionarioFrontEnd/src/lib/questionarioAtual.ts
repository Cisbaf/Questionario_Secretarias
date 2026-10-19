// Guarda no navegador qual questionário a pessoa estava preenchendo, para a página inicial oferecer "Continuar".
// Toda leitura e escrita fica dentro de try/catch: o localStorage pode estar bloqueado ou cheio (janela privada,
// política do navegador) e o site precisa funcionar do mesmo jeito, só sem a conveniência.

const CHAVE = "qsms:v1:atual";
const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

export function lerQuestionarioAtual(): string | null {
  try {
    const valor = window.localStorage.getItem(CHAVE);
    return valor && UUID.test(valor) ? valor : null;
  } catch {
    return null;
  }
}

export function definirQuestionarioAtual(publicId: string): void {
  try {
    window.localStorage.setItem(CHAVE, publicId);
  } catch {
    // sem localStorage: segue sem a opção "Continuar"
  }
}

export function limparQuestionarioAtual(): void {
  try {
    window.localStorage.removeItem(CHAVE);
  } catch {
    // idem
  }
}
