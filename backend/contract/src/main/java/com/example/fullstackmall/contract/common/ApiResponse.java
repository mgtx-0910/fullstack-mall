package com.example.fullstackmall.contract.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 所有业务 API 共用的响应信封。
 *
 * @param <T> 业务数据类型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

    /** 稳定业务码，例如 SUCCESS、VALIDATION_ERROR；前端分支判断优先使用该字段。 */
    private String code;
    /** 面向人的提示文案，可能随产品文案调整，不适合作为程序判断条件。 */
    private String message;
    /** 成功数据或结构化错误详情；不同接口由泛型 T 决定具体类型。 */
    private T data;
    /** 本次请求的链路标识，前端报错时可把它提供给后端排查日志。 */
    private String traceId;
    /** 服务端生成响应的时间点，使用 UTC Instant 表达。 */
    private Instant timestamp;

    public static <T> ApiResponse<T> success(T data, String traceId) {
        return new ApiResponse<>(
                ApiCode.SUCCESS.name(),
                ApiCode.SUCCESS.defaultMessage(),
                data,
                traceId,
                Instant.now()
        );
    }

    public static <T> ApiResponse<T> error(ApiCode code, String message, T data, String traceId) {
        return new ApiResponse<>(code.name(), message, data, traceId, Instant.now());
    }
}
