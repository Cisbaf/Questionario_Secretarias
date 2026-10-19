package questionario.api.dto.bloco;

import jakarta.validation.Constraint;
import jakarta.validation.ReportAsSingleViolation;
import jakarta.validation.Payload;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.PositiveOrZero;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Valor em reais, zero ou maior, com centavos (BigDecimal 17,2 no máximo). */
@Documented
@Constraint(validatedBy = {})
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@PositiveOrZero
@Digits(integer = 15, fraction = 2)
@ReportAsSingleViolation
public @interface Dinheiro {
    String message() default "Use um valor em reais, zero ou maior (até 2 casas decimais)";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
