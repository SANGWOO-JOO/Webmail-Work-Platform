package dsn.webmail.exception;

import dsn.webmail.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(IllegalStateException ex) {
        String code = determineErrorCode(ex.getMessage());
        HttpStatus status = determineStatus(ex.getMessage());
        
        // Log business logic errors for monitoring
        logger.warn("Business logic error - Code: {}, Message: {}", code, ex.getMessage());
        
        ErrorResponse error = new ErrorResponse(
                code,
                sanitizeMessage(ex.getMessage()),
                "timestamp=" + LocalDateTime.now()
        );
        
        return ResponseEntity.status(status).body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        ErrorResponse error = new ErrorResponse(
                "AUTH-002",
                ex.getMessage(),
                "timestamp=" + LocalDateTime.now()
        );
        
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<ErrorResponse> handleValidationException(Exception ex) {
        String message = "입력값 검증에 실패했습니다.";
        
        if (ex instanceof MethodArgumentNotValidException validEx) {
            var fieldError = validEx.getBindingResult().getFieldError();
            if (fieldError != null) {
                message = fieldError.getDefaultMessage();
            }
        } else if (ex instanceof BindException bindEx) {
            var fieldError = bindEx.getBindingResult().getFieldError();
            if (fieldError != null) {
                message = fieldError.getDefaultMessage();
            }
        }
        
        ErrorResponse error = new ErrorResponse(
                "VALIDATION-001",
                message,
                "timestamp=" + LocalDateTime.now()
        );
        
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ResponseEntity<Void> handleNoResourceFoundException(org.springframework.web.servlet.resource.NoResourceFoundException ex) {
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception ex) {
        // Log detailed error information for debugging (not exposed to user)
        logger.error("Unexpected system error", ex);
        
        ErrorResponse error = new ErrorResponse(
                "SYSTEM-001",
                "시스템 오류가 발생했습니다.",
                "timestamp=" + LocalDateTime.now()
        );
        
        return ResponseEntity.internalServerError().body(error);
    }

    /**
     * Sanitize error messages to prevent information disclosure
     */
    private String sanitizeMessage(String message) {
        // Return the message as-is for now since these are controlled business logic messages
        // In production, consider mapping specific messages to generic ones
        return message;
    }

    private String determineErrorCode(String message) {
        if (message.contains("이미 가입된")) {
            return "AUTH-003";
        } else if (message.contains("만료")) {
            return "AUTH-001";
        } else if (message.contains("시도 횟수")) {
            return "AUTH-004";
        } else if (message.contains("60초")) {
            return "AUTH-005";
        } else if (message.contains("인증 요청이 없습니다")) {
            return "AUTH-006";
        } else if (message.contains("사용자를 찾을 수 없습니다")) {
            return "AUTH-007";
        } else if (message.contains("이미 방문 기록")) {
            return "MAP-001";
        }
        return "AUTH-999";
    }

    private HttpStatus determineStatus(String message) {
        if (message.contains("이미 가입된")) {
            return HttpStatus.CONFLICT;
        } else if (message.contains("인증 요청이 없습니다") || message.contains("사용자를 찾을 수 없습니다")) {
            return HttpStatus.NOT_FOUND;
        }
        return HttpStatus.BAD_REQUEST;
    }
}