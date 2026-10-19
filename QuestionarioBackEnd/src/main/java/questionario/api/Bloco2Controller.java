package questionario.api;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import questionario.api.dto.Bloco2Request;
import questionario.api.dto.Bloco2Response;
import questionario.service.Bloco2Service;

/** Bloco 2 (prioridades de atenção à saúde). Público: quem tem o link (publicId) lê e grava. */
@RestController
@RequestMapping("/api/questionarios/{publicId}/blocos/2")
@RequiredArgsConstructor
public class Bloco2Controller {

    private final Bloco2Service service;

    @GetMapping
    public Bloco2Response ler(@PathVariable String publicId) {
        return service.ler(publicId);
    }

    @PutMapping
    public Bloco2Response salvar(@PathVariable String publicId, @Valid @RequestBody Bloco2Request request) {
        return service.salvar(publicId, request);
    }
}
