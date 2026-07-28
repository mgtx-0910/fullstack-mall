package com.example.fullstackmall.contract.health;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 健康检查响应。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthResponse {

    /** 应用健康状态；当前示例正常启动时返回 UP。 */
    private String status;
    /** 当前后端应用名称，帮助确认请求命中了哪个服务。 */
    private String application;
    /** 健康检查响应生成时间，由服务端产生。 */
    private Instant timestamp;
}
