package questionario.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import questionario.domain.enums.Impedimento;
import questionario.domain.enums.LinhaCuidado;
import questionario.domain.enums.TemaUrgencia;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Corpo do PUT do Bloco 2. No rascunho tudo é opcional (D5); valem formato, tamanho e as regras entre campos de
 * {@code Bloco2Validacao} (nota de 1 a 5, até 3 impedimentos, ordem sem repetição). {@code version} é a versão que o
 * cliente tinha ({@code null} no primeiro salvamento).
 */
public record Bloco2Request(
        @Size(max = 5, message = "Informe no máximo 5 prioridades") @Valid List<PrioridadeDto> prioridades,
        Map<LinhaCuidado, Integer> grausLinhaCuidado,
        @Size(max = 150, message = "Máximo de 150 caracteres") String linhaOutraDescricao,
        @Size(max = 5000, message = "Máximo de 5000 caracteres") String gargalos,
        @Size(max = 5000, message = "Máximo de 5000 caracteres") String servicosParaVizinhos,
        Set<Impedimento> impedimentos,
        @Size(max = 150, message = "Máximo de 150 caracteres") String impedimentoOutro,
        Map<TemaUrgencia, Integer> ordemTemas,
        Long version) {
}
