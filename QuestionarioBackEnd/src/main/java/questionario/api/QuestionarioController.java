package questionario.api;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import questionario.api.dto.QuestionarioResponse;
import questionario.domain.Questionario;
import questionario.service.QuestionarioService;

@RestController
@RequestMapping("/api/questionarios")
@RequiredArgsConstructor
public class QuestionarioController {

    private final QuestionarioService service;

    /** Cria um questionário vazio e devolve o publicId (o link é montado pelo front). 409 se não houver levantamento aberto. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public QuestionarioResponse iniciar() {
        return resposta(service.iniciar());
    }

    /** Cabeçalho do questionário e a situação (versão e data) de cada bloco no servidor. */
    @GetMapping("/{publicId}")
    public QuestionarioResponse buscar(@PathVariable String publicId) {
        return resposta(service.buscar(publicId));
    }

    private QuestionarioResponse resposta(Questionario questionario) {
        return QuestionarioResponse.from(questionario, service.situacaoDosBlocos(questionario));
    }
}
