package questionario.api.dto;

import questionario.domain.Bloco1Identificacao;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Bloco 1 como o front recebe. Bloco ainda não salvo: tudo {@code null}, inclusive {@code version}. */
public record Bloco1Response(
        String municipio,
        String secretario,
        String responsavel,
        String cargoSetor,
        String email,
        String telefone,
        LocalDate dataPreenchimento,
        Long version,
        LocalDateTime atualizadoEm) {

    public static Bloco1Response vazio() {
        return new Bloco1Response(null, null, null, null, null, null, null, null, null);
    }

    public static Bloco1Response from(Bloco1Identificacao bloco) {
        return new Bloco1Response(
                bloco.getMunicipio(), bloco.getSecretario(), bloco.getResponsavel(), bloco.getCargoSetor(),
                bloco.getEmail(), bloco.getTelefone(), bloco.getDataPreenchimento(),
                bloco.getVersion(), bloco.getAtualizadoEm());
    }
}
