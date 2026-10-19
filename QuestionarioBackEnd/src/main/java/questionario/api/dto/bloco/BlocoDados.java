package questionario.api.dto.bloco;

import questionario.api.dto.Pendencia;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Conteúdo de um bloco guardado como JSON (Blocos 3 a 8). Request e resposta são o mesmo objeto: na resposta
 * {@code version} e {@code atualizadoEm} vêm do servidor; no PUT só {@code version} é lida.
 */
public interface BlocoDados<T extends BlocoDados<T>> {

    Long version();

    LocalDateTime atualizadoEm();

    /** Cópia com o controle de versão trocado (o conteúdo gravado vai sem ele). */
    T comControle(Long version, LocalDateTime atualizadoEm);

    /** Cópia sem espaços sobrando, com campos vazios nulos e as linhas fixas completas. */
    T normalizado();

    /** Regras entre campos que dão 400 ao salvar (ex.: a mesma linha fixa repetida). */
    void validarRegras();

    /** Obrigatórios e coerências conferidos no envio final. */
    List<Pendencia> pendencias();
}
