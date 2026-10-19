package questionario.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import questionario.config.LevantamentoProperties;
import questionario.domain.Levantamento;
import questionario.domain.enums.StatusLevantamento;
import questionario.repository.LevantamentoRepository;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LevantamentoService {

    private final LevantamentoRepository repository;
    private final LevantamentoProperties properties;

    /** Chamado ao subir: o levantamento aberto passa a refletir as propriedades (cria se ainda não houver um). */
    @Transactional
    public void sincronizar() {
        sincronizar(properties);
    }

    @Transactional
    public void sincronizar(LevantamentoProperties capa) {
        capa.validar();
        Optional<Levantamento> aberto = repository.findFirstByStatusOrderByIdDesc(StatusLevantamento.ABERTO);
        if (aberto.isPresent()) {
            aberto.get().atualizarCapa(capa.tituloOuPadrao(), capa.inicio(), capa.fim(), capa.censoUpa(),
                    capa.prazo(), capa.enviarParaOuNulo());
        } else {
            repository.save(new Levantamento(capa.tituloOuPadrao(), capa.inicio(), capa.fim(), capa.censoUpa(),
                    capa.prazo(), capa.enviarParaOuNulo()));
        }
    }

    @Transactional(readOnly = true)
    public Optional<Levantamento> aberto() {
        return repository.findFirstByStatusOrderByIdDesc(StatusLevantamento.ABERTO);
    }

    @Transactional(readOnly = true)
    public Levantamento ativo() {
        return aberto().orElseThrow(() -> new EntityNotFoundException("Nenhum levantamento aberto no momento"));
    }
}
