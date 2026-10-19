package questionario.api.dto;

import questionario.domain.Levantamento;

import java.time.LocalDate;

/** Capa do questionário. Datas nulas significam "a definir". O id interno não sai. */
public record LevantamentoResponse(
        String titulo,
        LocalDate periodoReferenciaInicio,
        LocalDate periodoReferenciaFim,
        LocalDate dataCensoUpa,
        LocalDate prazoDevolucao,
        String enviarPara) {

    public static LevantamentoResponse from(Levantamento levantamento) {
        return new LevantamentoResponse(
                levantamento.getTitulo(),
                levantamento.getPeriodoReferenciaInicio(),
                levantamento.getPeriodoReferenciaFim(),
                levantamento.getDataCensoUpa(),
                levantamento.getPrazoDevolucao(),
                levantamento.getEnviarPara());
    }
}
