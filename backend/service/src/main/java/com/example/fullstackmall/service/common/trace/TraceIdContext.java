package com.example.fullstackmall.service.common.trace;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 从当前 HTTP 请求读取 traceId。
 */
public final class TraceIdContext {

    private TraceIdContext() {
    }

    public static String get(HttpServletRequest request) {
        Object traceId = request.getAttribute(TraceIdFilter.TRACE_ID_ATTRIBUTE);
        if (traceId instanceof String value && !value.isBlank()) {
            return value;
        }
        return "unknown";
    }
}
