package questionario.api;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import questionario.api.dto.bloco.Bloco3Dados;
import questionario.api.dto.bloco.Bloco4Dados;
import questionario.api.dto.bloco.Bloco5Dados;
import questionario.api.dto.bloco.Bloco6Dados;
import questionario.api.dto.bloco.Bloco7Dados;
import questionario.api.dto.bloco.Bloco8Dados;
import questionario.service.AnexoService;
import questionario.service.BlocosJsonService;

/** Blocos 3 a 8. Público: quem tem o link (publicId) lê e grava. O corpo do PUT é o bloco inteiro mais a {@code version}. */
@RestController
@RequestMapping("/api/questionarios/{publicId}/blocos")
@RequiredArgsConstructor
public class BlocosJsonController {

    private final BlocosJsonService service;
    private final AnexoService anexos;

    @GetMapping("/3")
    public Bloco3Dados ler3(@PathVariable String publicId) {
        return service.ler(publicId, 3, Bloco3Dados.class, Bloco3Dados::vazio);
    }

    @PutMapping("/3")
    public Bloco3Dados salvar3(@PathVariable String publicId, @Valid @RequestBody Bloco3Dados corpo) {
        return service.salvar(publicId, 3, Bloco3Dados.class, corpo);
    }

    @GetMapping("/4")
    public Bloco4Dados ler4(@PathVariable String publicId) {
        return service.ler(publicId, 4, Bloco4Dados.class, Bloco4Dados::vazio);
    }

    @PutMapping("/4")
    public Bloco4Dados salvar4(@PathVariable String publicId, @Valid @RequestBody Bloco4Dados corpo) {
        return service.salvar(publicId, 4, Bloco4Dados.class, corpo);
    }

    @GetMapping("/5")
    public Bloco5Dados ler5(@PathVariable String publicId) {
        return service.ler(publicId, 5, Bloco5Dados.class, Bloco5Dados::vazio);
    }

    @PutMapping("/5")
    public Bloco5Dados salvar5(@PathVariable String publicId, @Valid @RequestBody Bloco5Dados corpo) {
        return service.salvar(publicId, 5, Bloco5Dados.class, corpo);
    }

    @GetMapping("/6")
    public Bloco6Dados ler6(@PathVariable String publicId) {
        return service.ler(publicId, 6, Bloco6Dados.class, Bloco6Dados::vazio);
    }

    @PutMapping("/6")
    public Bloco6Dados salvar6(@PathVariable String publicId, @Valid @RequestBody Bloco6Dados corpo) {
        return service.salvar(publicId, 6, Bloco6Dados.class, corpo);
    }

    @GetMapping("/7")
    public Bloco7Dados ler7(@PathVariable String publicId) {
        return service.ler(publicId, 7, Bloco7Dados.class, Bloco7Dados::vazio);
    }

    @PutMapping("/7")
    public Bloco7Dados salvar7(@PathVariable String publicId, @Valid @RequestBody Bloco7Dados corpo) {
        return service.salvar(publicId, 7, Bloco7Dados.class, corpo);
    }

    @GetMapping("/8")
    public Bloco8Dados ler8(@PathVariable String publicId) {
        return service.ler(publicId, 8, Bloco8Dados.class, Bloco8Dados::vazio);
    }

    @PutMapping("/8")
    public Bloco8Dados salvar8(@PathVariable String publicId, @Valid @RequestBody Bloco8Dados corpo) {
        // Documento que já tem arquivo anexado continua marcado, mesmo que o cliente tenha mandado desmarcado.
        java.util.Set<questionario.domain.enums.DocumentoAnexavel> marcados = new java.util.LinkedHashSet<>(
                corpo.documentos() == null ? java.util.Set.of() : corpo.documentos());
        marcados.addAll(anexos.documentosComAnexo(publicId));
        return service.salvar(publicId, 8, Bloco8Dados.class, corpo.comDocumentos(marcados));
    }
}
