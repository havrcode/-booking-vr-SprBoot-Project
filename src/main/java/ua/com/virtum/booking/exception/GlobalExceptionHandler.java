package ua.com.virtum.booking.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<?> onNotFound(NotFoundException ex) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<?> onConflict(ConflictException ex) {
        return error(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<?> onBadRequest(BadRequestException ex) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler({MaxUploadSizeExceededException.class, MultipartException.class})
    public ResponseEntity<?> onMultipartError(Exception ex) {
        return error(HttpStatus.BAD_REQUEST, "Не вдалося завантажити скрін оплати. Додайте зображення до 8 МБ.");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> onValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fields = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fields.put(fieldError.getField(), validationMessage(fieldError));
        }
        Map<String, Object> body = new HashMap<>();
        body.put("status", 400);
        body.put("error", "Перевірте дані у формі бронювання.");
        body.put("fields", fields);
        body.put("timestamp", Instant.now());
        return ResponseEntity.badRequest().body(body);
    }

    private String validationMessage(FieldError fieldError) {
        String message = fieldError.getDefaultMessage();

        if (message != null && !message.isBlank()) {
            return message;
        }

        return switch (fieldError.getField()) {
            case "serviceSlug" -> "Оберіть послугу.";
            case "customerName" -> "Вкажіть ім'я.";
            case "customerPhone" -> "Вкажіть коректний номер телефону.";
            case "customerEmail" -> "Вкажіть коректний email або залиште поле порожнім.";
            case "customerComment" -> "Коментар має бути до 500 символів.";
            case "startsAt" -> "Оберіть час бронювання.";
            case "helmetsCount" -> "Оберіть кількість шоломів.";
            default -> "Перевірте це поле.";
        };
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", status.value());
        body.put("error", message);
        body.put("timestamp", Instant.now());
        return ResponseEntity.status(status).body(body);
    }
}
