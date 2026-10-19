package questionario.service;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import questionario.api.dto.Bloco2Request;
import questionario.api.dto.Bloco2Response;
import questionario.api.dto.PrioridadeDto;
import questionario.domain.Bloco2Prioridades;
import questionario.domain.PrioridadeItem;
import questionario.domain.Questionario;
import questionario.domain.enums.Impedimento;
import questionario.domain.enums.LinhaCuidado;
import questionario.domain.enums.TemaUrgencia;
import questionario.repository.Bloco2PrioridadesRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Leitura e gravação do Bloco 2 (prioridades de atenção à saúde) com controle de versão por bloco (D6). */
@Service
@RequiredArgsConstructor
public class Bloco2Service {

    private final QuestionarioService questionarios;
    private final Bloco2PrioridadesRepository repository;

    @Transactional(readOnly = true)
    public Bloco2Response ler(String publicId) {
        Questionario questionario = questionarios.buscar(publicId);
        return repository.findByQuestionario(questionario)
                .map(Bloco2Response::from)
                .orElseGet(Bloco2Response::vazio);
    }

    /**
     * Grava o bloco inteiro. Só vale se {@code request.version()} for a versão atual do servidor ({@code null} se
     * ainda não existe). Caso contrário, 409 com código VERSAO_DESATUALIZADA e nada é alterado.
     */
    @Transactional
    public Bloco2Response salvar(String publicId, Bloco2Request request) {
        Bloco2Validacao.validar(request);
        Questionario questionario = questionarios.buscar(publicId);
        ControleDeVersao.exigirRascunho(questionario);

        Bloco2Prioridades bloco = repository.findByQuestionario(questionario).orElse(null);
        if (bloco == null) {
            if (request.version() != null) {
                throw ControleDeVersao.versaoDesatualizada();
            }
            bloco = new Bloco2Prioridades(questionario);
        } else if (!Objects.equals(bloco.getVersion(), request.version())) {
            throw ControleDeVersao.versaoDesatualizada();
        }

        Set<Impedimento> impedimentos = Bloco2Validacao.marcados(request.impedimentos());
        // A descrição de "Outro" só vale com "Outro" marcado; sem a marca ela seria um texto órfão.
        String impedimentoOutro = impedimentos.contains(Impedimento.OUTRO) ? texto(request.impedimentoOutro()) : null;

        bloco.atualizar(
                prioridades(request.prioridades()), graus(request.grausLinhaCuidado()),
                texto(request.linhaOutraDescricao()), texto(request.gargalos()), texto(request.servicosParaVizinhos()),
                impedimentos, impedimentoOutro, ordem(request.ordemTemas()));
        try {
            return Bloco2Response.from(repository.saveAndFlush(bloco));
        } catch (DataIntegrityViolationException | ObjectOptimisticLockingFailureException corrida) {
            // Duas gravações simultâneas (primeira criação ou update): a segunda perde, como na versão velha.
            throw ControleDeVersao.versaoDesatualizada();
        }
    }

    /** Sempre 5 linhas, nas posições 1 a 5: as que não vieram (ou vieram vazias) ficam com campos nulos. */
    private static List<PrioridadeItem> prioridades(List<PrioridadeDto> recebidas) {
        List<PrioridadeItem> itens = new ArrayList<>();
        for (int i = 0; i < Bloco2Response.TOTAL_PRIORIDADES; i++) {
            PrioridadeDto dto = recebidas != null && i < recebidas.size() ? recebidas.get(i) : null;
            itens.add(dto == null
                    ? new PrioridadeItem(i + 1, null, null, null)
                    : new PrioridadeItem(i + 1, texto(dto.titulo()), texto(dto.problema()), dto.resolveSozinho()));
        }
        return itens;
    }

    /** Só as notas respondidas são gravadas. */
    private static Map<LinhaCuidado, Integer> graus(Map<LinhaCuidado, Integer> recebidos) {
        Map<LinhaCuidado, Integer> respondidos = new HashMap<>();
        if (recebidos != null) {
            recebidos.forEach((linha, nota) -> {
                if (nota != null) respondidos.put(linha, nota);
            });
        }
        return respondidos;
    }

    private static Map<TemaUrgencia, Integer> ordem(Map<TemaUrgencia, Integer> recebida) {
        Map<TemaUrgencia, Integer> definida = new HashMap<>();
        if (recebida != null) {
            recebida.forEach((tema, posicao) -> {
                if (posicao != null) definida.put(tema, posicao);
            });
        }
        return definida;
    }

    /** Tira espaços das pontas; vazio vira nulo (campo não preenchido). */
    private static String texto(String valor) {
        if (valor == null) return null;
        String limpo = valor.strip();
        return limpo.isEmpty() ? null : limpo;
    }
}
