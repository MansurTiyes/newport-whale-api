package com.mansurtiyes.newportwhaleapi.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.OffsetDateTime;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 404: your NotFoundException (top-level) AND/OR service’s inner NotFoundException
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(RuntimeException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), req);
    }

    // 400: bad input / binding / validation
    @ExceptionHandler({
            IllegalArgumentException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class,
            ConversionFailedException.class,
            ConstraintViolationException.class,
            java.time.format.DateTimeParseException.class
    })
    public ResponseEntity<Map<String, Object>> handleBadRequest(Exception ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, safeMessage(ex), req);
    }

    // 405: method not allowed
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex, HttpServletRequest req) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, "Method not allowed", req);
    }

    // 503: DB connectivity / resource failures
    @ExceptionHandler({
            CannotGetJdbcConnectionException.class,
            DataAccessResourceFailureException.class
    })
    public ResponseEntity<Map<String, Object>> handleDbUnavailable(Exception ex, HttpServletRequest req) {
        return build(HttpStatus.SERVICE_UNAVAILABLE, "Database is temporarily unavailable. Please retry.", req);
    }

    // 400 vs 500 for SQL problems
    @ExceptionHandler(UncategorizedSQLException.class)
    public ResponseEntity<Map<String, Object>> handleUncategorizedSql(UncategorizedSQLException ex, HttpServletRequest req) {
        String msg = ex.getSQLException() != null ? ex.getSQLException().getMessage() : ex.getMessage();
        if (msg != null && msg.toLowerCase().contains("invalid input value for enum")) {
            return build(HttpStatus.BAD_REQUEST, "Invalid value for enum parameter.", req);
        }
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Internal database error.", req);
    }

    // 500: SQL grammar (developer bug)
    @ExceptionHandler(BadSqlGrammarException.class)
    public ResponseEntity<Map<String, Object>> handleSqlGrammar(BadSqlGrammarException ex, HttpServletRequest req) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Internal query error.", req);
    }

    // 500: catch-all for remaining DataAccess exceptions
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, Object>> handleDataAccess(DataAccessException ex, HttpServletRequest req) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Database error.", req);
    }

    // 500: last resort
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleAny(Exception ex, HttpServletRequest req) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error.", req);
    }

    // -------- helpers --------

    private ResponseEntity<Map<String, Object>> build(HttpStatus status, String message, HttpServletRequest req) {
        Map<String, Object> body = Map.of(
                "timestamp", OffsetDateTime.now().toString(),
                "status", status.value(),
                "error", status.getReasonPhrase(),
                "message", (message == null || message.isBlank()) ? defaultMessage(status) : truncate(message, 500),
                "path", req.getRequestURI()
        );
        return ResponseEntity.status(status).body(body);
    }

    private String safeMessage(Exception ex) {
        String m = ex.getMessage();
        return (m == null || m.isBlank()) ? "Invalid request." : truncate(m, 500);
    }

    private String truncate(String s, int max) {
        return (s.length() <= max) ? s : s.substring(0, max) + "…";
    }

    private String defaultMessage(HttpStatus status) {
        return switch (status) {
            case BAD_REQUEST -> "Invalid request.";
            case NOT_FOUND -> "Resource not found.";
            case SERVICE_UNAVAILABLE -> "Service unavailable.";
            default -> "An error occurred.";
        };
    }
}
