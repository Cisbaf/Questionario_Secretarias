package questionario.api.dto.bloco;

import jakarta.validation.Constraint;
import jakarta.validation.ReportAsSingleViolation;
import jakarta.validation.Payload;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.PositiveOrZero;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Quantidade inteira, zero ou maior. */
@Documented
@Constraint(validatedBy = {})
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@PositiveOrZero
@Max(99_999_999)
@ReportAsSingleViolation
public @interface Contagem {
    String message() default "Use um número inteiro, zero ou maior";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
