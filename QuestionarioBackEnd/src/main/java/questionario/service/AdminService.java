package questionario.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import questionario.api.dto.*;
import questionario.domain.Questionario;
import questionario.repository.*;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    static final int TOTAL_BLOCOS = 8;

    private final QuestionarioRepository repository;
    private final QuestionarioService questionarios;
    private final EnvioService envio;
    private final AnexoRepository anexos;
    private final AnexoConteudoRepository conteudos;
    private final Bloco1IdentificacaoRepository bloco1;
    private final Bloco2PrioridadesRepository bloco2;
    private final BlocoConteudoRepository blocos;

    public record Lido(Questionario questionario, RespostasResponse respostas, List<Pendencia> pendencias,
                       List<BlocoSituacaoResponse> situacao) {
    }

    @Transactional(readOnly = true)
    public List<AdminResumo> listar(FiltroStatus filtro) {
        List<Lido> todos = lerTodos();
        Map<String, Long> porNome = todos.stream()
                .map(l -> chave(l.respostas().bloco1().municipio()))
                .filter(c -> !c.isEmpty())
                .collect(Collectors.groupingBy(c -> c, Collectors.counting()));
        return todos.stream()
                .filter(l -> filtro.aceita(l.questionario()))
                .map(l -> resumir(l, porNome))
                .toList();
    }

    @Transactional
    public void excluir(String publicId) {
        Questionario q = questionarios.buscar(publicId);
        anexos.findByQuestionarioOrderByIdAsc(q).forEach(a -> conteudos.deleteById(a.getId()));
        anexos.deleteAll(anexos.findByQuestionarioOrderByIdAsc(q));
        blocos.deleteAll(blocos.findByQuestionario(q));
        bloco1.findByQuestionario(q).ifPresent(bloco1::delete);
        bloco2.findByQuestionario(q).ifPresent(bloco2::delete);
        repository.delete(q);
    }

    @Transactional(readOnly = true)
    public AdminDetalhe detalhe(String publicId) {
        RespostasResponse respostas = envio.respostas(publicId);
        return new AdminDetalhe(respostas, EnvioService.pendencias(respostas));
    }

    @Transactional(readOnly = true)
    public List<Lido> lerPara(FiltroStatus filtro) {
        return lerTodos().stream().filter(l -> filtro.aceita(l.questionario())).toList();
    }

    @Transactional(readOnly = true)
    public Lido ler(String publicId) {
        Questionario q = questionarios.buscar(publicId);
        return lerUm(q);
    }

    @Transactional(readOnly = true)
    public Set<String> municipiosRepetidos() {
        return lerTodos().stream()
                .map(l -> chave(l.respostas().bloco1().municipio()))
                .filter(c -> !c.isEmpty())
                .collect(Collectors.groupingBy(c -> c, Collectors.counting()))
                .entrySet().stream().filter(e -> e.getValue() > 1).map(Map.Entry::getKey).collect(Collectors.toSet());
    }

    public static String chave(String municipio) {
        if (municipio == null) return "";
        String semAcento = Normalizer.normalize(municipio, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return semAcento.toLowerCase().replaceAll("[^a-z0-9]+", " ").strip();
    }

    private List<Lido> lerTodos() {
        List<Lido> lidos = new ArrayList<>();
        for (Questionario q : repository.findAll()) lidos.add(lerUm(q));
        Comparator<Lido> porMunicipio = Comparator
                .comparing((Lido l) -> chave(l.respostas().bloco1().municipio()).isEmpty())
                .thenComparing(l -> chave(l.respostas().bloco1().municipio()))
                .thenComparing(l -> l.questionario().getCriadoEm());
        lidos.sort(porMunicipio);
        return lidos;
    }

    private Lido lerUm(Questionario q) {
        RespostasResponse respostas = envio.respostas(q.getPublicId());
        return new Lido(q, respostas, EnvioService.pendencias(respostas), questionarios.situacaoDosBlocos(q));
    }

    private static AdminResumo resumir(Lido l, Map<String, Long> porNome) {
        Questionario q = l.questionario();
        String municipio = l.respostas().bloco1().municipio();
        String chave = chave(municipio);
        Set<Integer> comPendencia = l.pendencias().stream().map(Pendencia::bloco).collect(Collectors.toSet());
        int salvos = 0, completos = 0;
        LocalDateTime atualizado = q.getCriadoEm();
        for (BlocoSituacaoResponse b : l.situacao()) {
            if (b.version() == null) continue;
            salvos++;
            if (!comPendencia.contains(b.numero())) completos++;
            if (b.atualizadoEm() != null && b.atualizadoEm().isAfter(atualizado)) atualizado = b.atualizadoEm();
        }
        return new AdminResumo(q.getPublicId(), municipio == null || municipio.isBlank() ? null : municipio.strip(),
                q.getStatus(), salvos, completos, TOTAL_BLOCOS, l.pendencias().size(), q.getCriadoEm(), atualizado,
                q.getEnviadoEm(), !chave.isEmpty() && porNome.getOrDefault(chave, 0L) > 1, chave.isEmpty());
    }
}
