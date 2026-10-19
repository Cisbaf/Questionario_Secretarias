package questionario.api.dto;

import questionario.domain.enums.StatusQuestionario;

import java.time.LocalDateTime;

/**
 * Uma linha da lista do admin. {@code blocosSalvos}: blocos com alguma gravacao; {@code blocosCompletos}: salvos e sem
 * nenhuma pendencia (e o que a barra lateral do questionario chama de "feito"). {@code municipioRepetido}: outro
 * questionario traz o mesmo municipio (ignorando acentos, maiusculas e espacos).
 */
public record AdminResumo(
        String publicId,
        String municipio,
        StatusQuestionario status,
        int blocosSalvos,
        int blocosCompletos,
        int totalBlocos,
        int pendencias,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm,
        LocalDateTime enviadoEm,
        boolean municipioRepetido,
        boolean semMunicipio) {
}
