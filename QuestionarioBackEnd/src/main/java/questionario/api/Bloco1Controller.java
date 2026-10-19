package questionario.api;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import questionario.api.dto.Bloco1Request;
import questionario.api.dto.Bloco1Response;
import questionario.service.Bloco1Service;

/** Bloco 1 (identificação). Público: quem tem o link (publicId) lê e grava. */
@RestController
@RequestMapping("/api/questionarios/{publicId}/blocos/1")
@RequiredArgsConstructor
public class Bloco1Controller {

    private final Bloco1Service service;

    @GetMapping
    public Bloco1Response ler(@PathVariable String publicId) {
        return service.ler(publicId);
    }

    @PutMapping
    public Bloco1Response salvar(@PathVariable String publicId, @Valid @RequestBody Bloco1Request request) {
        return service.salvar(publicId, request);
    }
}
