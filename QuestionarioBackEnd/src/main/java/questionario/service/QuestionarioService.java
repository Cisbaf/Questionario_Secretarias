package questionario.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import questionario.api.dto.BlocoSituacaoResponse;
import questionario.domain.Levantamento;
import questionario.domain.Questionario;
import questionario.exception.ConflictException;
import questionario.repository.Bloco1IdentificacaoRepository;
import questionario.repository.Bloco2PrioridadesRepository;
import questionario.repository.BlocoConteudoRepository;
import questionario.repository.QuestionarioRepository;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class QuestionarioService {

    public static final int TOTAL_BLOCOS = 8;

    private final QuestionarioRepository repository;
    private final LevantamentoService levantamentos;
    private final Bloco1IdentificacaoRepository bloco1;
    private final Bloco2PrioridadesRepository bloco2;
    private final BlocoConteudoRepository blocosJson;

    /**
     * Cria um questionário vazio no levantamento aberto. Não há checagem de município repetido: o município é texto
     * livre no Bloco 1 e a equipe CISBAF resolve duplicatas na visualização (E11).
     */
    @Transactional
    public Questionario iniciar() {
        Levantamento levantamento = levantamentos.aberto()
                .orElseThrow(() -> new ConflictException("Não há levantamento aberto para receber questionários"));
        return repository.save(new Questionario(levantamento));
    }

    /** Link errado, inexistente ou que não seja um UUID: todos dão a mesma resposta (404), sem pista do motivo. */
    @Transactional(readOnly = true)
    public Questionario buscar(String publicId) {
        return repository.findByPublicId(publicId)
                .orElseThrow(() -> new EntityNotFoundException("Questionário não encontrado"));
    }

    /**
     * Situação dos 8 blocos no servidor. Cada etapa de bloco (E03 a E10) acrescenta aqui a leitura do seu repositório;
     * os blocos ainda sem implementação aparecem como "não salvos".
     */
    @Transactional(readOnly = true)
    public List<BlocoSituacaoResponse> situacaoDosBlocos(Questionario questionario) {
        List<BlocoSituacaoResponse> situacao = new ArrayList<>();
        for (int numero = 1; numero <= TOTAL_BLOCOS; numero++) {
            situacao.add(BlocoSituacaoResponse.naoSalvo(numero));
        }
        bloco1.findByQuestionario(questionario).ifPresent(bloco ->
                situacao.set(0, new BlocoSituacaoResponse(1, bloco.getVersion(), bloco.getAtualizadoEm())));
        bloco2.findByQuestionario(questionario).ifPresent(bloco ->
                situacao.set(1, new BlocoSituacaoResponse(2, bloco.getVersion(), bloco.getAtualizadoEm())));
        // Blocos 3 a 8 (conteúdo em JSON, uma linha por bloco).
        blocosJson.findByQuestionario(questionario).forEach(bloco ->
                situacao.set(bloco.getNumero() - 1,
                        new BlocoSituacaoResponse(bloco.getNumero(), bloco.getVersion(), bloco.getAtualizadoEm())));
        return situacao;
    }
}
