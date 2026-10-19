package questionario.api.dto;

import questionario.domain.Questionario;
import questionario.domain.enums.StatusQuestionario;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Só o {@code publicId} é exposto; o id numérico interno nunca sai da API. {@code blocos} traz a situação dos 8
 * blocos (versão e data do último salvamento no servidor).
 */
public record QuestionarioResponse(
        String publicId,
        StatusQuestionario status,
        LocalDateTime criadoEm,
        LocalDateTime enviadoEm,
        List<BlocoSituacaoResponse> blocos) {

    public static QuestionarioResponse from(Questionario questionario, List<BlocoSituacaoResponse> blocos) {
        return new QuestionarioResponse(
                questionario.getPublicId(),
                questionario.getStatus(),
                questionario.getCriadoEm(),
                questionario.getEnviadoEm(),
                blocos);
    }
}
