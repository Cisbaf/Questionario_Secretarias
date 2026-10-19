package questionario.api;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import questionario.api.dto.LevantamentoResponse;
import questionario.service.LevantamentoService;

@RestController
@RequestMapping("/api/levantamentos")
@RequiredArgsConstructor
public class LevantamentoController {

    private final LevantamentoService service;

    /** Capa do questionário (período, data do censo, prazo, para onde enviar). 404 se não houver levantamento aberto. */
    @GetMapping("/ativo")
    public LevantamentoResponse ativo() {
        return LevantamentoResponse.from(service.ativo());
    }
}
