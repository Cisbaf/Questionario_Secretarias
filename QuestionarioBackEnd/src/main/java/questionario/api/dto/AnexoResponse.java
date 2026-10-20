package questionario.api.dto;

import questionario.domain.Anexo;
import questionario.domain.enums.DocumentoAnexavel;

import java.time.LocalDateTime;

/** Um arquivo anexado ao 8.2 (sem os bytes). */
public record AnexoResponse(Long id, DocumentoAnexavel documento, String nome, long tamanho, LocalDateTime criadoEm) {

    public static AnexoResponse de(Anexo a) {
        return new AnexoResponse(a.getId(), a.getDocumento(), a.getNomeOriginal(), a.getTamanho(), a.getCriadoEm());
    }
}
