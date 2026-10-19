package questionario.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import questionario.api.dto.AdminDetalhe;
import questionario.api.dto.AdminResumo;
import questionario.api.dto.BlocoSituacaoResponse;
import questionario.api.dto.Pendencia;
import questionario.api.dto.RespostasResponse;
import questionario.domain.Questionario;
import questionario.repository.QuestionarioRepository;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** Leitura da equipe (admin): lista, visao completa e o material do Excel. Nada aqui grava. */
@Service
@RequiredArgsConstructor
public class AdminService {

    static final int TOTAL_BLOCOS = 8;

    private final QuestionarioRepository repository;
    private final QuestionarioService questionarios;
    private final EnvioService envio;

    /** Um questionario ja lido por inteiro, com o que o Excel e a lista precisam. */
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

    @Transactional(readOnly = true)
    public AdminDetalhe detalhe(String publicId) {
        RespostasResponse respostas = envio.respostas(publicId);
        return new AdminDetalhe(respostas, EnvioService.pendencias(respostas));
    }

    /** Os questionarios do filtro, ordenados por municipio (sem municipio no fim), ja lidos por inteiro. */
    @Transactional(readOnly = true)
    public List<Lido> lerPara(FiltroStatus filtro) {
        return lerTodos().stream().filter(l -> filtro.aceita(l.questionario())).toList();
    }

    @Transactional(readOnly = true)
    public Lido ler(String publicId) {
        Questionario q = questionarios.buscar(publicId);
        return lerUm(q);
    }

    /** Quais municipios aparecem em mais de um questionario (pela chave sem acento/caixa). */
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
