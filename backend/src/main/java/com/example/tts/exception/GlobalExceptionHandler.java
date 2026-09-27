package com.example.tts.exception;

import com.example.tts.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Centralized Exception Handler for Text-to-Speech REST API.
 * Ensures consistent, clean, and secure error formatting across the application:
 * {
 *   "success": false,
 *   "error": {
 *     "code": "INVALID_REQUEST",
 *     "message": "Text must not be empty"
 *   }
 * }
 *
 * Security:
 * - API keys, provider tokens, and internal stack traces are NEVER leaked to the client.
 * - Server-side details are securely logged at WARN/ERROR levels with sanitized user messages.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // =========================================================================
    // 400 BAD REQUEST HANDLERS
    // =========================================================================

    /**
     * Handle Bean Validation failures (@Valid on DTOs)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        log.warn("Validation failure on request body: {}", fieldErrors);

        String firstMessage = fieldErrors.values().stream().findFirst().orElse("Validation failed for input data");

        ErrorResponse response = ErrorResponse.of("INVALID_REQUEST", firstMessage, fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handle missing or malformed JSON payload
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMessageNotReadable(HttpMessageNotReadableException ex) {
        log.warn("Malformed or missing JSON request body: {}", ex.getMessage());

        ErrorResponse response = ErrorResponse.of(
                "INVALID_REQUEST",
                "Required request body is missing or contains malformed JSON"
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handle generic application-level bad requests
     */
    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRequest(InvalidRequestException ex) {
        log.warn("Invalid request: {}", ex.getMessage());

        ErrorResponse response = ErrorResponse.of("INVALID_REQUEST", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handle invalid or unsupported language requests
     */
    @ExceptionHandler(InvalidLanguageException.class)
    public ResponseEntity<ErrorResponse> handleInvalidLanguage(InvalidLanguageException ex) {
        log.warn("Invalid language requested: {}", ex.getMessage());

        ErrorResponse response = ErrorResponse.of("INVALID_LANGUAGE", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handle invalid or unsupported voice requests
     */
    @ExceptionHandler(InvalidVoiceException.class)
    public ResponseEntity<ErrorResponse> handleInvalidVoice(InvalidVoiceException ex) {
        log.warn("Invalid voice requested: {}", ex.getMessage());

        ErrorResponse response = ErrorResponse.of("INVALID_VOICE", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handle voice-language incompatibility
     */
    @ExceptionHandler(VoiceLanguageMismatchException.class)
    public ResponseEntity<ErrorResponse> handleVoiceLanguageMismatch(VoiceLanguageMismatchException ex) {
        log.warn("Voice-language mismatch: voice='{}', language='{}'", ex.getVoice(), ex.getLanguage());

        ErrorResponse response = ErrorResponse.of("VOICE_LANGUAGE_MISMATCH", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // =========================================================================
    // 401 UNAUTHORIZED & 403 FORBIDDEN HANDLERS
    // =========================================================================

    /**
     * Handle duplicate user registration
     */
    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleUserAlreadyExists(UserAlreadyExistsException ex) {
        log.warn("User registration conflict: {}", ex.getMessage());

        ErrorResponse response = ErrorResponse.of(
                "USER_ALREADY_EXISTS",
                ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    /**
     * Handle invalid credentials or authentication failures
     */
    @ExceptionHandler({
            InvalidCredentialsException.class,
            org.springframework.security.authentication.BadCredentialsException.class
    })
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(Exception ex) {
        log.warn("Invalid credentials attempted: {}", ex.getMessage());

        ErrorResponse response = ErrorResponse.of(
                "INVALID_CREDENTIALS",
                "Invalid email or password"
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    /**
     * Handle 401 Unauthorized
     */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(UnauthorizedException ex) {
        log.warn("Unauthorized access attempt: {}", ex.getMessage());

        ErrorResponse response = ErrorResponse.of(
                "UNAUTHORIZED",
                ex.getMessage() != null ? ex.getMessage() : "Authentication is required to access this resource"
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    /**
     * Handle 403 Forbidden and AccessDeniedException
     */
    @ExceptionHandler({ForbiddenException.class, org.springframework.security.access.AccessDeniedException.class})
    public ResponseEntity<ErrorResponse> handleForbidden(Exception ex) {
        log.warn("Forbidden access attempt: {}", ex.getMessage());

        ErrorResponse response = ErrorResponse.of(
                "FORBIDDEN",
                ex.getMessage() != null ? ex.getMessage() : "Access to this resource is denied"
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    // =========================================================================
    // 404 NOT FOUND & 405 METHOD NOT ALLOWED & 415 MEDIA TYPE HANDLERS
    // =========================================================================

    /**
     * Handle 404 Resource Not Found
     */
    @ExceptionHandler({ResourceNotFoundException.class, NoResourceFoundException.class})
    public ResponseEntity<ErrorResponse> handleNotFound(Exception ex) {
        log.warn("Resource not found: {}", ex.getMessage());

        ErrorResponse response = ErrorResponse.of(
                "NOT_FOUND",
                "The requested resource was not found on the server"
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * Handle 405 Method Not Allowed
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        log.warn("HTTP method not supported: {}", ex.getMessage());

        ErrorResponse response = ErrorResponse.of(
                "METHOD_NOT_ALLOWED",
                "HTTP method '" + ex.getMethod() + "' is not supported for this endpoint"
        );
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(response);
    }

    /**
     * Handle 415 Unsupported Media Type (invalid Content-Type)
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException ex) {
        log.warn("Unsupported Content-Type: {}", ex.getContentType());

        ErrorResponse response = ErrorResponse.of(
                "UNSUPPORTED_MEDIA_TYPE",
                "Content-Type '" + ex.getContentType() + "' is not supported"
        );
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(response);
    }

    /**
     * Handle unsupported uploaded document formats (TXT, PDF, DOCX only)
     */
    @ExceptionHandler(UnsupportedDocumentException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedDocument(UnsupportedDocumentException ex) {
        log.warn("Unsupported document upload: {}", ex.getMessage());

        ErrorResponse response = ErrorResponse.of(
                "UNSUPPORTED_DOCUMENT_TYPE",
                ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(response);
    }

    /**
     * Handle file size limit exceeded on multipart upload (413 Payload Too Large)
     */
    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceeded(org.springframework.web.multipart.MaxUploadSizeExceededException ex) {
        log.warn("Document upload size exceeded limit: {}", ex.getMessage());

        ErrorResponse response = ErrorResponse.of(
                "FILE_SIZE_LIMIT_EXCEEDED",
                "Uploaded file exceeds the maximum allowed size limit of 10MB."
        );
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(response);
    }

    // =========================================================================
    // 429 TOO MANY REQUESTS HANDLER
    // =========================================================================

    /**
     * Handle 429 Rate Limit Exceeded
     */
    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleRateLimitExceeded(RateLimitExceededException ex) {
        log.warn("Rate limit exceeded: {}", ex.getMessage());

        ErrorResponse response = ErrorResponse.of(
                "TOO_MANY_REQUESTS",
                ex.getMessage() != null ? ex.getMessage() : "Too many requests. Please wait before making further requests."
        );
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(response);
    }

    /**
     * Handle type mismatch on request parameters or path variables (e.g. string passed instead of numeric ID)
     */
    @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatch(
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException ex) {
        log.warn("Parameter type mismatch for '{}': value='{}'", ex.getName(), ex.getValue());

        String message = String.format("Invalid parameter '%s': value '%s' is not valid", ex.getName(), ex.getValue());
        ErrorResponse response = ErrorResponse.of("INVALID_REQUEST", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handle ConstraintViolationException (e.g. @Min, @Positive violations on path variables)
     */
    @ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(jakarta.validation.ConstraintViolationException ex) {
        log.warn("Constraint violation: {}", ex.getMessage());

        String message = ex.getConstraintViolations().stream()
                .map(jakarta.validation.ConstraintViolation::getMessage)
                .findFirst()
                .orElse("Validation failed for input parameter");

        ErrorResponse response = ErrorResponse.of("INVALID_REQUEST", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // =========================================================================
    // 503 SERVICE UNAVAILABLE & PROVIDER/DATABASE FAILURE HANDLERS
    // =========================================================================

    /**
     * Handle database connectivity or transaction failures gracefully
     */
    @ExceptionHandler({
            org.springframework.dao.DataAccessException.class,
            org.springframework.transaction.TransactionException.class,
            org.hibernate.HibernateException.class,
            java.sql.SQLException.class
    })
    public ResponseEntity<ErrorResponse> handleDatabaseExceptions(Exception ex) {
        log.error("Database connection/transaction failure: {}", ex.getMessage());

        ErrorResponse response = ErrorResponse.of(
                "SERVICE_UNAVAILABLE",
                "Database service is temporarily unavailable. Please verify PostgreSQL connection."
        );
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    /**
     * Handle 503 Service Unavailable (custom service exception)
     */
    @ExceptionHandler(ServiceUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleServiceUnavailable(ServiceUnavailableException ex) {
        log.error("Service unavailable: {}", ex.getMessage());

        ErrorResponse response = ErrorResponse.of(
                "SERVICE_UNAVAILABLE",
                ex.getMessage() != null ? ex.getMessage() : "Text-to-speech service is temporarily unavailable. Please try again later."
        );
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    /**
     * Handle external TTS provider timeout (sanitized, zero credentials leaked)
     */
    @ExceptionHandler(TtsTimeoutException.class)
    public ResponseEntity<ErrorResponse> handleTtsTimeoutException(TtsTimeoutException ex) {
        log.error("External TTS Provider timed out: {}", ex.getMessage());

        ErrorResponse response = ErrorResponse.of(
                "SERVICE_UNAVAILABLE",
                "Speech synthesis request timed out while communicating with provider. Please try again."
        );
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    /**
     * Handle external TTS provider downtime or connection failure
     */
    @ExceptionHandler(TtsProviderUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleTtsProviderUnavailable(TtsProviderUnavailableException ex) {
        log.error("External TTS Provider unavailable: {}", ex.getMessage());

        ErrorResponse response = ErrorResponse.of(
                "SERVICE_UNAVAILABLE",
                "The speech synthesis provider is temporarily unavailable. Please try again shortly."
        );
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    /**
     * Handle external TTS provider authentication issues safely without exposing API keys or secrets
     */
    @ExceptionHandler(TtsAuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleTtsAuthException(TtsAuthenticationException ex) {
        // Securely log provider failure without exposing to client
        log.error("TTS Provider authentication failed on backend: {}", ex.getMessage());

        ErrorResponse response = ErrorResponse.of(
                "SERVICE_UNAVAILABLE",
                "Speech synthesis provider authentication failure. Please contact administrator."
        );
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    /**
     * Handle external TTS provider synthesis errors safely
     */
    @ExceptionHandler(TtsSynthesisException.class)
    public ResponseEntity<ErrorResponse> handleTtsSynthesisException(TtsSynthesisException ex) {
        log.error("TTS Provider synthesis failed: {}", ex.getMessage());

        ErrorResponse response = ErrorResponse.of(
                "SYNTHESIS_FAILED",
                "Failed to synthesize audio with speech provider. Please check input parameters and try again."
        );
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(response);
    }

    /**
     * Handle custom ApiException with dynamic status codes
     */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException ex) {
        log.warn("API Exception ({}): {}", ex.getStatus(), ex.getMessage());

        String code = ex.getStatus().is4xxClientError() ? "INVALID_REQUEST" : "INTERNAL_SERVER_ERROR";
        ErrorResponse response = ErrorResponse.of(code, ex.getMessage());
        return ResponseEntity.status(ex.getStatus()).body(response);
    }

    // =========================================================================
    // 500 INTERNAL SERVER ERROR (FALLTHROUGH)
    // =========================================================================

    /**
     * Handle generic unexpected internal server errors.
     * Stacks traces and internal details are never exposed to the client.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(Exception ex) {
        log.error("Unhandled internal server error occurred", ex);

        ErrorResponse response = ErrorResponse.of(
                "INTERNAL_SERVER_ERROR",
                "An unexpected internal server error occurred. Please try again later."
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
