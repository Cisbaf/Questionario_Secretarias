package questionario.api.dto;

import questionario.api.dto.bloco.Bloco3Dados;
import questionario.api.dto.bloco.Bloco4Dados;
import questionario.api.dto.bloco.Bloco5Dados;
import questionario.api.dto.bloco.Bloco6Dados;
import questionario.api.dto.bloco.Bloco7Dados;
import questionario.api.dto.bloco.Bloco8Dados;
import questionario.domain.Questionario;
import questionario.domain.enums.StatusQuestionario;

import java.time.LocalDateTime;
import java.util.List;

/** Todas as respostas de um questionário de uma vez (revisão no site e, na E11, a visualização da equipe). */
public record RespostasResponse(
        String publicId,
        StatusQuestionario status,
        LocalDateTime criadoEm,
        LocalDateTime enviadoEm,
        Bloco1Response bloco1,
        Bloco2Response bloco2,
        Bloco3Dados bloco3,
        Bloco4Dados bloco4,
        Bloco5Dados bloco5,
        Bloco6Dados bloco6,
        Bloco7Dados bloco7,
        Bloco8Dados bloco8,
        List<AnexoResponse> anexos) {

    public static RespostasResponse de(Questionario q, Bloco1Response b1, Bloco2Response b2, Bloco3Dados b3,
                                       Bloco4Dados b4, Bloco5Dados b5, Bloco6Dados b6, Bloco7Dados b7, Bloco8Dados b8,
                                       List<AnexoResponse> anexos) {
        return new RespostasResponse(q.getPublicId(), q.getStatus(), q.getCriadoEm(), q.getEnviadoEm(),
                b1, b2, b3, b4, b5, b6, b7, b8, anexos);
    }
}
