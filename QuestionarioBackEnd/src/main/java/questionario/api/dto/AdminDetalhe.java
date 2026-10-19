package questionario.api.dto;

import java.util.List;

/** Tudo que o admin ve de um questionario: as respostas dos 8 blocos e o que ainda falta. */
public record AdminDetalhe(RespostasResponse respostas, List<Pendencia> pendencias) {
}
