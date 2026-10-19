package questionario.service.excel;

import java.math.BigDecimal;

/** Valor em reais: vai para a planilha como numero com formato R$ (e nao como texto). */
record Reais(BigDecimal valor) {

    static Object de(BigDecimal valor) {
        return valor == null ? null : new Reais(valor);
    }
}
