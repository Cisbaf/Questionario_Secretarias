package questionario.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import questionario.api.dto.Bloco1Response;
import questionario.api.dto.Bloco2Response;
import questionario.api.dto.Pendencia;
import questionario.api.dto.QuestionarioResponse;
import questionario.api.dto.RespostasResponse;
import questionario.api.dto.bloco.Bloco3Dados;
import questionario.api.dto.bloco.Bloco4Dados;
import questionario.api.dto.bloco.Bloco5Dados;
import questionario.api.dto.bloco.Bloco6Dados;
import questionario.api.dto.bloco.Bloco7Dados;
import questionario.api.dto.bloco.Bloco8Dados;
import questionario.domain.Questionario;
import questionario.exception.EnvioIncompletoException;
import questionario.repository.QuestionarioRepository;
import questionario.service.regras.PendenciasBlocos1e2;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Revisão (todas as respostas e pendências) e envio final do questionário. */
@Service
@RequiredArgsConstructor
public class EnvioService {

    private final QuestionarioService questionarios;
    private final QuestionarioRepository repository;
    private final Bloco1Service bloco1;
    private final Bloco2Service bloco2;
    private final BlocosJsonService blocos;
    private final AnexoService anexos;

    @Transactional(readOnly = true)
    public RespostasResponse respostas(String publicId) {
        Questionario q = questionarios.buscar(publicId);
        return RespostasResponse.de(q,
                bloco1.ler(publicId), bloco2.ler(publicId),
                blocos.lerDoQuestionario(q, 3, Bloco3Dados.class, Bloco3Dados::vazio),
                blocos.lerDoQuestionario(q, 4, Bloco4Dados.class, Bloco4Dados::vazio),
                blocos.lerDoQuestionario(q, 5, Bloco5Dados.class, Bloco5Dados::vazio),
                blocos.lerDoQuestionario(q, 6, Bloco6Dados.class, Bloco6Dados::vazio),
                blocos.lerDoQuestionario(q, 7, Bloco7Dados.class, Bloco7Dados::vazio),
                blocos.lerDoQuestionario(q, 8, Bloco8Dados.class, Bloco8Dados::vazio),
                anexos.listar(q));
    }

    /** Tudo que impede o envio, na ordem dos blocos. Lista vazia = pode enviar. */
    @Transactional(readOnly = true)
    public List<Pendencia> pendencias(String publicId) {
        return pendencias(respostas(publicId));
    }

    static List<Pendencia> pendencias(RespostasResponse r) {
        List<Pendencia> lista = new ArrayList<>();
        lista.addAll(PendenciasBlocos1e2.bloco1(r.bloco1()));
        lista.addAll(PendenciasBlocos1e2.bloco2(r.bloco2()));
        lista.addAll(r.bloco3().pendencias());
        lista.addAll(r.bloco4().pendencias());
        lista.addAll(r.bloco5().pendencias());
        lista.addAll(r.bloco6().pendencias());
        lista.addAll(r.bloco7().pendencias());
        lista.addAll(r.bloco8().pendencias());
        return lista;
    }

    /**
     * Envio definitivo (D12): confere tudo de novo no servidor (o front só esconde o botão), marca como ENVIADO e grava
     * a data. Com pendências, 409 ENVIO_INCOMPLETO e nada muda. Enviar duas vezes dá 409 QUESTIONARIO_ENVIADO.
     */
    @Transactional
    public QuestionarioResponse enviar(String publicId) {
        Questionario q = questionarios.buscar(publicId);
        ControleDeVersao.exigirRascunho(q);
        List<Pendencia> pendencias = pendencias(respostas(publicId));
        if (!pendencias.isEmpty()) {
            Map<String, String> campos = new LinkedHashMap<>();
            pendencias.forEach(p -> campos.putIfAbsent("bloco" + p.bloco() + "." + p.campo(), p.mensagem()));
            throw new EnvioIncompletoException(campos);
        }
        q.marcarComoEnviado();
        repository.save(q);
        return QuestionarioResponse.from(q, questionarios.situacaoDosBlocos(q));
    }
}
