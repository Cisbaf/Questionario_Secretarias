package questionario.exception;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import questionario.api.dto.ApiErrorResponse;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiErrorResponse> validation(MethodArgumentNotValidException exception) {
        Map<String, String> fields = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors()
                .forEach(error -> fields.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return ResponseEntity.badRequest().body(new ApiErrorResponse("Dados inválidos", fields));
    }

    @ExceptionHandler(DadosInvalidosException.class)
    ResponseEntity<ApiErrorResponse> invalidData(DadosInvalidosException exception) {
        return ResponseEntity.badRequest().body(new ApiErrorResponse(exception.getMessage(), exception.getCampos()));
    }

    @ExceptionHandler(EnvioIncompletoException.class)
    ResponseEntity<ApiErrorResponse> incompleteSubmission(EnvioIncompletoException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                new ApiErrorResponse(exception.getMessage(), exception.getPendencias(), EnvioIncompletoException.CODIGO));
    }

    @ExceptionHandler(CredenciaisInvalidasException.class)
    ResponseEntity<ApiErrorResponse> badCredentials(CredenciaisInvalidasException exception) {
        return response(HttpStatus.UNAUTHORIZED, exception.getMessage());
    }

    @ExceptionHandler(LoginBloqueadoException.class)
    ResponseEntity<ApiErrorResponse> loginBlocked(LoginBloqueadoException exception) {
        return response(HttpStatus.TOO_MANY_REQUESTS, exception.getMessage());
    }

    @ExceptionHandler(EntityNotFoundException.class)
    ResponseEntity<ApiErrorResponse> notFound(EntityNotFoundException exception) {
        return response(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiErrorResponse> databaseConflict(DataIntegrityViolationException exception) {
        return response(HttpStatus.CONFLICT, "Operação não pôde ser concluída por conflito de dados");
    }

    @ExceptionHandler(ConflictException.class)
    ResponseEntity<ApiErrorResponse> businessConflict(ConflictException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiErrorResponse(exception.getMessage(), null, exception.getCodigo()));
    }

    /** Duas pessoas salvando o mesmo bloco ao mesmo tempo: quem chegou depois recebe o mesmo 409 da versão velha. */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ResponseEntity<ApiErrorResponse> optimisticLock() {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiErrorResponse(
                "Este bloco foi alterado por outra pessoa. Recarregue para ver a versão mais recente.",
                null, ConflictException.VERSAO_DESATUALIZADA));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ApiErrorResponse> badRequest(IllegalArgumentException exception) {
        return response(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiErrorResponse> badMessageRequest() {
        return response(HttpStatus.BAD_REQUEST, "Corpo da requisição ausente ou malformado");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ApiErrorResponse> argumentType(MethodArgumentTypeMismatchException exception) {
        var tipo = exception.getRequiredType();
        String dica;
        if (tipo == LocalDate.class) {
            dica = ": use uma data no formato AAAA-MM-DD";
        } else if (tipo != null && Number.class.isAssignableFrom(tipo)) {
            dica = ": use um número inteiro";
        } else {
            dica = ": valor não reconhecido";
        }
        return response(HttpStatus.BAD_REQUEST, "Parâmetro '" + exception.getName() + "' inválido" + dica);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    ResponseEntity<ApiErrorResponse> missingParameter(MissingServletRequestParameterException exception) {
        return response(HttpStatus.BAD_REQUEST, "Parâmetro obrigatório ausente: '" + exception.getParameterName() + "'");
    }

    private ResponseEntity<ApiErrorResponse> response(HttpStatus status, String message) {
        String responseMessage = message == null ? status.getReasonPhrase() : message;
        return ResponseEntity.status(status).body(new ApiErrorResponse(responseMessage));
    }
}
