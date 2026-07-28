package com.example.fullstackmall.service.common.exception;

import com.example.fullstackmall.contract.common.ApiCode;
import com.example.fullstackmall.contract.common.ApiResponse;
import com.example.fullstackmall.contract.common.FieldValidationError;
import com.example.fullstackmall.contract.common.ValidationErrorData;
import com.example.fullstackmall.service.common.trace.TraceIdContext;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

/**
 * 将 Java 异常统一转换为稳定的 HTTP 状态码和 API 响应。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理 {@code @Valid} 触发的 Bean Validation 错误，并把多个字段错误统一返回给前端。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<ValidationErrorData>> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        List<FieldValidationError> fieldErrors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> new FieldValidationError(
                        error.getField(),
                        error.getDefaultMessage() == null ? "字段值不合法" : error.getDefaultMessage()
                ))
                .toList();

        ApiResponse<ValidationErrorData> response = ApiResponse.error(
                ApiCode.VALIDATION_ERROR,
                ApiCode.VALIDATION_ERROR.defaultMessage(),
                new ValidationErrorData(fieldErrors),
                TraceIdContext.get(request)
        );
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * JSON 语法错误、字段类型不匹配或枚举转换失败都会在进入 Controller 方法前到达这里。
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleMalformedJson(
            HttpMessageNotReadableException exception,
            HttpServletRequest request
    ) {
        ApiResponse<Void> response = ApiResponse.error(
                ApiCode.MALFORMED_JSON,
                ApiCode.MALFORMED_JSON.defaultMessage(),
                null,
                TraceIdContext.get(request)
        );
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * 可预期的业务失败保留原业务码、提示和 HTTP 状态，不应被错误包装成 500。
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(
            BusinessException exception,
            HttpServletRequest request
    ) {
        ApiResponse<Void> response = ApiResponse.error(
                exception.getCode(),
                exception.getMessage(),
                null,
                TraceIdContext.get(request)
        );
        return ResponseEntity.status(exception.getHttpStatus()).body(response);
    }

    /**
     * 请求路径不存在时也返回项目统一响应，而不是暴露 Spring 默认错误页。
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(
            NoResourceFoundException exception,
            HttpServletRequest request
    ) {
        ApiResponse<Void> response = ApiResponse.error(
                ApiCode.NOT_FOUND,
                ApiCode.NOT_FOUND.defaultMessage(),
                null,
                TraceIdContext.get(request)
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * 最后的兜底异常处理：详细堆栈只写服务端日志，对外只返回通用错误，避免泄露内部实现。
     * traceId 同时出现在日志和响应中，方便前后端一起定位问题。
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnknownException(
            Exception exception,
            HttpServletRequest request
    ) {
        String traceId = TraceIdContext.get(request);
        log.error("未处理的服务端异常，traceId={}", traceId, exception);

        ApiResponse<Void> response = ApiResponse.error(
                ApiCode.INTERNAL_ERROR,
                ApiCode.INTERNAL_ERROR.defaultMessage(),
                null,
                traceId
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
