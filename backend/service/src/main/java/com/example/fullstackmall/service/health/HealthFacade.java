package com.example.fullstackmall.service.health;

import com.example.fullstackmall.contract.health.HealthResponse;
import com.example.fullstackmall.contract.health.IHealthFacade;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * 健康检查契约的具体实现。
 */
@Service
public class HealthFacade implements IHealthFacade {

    private final String applicationName;

    /**
     * 通过构造器注入配置文件中的应用名称。
     *
     * @param applicationName application.yml 中的 spring.application.name
     */
    public HealthFacade(
            @Value("${spring.application.name}") String applicationName
    ) {
        this.applicationName = applicationName;
    }

    @Override
    public HealthResponse health() {
        return new HealthResponse(
                "UP",
                applicationName,
                Instant.now()
        );
    }
}
