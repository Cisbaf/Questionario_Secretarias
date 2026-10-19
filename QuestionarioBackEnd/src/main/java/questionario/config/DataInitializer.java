package questionario.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import questionario.service.LevantamentoService;

/** Ao subir, grava a capa (app.levantamento.*) no levantamento aberto. Se a propriedade estiver errada, a subida falha. */
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final LevantamentoService levantamentos;

    @Override
    public void run(ApplicationArguments args) {
        levantamentos.sincronizar();
    }
}
