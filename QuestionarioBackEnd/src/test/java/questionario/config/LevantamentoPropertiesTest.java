package questionario.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Regras da capa (app.levantamento.*) sem subir o Spring: o que é "a definir", o que é erro e o que o erro diz. */
class LevantamentoPropertiesTest {

    private static LevantamentoProperties capa(String inicio, String fim, String censo, String prazo) {
        return new LevantamentoProperties("Título", inicio, fim, censo, prazo, "planejamento@cisbaf.org.br");
    }

    @Test
    @DisplayName("Vazio ou em branco vira nulo, que a tela mostra como 'a definir'")
    void vazioViraNulo() {
        LevantamentoProperties props = capa("", "   ", null, "");

        assertThat(props.inicio()).isNull();
        assertThat(props.fim()).isNull();
        assertThat(props.censoUpa()).isNull();
        assertThat(props.prazo()).isNull();
        assertThatCode(props::validar).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Datas AAAA-MM-DD são lidas, mesmo com espaços em volta")
    void datasValidas() {
        LevantamentoProperties props = capa(" 2026-01-01 ", "2026-06-30", "2026-11-05", "2026-11-30");

        assertThat(props.inicio()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(props.fim()).isEqualTo(LocalDate.of(2026, 6, 30));
        assertThat(props.censoUpa()).isEqualTo(LocalDate.of(2026, 11, 5));
        assertThat(props.prazo()).isEqualTo(LocalDate.of(2026, 11, 30));
    }

    @Test
    @DisplayName("Data em outro formato falha dizendo qual propriedade e qual o formato certo")
    void formatoInvalido() {
        LevantamentoProperties props = capa(null, null, "05/11/2026", null);

        assertThatThrownBy(props::validar)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.levantamento.data-censo-upa")
                .hasMessageContaining("05/11/2026")
                .hasMessageContaining("AAAA-MM-DD");
    }

    @Test
    @DisplayName("Dia que não existe no calendário também falha")
    void diaInexistente() {
        LevantamentoProperties props = capa(null, null, null, "2026-02-30");

        assertThatThrownBy(props::validar)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.levantamento.prazo-devolucao");
    }

    @Test
    @DisplayName("Período que termina antes de começar falha")
    void fimAntesDoInicio() {
        LevantamentoProperties props = capa("2026-06-30", "2026-01-01", null, null);

        assertThatThrownBy(props::validar)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("periodo-referencia-fim");
    }

    @Test
    @DisplayName("Período de um dia só (fim igual ao início) é aceito")
    void fimIgualAoInicio() {
        assertThatCode(() -> capa("2026-03-10", "2026-03-10", null, null).validar()).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Só o início ou só o fim informado não é erro")
    void soUmaPonta() {
        assertThatCode(() -> capa("2026-03-10", null, null, null).validar()).doesNotThrowAnyException();
        assertThatCode(() -> capa(null, "2026-03-10", null, null).validar()).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Título vazio usa o título padrão; e-mail em branco vira nulo")
    void tituloEEnviarPara() {
        LevantamentoProperties props = new LevantamentoProperties("  ", null, null, null, null, "   ");

        assertThat(props.tituloOuPadrao()).isEqualTo(LevantamentoProperties.TITULO_PADRAO);
        assertThat(props.enviarParaOuNulo()).isNull();
        assertThat(new LevantamentoProperties(" Meu título ", null, null, null, null, " a@b.c ").tituloOuPadrao())
                .isEqualTo("Meu título");
    }
}
