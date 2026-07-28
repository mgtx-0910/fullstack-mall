package com.example.fullstackmall.service.common.exception;

import com.example.fullstackmall.contract.common.ApiCode;
import org.springframework.http.HttpStatus;

/**
 * 表示请求格式正确，但违反了可预期的业务规则。
 */
public class BusinessException extends RuntimeException {

    /** 前端可以稳定判断的业务错误码，不应使用异常类名代替。 */
    private final ApiCode code;

    /** 同一个业务异常同时决定 HTTP 语义，例如不存在返回 404、冲突返回 409。 */
    private final HttpStatus httpStatus;

    /**
     * 未显式指定状态码时默认按业务冲突处理。
     */
    public BusinessException(ApiCode code, String message) {
        this(code, message, HttpStatus.CONFLICT);
    }

    public BusinessException(ApiCode code, String message, HttpStatus httpStatus) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }

    public ApiCode getCode() { return code; }
    public HttpStatus getHttpStatus() { return httpStatus; }
}
