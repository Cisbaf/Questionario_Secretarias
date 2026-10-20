package questionario.api;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import questionario.api.dto.AnexoResponse;
import questionario.domain.enums.DocumentoAnexavel;
import questionario.service.AnexoService;

import java.nio.charset.StandardCharsets;
import java.util.List;

/** Anexos do item 8.2. Público como o resto do questionário: quem tem o link (publicId) envia, lista, baixa e remove. */
@RestController
@RequestMapping("/api/questionarios/{publicId}/anexos")
@RequiredArgsConstructor
public class AnexoController {

    private final AnexoService service;

    @GetMapping
    public List<AnexoResponse> listar(@PathVariable String publicId) {
        return service.listar(publicId);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public AnexoResponse enviar(@PathVariable String publicId, @RequestParam DocumentoAnexavel documento,
                                @RequestParam("arquivo") MultipartFile arquivo) {
        return service.enviar(publicId, documento, arquivo);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable String publicId, @PathVariable Long id) {
        service.excluir(publicId, id);
    }

    @GetMapping("/{id}/arquivo")
    public ResponseEntity<byte[]> baixar(@PathVariable String publicId, @PathVariable Long id) {
        AnexoService.Baixavel b = service.baixar(publicId, id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(b.nome(), StandardCharsets.UTF_8).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .contentType(MediaType.parseMediaType(b.tipo()))
                .contentLength(b.dados().length)
                .body(b.dados());
    }
}
