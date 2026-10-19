package questionario.api;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import questionario.api.dto.Pendencia;
import questionario.api.dto.QuestionarioResponse;
import questionario.api.dto.RespostasResponse;
import questionario.service.EnvioService;

import java.util.List;

/** Revisão e envio final. Público: quem tem o link (publicId) revisa e envia. */
@RestController
@RequestMapping("/api/questionarios/{publicId}")
@RequiredArgsConstructor
public class EnvioController {

    private final EnvioService service;

    /** Todas as respostas dos 8 blocos, para a tela de revisão. */
    @GetMapping("/respostas")
    public RespostasResponse respostas(@PathVariable String publicId) {
        return service.respostas(publicId);
    }

    /** O que ainda impede o envio. Lista vazia = pode enviar. */
    @GetMapping("/pendencias")
    public List<Pendencia> pendencias(@PathVariable String publicId) {
        return service.pendencias(publicId);
    }

    @PostMapping("/enviar")
    public QuestionarioResponse enviar(@PathVariable String publicId) {
        return service.enviar(publicId);
    }
}
