package questionario.api.dto;

/**
 * Algo que impede o envio final (D5): um obrigatório que falta ou um número incoerente. No rascunho nada disso bloqueia
 * o salvamento; só aparece na revisão e trava o "Enviar questionário".
 */
public record Pendencia(int bloco, String campo, String mensagem, Tipo tipo) {

    public enum Tipo { OBRIGATORIO, COERENCIA }

    public static Pendencia obrigatorio(int bloco, String campo, String mensagem) {
        return new Pendencia(bloco, campo, mensagem, Tipo.OBRIGATORIO);
    }

    public static Pendencia coerencia(int bloco, String campo, String mensagem) {
        return new Pendencia(bloco, campo, mensagem, Tipo.COERENCIA);
    }
}
