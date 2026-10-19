package questionario.api.dto.bloco;

import jakarta.validation.Constraint;
import jakarta.validation.ReportAsSingleViolation;
import jakarta.validation.Payload;
import jakarta.validation.constraints.Pattern;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Código SIGTAP: 10 números ou vazio. */
@Documented
@Constraint(validatedBy = {})
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Pattern(regexp = "^$|^\\d{10}$")
@ReportAsSingleViolation
public @interface Sigtap {
    String message() default "O código SIGTAP tem 10 números";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
