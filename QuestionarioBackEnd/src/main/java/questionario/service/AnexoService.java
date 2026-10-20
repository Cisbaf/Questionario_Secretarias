package questionario.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import questionario.api.dto.AnexoResponse;
import questionario.domain.Anexo;
import questionario.domain.AnexoConteudo;
import questionario.domain.Questionario;
import questionario.domain.enums.DocumentoAnexavel;
import questionario.exception.DadosInvalidosException;
import questionario.repository.AnexoConteudoRepository;
import questionario.repository.AnexoRepository;

import jakarta.persistence.EntityNotFoundException;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Anexos do item 8.2. O questionário é público, então tudo aqui é limitado: no máximo {@value #MAX_POR_DOCUMENTO}
 * arquivos por documento (a planilha de coleta do Bloco 1 é um só, e substitui a anterior) (6 documentos = no máximo 18 arquivos), {@code 10 MB} cada, só alguns formatos, conferidos pela
 * extensão e pelos primeiros bytes (não pelo tipo que o navegador diz). Só enquanto o questionário é rascunho.
 */
@Service
@RequiredArgsConstructor
public class AnexoService {

    public static final int MAX_POR_DOCUMENTO = 3;
    public static final long MAX_BYTES = 10L * 1024 * 1024;

    private static final Map<String, String> TIPOS = Map.ofEntries(
            Map.entry("pdf", "application/pdf"),
            Map.entry("doc", "application/msword"),
            Map.entry("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
            Map.entry("xls", "application/vnd.ms-excel"),
            Map.entry("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
            Map.entry("odt", "application/vnd.oasis.opendocument.text"),
            Map.entry("ods", "application/vnd.oasis.opendocument.spreadsheet"),
            Map.entry("csv", "text/csv"),
            Map.entry("png", "image/png"),
            Map.entry("jpg", "image/jpeg"),
            Map.entry("jpeg", "image/jpeg"));

    /** A planilha de coleta (Bloco 1) só aceita planilhas, e só uma: o envio novo troca o anterior. */
    private static final Set<String> EXTENSOES_PLANILHA = Set.of("xlsx", "xls", "ods");

    private final QuestionarioService questionarios;
    private final AnexoRepository anexos;
    private final AnexoConteudoRepository conteudos;

    /** Arquivo para baixar: nome, tipo e bytes. */
    public record Baixavel(String nome, String tipo, byte[] dados) {
    }

    @Transactional(readOnly = true)
    public java.util.List<AnexoResponse> listar(String publicId) {
        return listar(questionarios.buscar(publicId));
    }

    @Transactional(readOnly = true)
    public java.util.List<AnexoResponse> listar(Questionario q) {
        return anexos.findByQuestionarioOrderByIdAsc(q).stream().map(AnexoResponse::de).toList();
    }

    /** Documentos que já têm ao menos um arquivo: não podem ser desmarcados no 8.2 enquanto houver anexo. */
    @Transactional(readOnly = true)
    public Set<DocumentoAnexavel> documentosComAnexo(String publicId) {
        return anexos.findByQuestionarioOrderByIdAsc(questionarios.buscar(publicId)).stream()
                .map(Anexo::getDocumento).filter(DocumentoAnexavel::doItem82).collect(Collectors.toSet());
    }

    @Transactional
    public AnexoResponse enviar(String publicId, DocumentoAnexavel documento, MultipartFile arquivo) {
        Questionario q = questionarios.buscar(publicId);
        ControleDeVersao.exigirRascunho(q);
        if (arquivo == null || arquivo.isEmpty()) throw recusa("Escolha um arquivo com conteúdo.");
        if (arquivo.getSize() > MAX_BYTES) throw recusa("O arquivo passa de 10 MB.");
        boolean planilha = documento == DocumentoAnexavel.PLANILHA_COLETA;
        if (!planilha && anexos.countByQuestionarioAndDocumento(q, documento) >= MAX_POR_DOCUMENTO) {
            throw recusa("Este documento já tem " + MAX_POR_DOCUMENTO + " arquivos (o máximo). Remova um para enviar outro.");
        }
        String nome = nomeSeguro(arquivo.getOriginalFilename());
        String extensao = extensao(nome);
        String tipo = TIPOS.get(extensao);
        if (planilha && !EXTENSOES_PLANILHA.contains(extensao)) {
            throw recusa("Envie a planilha de coleta preenchida em Excel (.xlsx ou .xls) ou OpenDocument (.ods).");
        }
        if (tipo == null) throw recusa("Formato não aceito. Envie PDF, Word, Excel, CSV, OpenDocument ou imagem (PNG/JPG).");
        byte[] bytes;
        try {
            bytes = arquivo.getBytes();
        } catch (IOException e) {
            throw recusa("Não foi possível ler o arquivo. Tente de novo.");
        }
        if (!assinaturaConfere(extensao, bytes)) throw recusa("O conteúdo do arquivo não corresponde ao formato ." + extensao + ".");

        if (planilha) { // um arquivo só: o novo substitui o anterior (só depois de o novo passar em todas as conferências)
            for (Anexo antigo : anexos.findByQuestionarioOrderByIdAsc(q)) {
                if (antigo.getDocumento() == DocumentoAnexavel.PLANILHA_COLETA) {
                    conteudos.deleteById(antigo.getId());
                    anexos.delete(antigo);
                }
            }
            anexos.flush();
        }
        Anexo anexo = anexos.save(new Anexo(q, documento, nome, tipo, bytes.length));
        conteudos.save(new AnexoConteudo(anexo.getId(), bytes));
        return AnexoResponse.de(anexo);
    }

    @Transactional
    public void excluir(String publicId, Long id) {
        Questionario q = questionarios.buscar(publicId);
        ControleDeVersao.exigirRascunho(q);
        Anexo anexo = anexos.findByIdAndQuestionario(id, q).orElseThrow(() -> new EntityNotFoundException("Anexo não encontrado"));
        conteudos.deleteById(anexo.getId());
        anexos.delete(anexo);
    }

    @Transactional(readOnly = true)
    public Baixavel baixar(String publicId, Long id) {
        Questionario q = questionarios.buscar(publicId);
        Anexo anexo = anexos.findByIdAndQuestionario(id, q).orElseThrow(() -> new EntityNotFoundException("Anexo não encontrado"));
        AnexoConteudo c = conteudos.findById(anexo.getId()).orElseThrow(() -> new EntityNotFoundException("Anexo não encontrado"));
        return new Baixavel(anexo.getNomeOriginal(), anexo.getTipoConteudo(), c.getDados());
    }

    // ---------- validação ----------

    private static DadosInvalidosException recusa(String mensagem) {
        return new DadosInvalidosException(Map.of("arquivo", mensagem));
    }

    /** Só o nome do arquivo (sem pasta), sem caracteres de controle e com no máximo 150 caracteres. */
    static String nomeSeguro(String original) {
        String nome = original == null ? "" : original.replace('\\', '/');
        nome = nome.substring(nome.lastIndexOf('/') + 1).replaceAll("\\p{Cntrl}", "").strip();
        if (nome.length() > 150) {
            String ext = extensao(nome);
            nome = nome.substring(0, 150 - ext.length() - 1) + "." + ext;
        }
        return nome.isEmpty() ? "arquivo" : nome;
    }

    static String extensao(String nome) {
        int ponto = nome.lastIndexOf('.');
        return ponto < 0 ? "" : nome.substring(ponto + 1).toLowerCase(Locale.ROOT);
    }

    static boolean assinaturaConfere(String extensao, byte[] b) {
        return switch (extensao) {
            case "pdf" -> comeca(b, '%', 'P', 'D', 'F');
            case "docx", "xlsx", "odt", "ods" -> comeca(b, 'P', 'K', 3, 4);
            case "doc", "xls" -> comeca(b, 0xD0, 0xCF, 0x11, 0xE0);
            case "png" -> comeca(b, 0x89, 'P', 'N', 'G');
            case "jpg", "jpeg" -> comeca(b, 0xFF, 0xD8, 0xFF);
            case "csv" -> semNulos(b);
            default -> false;
        };
    }

    private static boolean comeca(byte[] b, int... esperado) {
        if (b.length < esperado.length) return false;
        for (int i = 0; i < esperado.length; i++) if ((b[i] & 0xFF) != esperado[i]) return false;
        return true;
    }

    /** CSV é texto: sem byte zero nos primeiros 8 KB (binário disfarçado de .csv). */
    private static boolean semNulos(byte[] b) {
        for (int i = 0; i < Math.min(b.length, 8192); i++) if (b[i] == 0) return false;
        return true;
    }
}
