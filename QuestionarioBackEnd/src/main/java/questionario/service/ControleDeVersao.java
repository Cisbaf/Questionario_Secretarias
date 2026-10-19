package questionario.service;

import questionario.domain.Questionario;
import questionario.domain.enums.StatusQuestionario;
import questionario.exception.ConflictException;

/** Regras de conflito comuns a todos os blocos: questionário já enviado e versão desatualizada (D6, D12). */
final class ControleDeVersao {

    private ControleDeVersao() {
    }

    static void exigirRascunho(Questionario questionario) {
        if (questionario.getStatus() == StatusQuestionario.ENVIADO) {
            throw new ConflictException(ConflictException.QUESTIONARIO_ENVIADO,
                    "Este questionário já foi enviado e não pode mais ser alterado");
        }
    }

    static ConflictException versaoDesatualizada() {
        return new ConflictException(ConflictException.VERSAO_DESATUALIZADA,
                "Este bloco foi alterado por outra pessoa. Recarregue para ver a versão mais recente.");
    }
}
