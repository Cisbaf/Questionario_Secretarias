package questionario.service;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import questionario.api.dto.Bloco1Request;
import questionario.api.dto.Bloco1Response;
import questionario.domain.Bloco1Identificacao;
import questionario.domain.Questionario;
import questionario.repository.Bloco1IdentificacaoRepository;

import java.util.Objects;

/** Leitura e gravação do Bloco 1 (identificação) com controle de versão por bloco (D6). */
@Service
@RequiredArgsConstructor
public class Bloco1Service {

    private final QuestionarioService questionarios;
    private final Bloco1IdentificacaoRepository repository;

    @Transactional(readOnly = true)
    public Bloco1Response ler(String publicId) {
        Questionario questionario = questionarios.buscar(publicId);
        return repository.findByQuestionario(questionario)
                .map(Bloco1Response::from)
                .orElseGet(Bloco1Response::vazio);
    }

    /**
     * Grava o bloco inteiro. Só vale se {@code request.version()} for a versão atual do servidor ({@code null} se
     * ainda não existe). Caso contrário, 409 com código VERSAO_DESATUALIZADA e nada é alterado.
     */
    @Transactional
    public Bloco1Response salvar(String publicId, Bloco1Request request) {
        Questionario questionario = questionarios.buscar(publicId);
        ControleDeVersao.exigirRascunho(questionario);
        if (request.dataPreenchimento() != null
                && request.dataPreenchimento().isAfter(java.time.LocalDate.now(java.time.ZoneId.of("America/Sao_Paulo")))) {
            throw new questionario.exception.DadosInvalidosException(
                    java.util.Map.of("dataPreenchimento", "A data não pode ser futura"));
        }

        Bloco1Identificacao bloco = repository.findByQuestionario(questionario).orElse(null);
        if (bloco == null) {
            if (request.version() != null) {
                throw ControleDeVersao.versaoDesatualizada();
            }
            bloco = new Bloco1Identificacao(questionario);
        } else if (!Objects.equals(bloco.getVersion(), request.version())) {
            throw ControleDeVersao.versaoDesatualizada();
        }

        bloco.atualizar(
                texto(request.municipio()), texto(request.secretario()), texto(request.responsavel()),
                texto(request.cargoSetor()), texto(request.email()), texto(request.telefone()),
                request.dataPreenchimento());
        try {
            return Bloco1Response.from(repository.saveAndFlush(bloco));
        } catch (DataIntegrityViolationException | ObjectOptimisticLockingFailureException corrida) {
            // Duas gravações simultâneas (primeira criação ou update): a segunda perde, como na versão velha.
            throw ControleDeVersao.versaoDesatualizada();
        }
    }

    /** Tira espaços das pontas; vazio vira nulo (campo não preenchido). */
    private static String texto(String valor) {
        if (valor == null) return null;
        String limpo = valor.strip();
        return limpo.isEmpty() ? null : limpo;
    }
}
