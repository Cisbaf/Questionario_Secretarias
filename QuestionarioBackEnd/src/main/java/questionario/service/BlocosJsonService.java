package questionario.service;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import questionario.api.dto.bloco.BlocoDados;
import questionario.domain.BlocoConteudo;
import questionario.domain.Questionario;
import questionario.repository.BlocoConteudoRepository;
import tools.jackson.databind.ObjectMapper;

import java.util.Objects;
import java.util.function.Supplier;

/** Leitura e gravação dos Blocos 3 a 8 (conteúdo em JSON) com a mesma regra de versão por bloco do Bloco 1 (D6). */
@Service
@RequiredArgsConstructor
public class BlocosJsonService {

    private final QuestionarioService questionarios;
    private final BlocoConteudoRepository repository;
    private final ObjectMapper mapper;

    @Transactional(readOnly = true)
    public <T extends BlocoDados<T>> T ler(String publicId, int numero, Class<T> tipo, Supplier<T> vazio) {
        Questionario questionario = questionarios.buscar(publicId);
        return lerDoQuestionario(questionario, numero, tipo, vazio);
    }

    /** Usado também pela revisão e pelo envio, que já têm o questionário carregado. */
    @Transactional(readOnly = true)
    public <T extends BlocoDados<T>> T lerDoQuestionario(Questionario questionario, int numero, Class<T> tipo,
                                                         Supplier<T> vazio) {
        return repository.findByQuestionarioAndNumero(questionario, numero)
                .map(bloco -> converter(bloco, tipo))
                .orElseGet(vazio);
    }

    /**
     * Grava o bloco inteiro. Só vale se {@code request.version()} for a versão atual do servidor ({@code null} se ainda
     * não existe); caso contrário, 409 VERSAO_DESATUALIZADA e nada muda. Regras que dão 400 rodam antes de tudo.
     */
    @Transactional
    public <T extends BlocoDados<T>> T salvar(String publicId, int numero, Class<T> tipo, T request) {
        request.validarRegras();
        Questionario questionario = questionarios.buscar(publicId);
        ControleDeVersao.exigirRascunho(questionario);

        BlocoConteudo bloco = repository.findByQuestionarioAndNumero(questionario, numero).orElse(null);
        if (bloco == null) {
            if (request.version() != null) {
                throw ControleDeVersao.versaoDesatualizada();
            }
            bloco = new BlocoConteudo(questionario, numero);
        } else if (!Objects.equals(bloco.getVersion(), request.version())) {
            throw ControleDeVersao.versaoDesatualizada();
        }

        bloco.atualizar(mapper.writeValueAsString(request.normalizado().comControle(null, null)));
        try {
            return converter(repository.saveAndFlush(bloco), tipo);
        } catch (DataIntegrityViolationException | ObjectOptimisticLockingFailureException corrida) {
            // Duas gravações simultâneas (primeira criação ou update): a segunda perde, como nos outros blocos.
            throw ControleDeVersao.versaoDesatualizada();
        }
    }

    private <T extends BlocoDados<T>> T converter(BlocoConteudo bloco, Class<T> tipo) {
        T dados = mapper.readValue(bloco.getConteudo(), tipo);
        return dados.normalizado().comControle(bloco.getVersion(), bloco.getAtualizadoEm());
    }
}
