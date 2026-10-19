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

/** Número decimal (dias, horas), zero ou maior, até 2 casas. */
@Documented
@Constraint(validatedBy = {})
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@PositiveOrZero
@Digits(integer = 7, fraction = 2)
@ReportAsSingleViolation
public @interface Decimal {
    String message() default "Use um número, zero ou maior (até 2 casas decimais)";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
