package questionario.api.dto.bloco;

import jakarta.validation.constraints.Size;
import questionario.api.dto.Pendencia;
import questionario.domain.enums.DocumentoAnexavel;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Bloco 8 do documento: observações finais, documentos que podem ser anexados e declaração. Não é assinatura digital:
 * o nome, a data e a caixa "Declaro..." substituem a linha de assinatura do papel (confirmar com a equipe técnica).
 */
public record Bloco8Dados(
        @Size(max = 5000, message = "Máximo de 5000 caracteres") String observacoes,
        Set<DocumentoAnexavel> documentos,
        @Size(max = 150, message = "Máximo de 150 caracteres") String assinaturaNome,
        LocalDate assinaturaData,
        Boolean declaracao,
        Long version,
        LocalDateTime atualizadoEm) implements BlocoDados<Bloco8Dados> {

    public static Bloco8Dados vazio() {
        return new Bloco8Dados(null, Set.of(), null, null, false, null, null);
    }

    @Override
    public Bloco8Dados comControle(Long novaVersao, LocalDateTime novoAtualizadoEm) {
        return new Bloco8Dados(observacoes, documentos, assinaturaNome, assinaturaData, declaracao, novaVersao, novoAtualizadoEm);
    }

    @Override
    public Bloco8Dados normalizado() {
        return new Bloco8Dados(Normaliza.txt(observacoes), Normaliza.conjunto(documentos), Normaliza.txt(assinaturaNome),
                assinaturaData, Boolean.TRUE.equals(declaracao), version, atualizadoEm);
    }

    @Override
    public void validarRegras() {
        // Sem regras que travem o rascunho.
    }

    @Override
    public List<Pendencia> pendencias() {
        List<Pendencia> lista = new ArrayList<>();
        if (Normaliza.vazio(assinaturaNome)) lista.add(Pendencia.obrigatorio(8, "assinaturaNome", "Assinatura: informe o nome do(a) secretário(a)"));
        if (assinaturaData == null) lista.add(Pendencia.obrigatorio(8, "assinaturaData", "Assinatura: informe a data"));
        if (!Boolean.TRUE.equals(declaracao)) lista.add(Pendencia.obrigatorio(8, "declaracao", "Assinatura: marque a declaração de que as informações são verdadeiras"));
        return lista;
    }
}
