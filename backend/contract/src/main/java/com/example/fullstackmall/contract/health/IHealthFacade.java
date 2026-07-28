package com.example.fullstackmall.contract.health;

/**
 * 健康检查对外契约。
 *
 * <p>contract 模块只描述“可以调用什么”和“返回什么”，不关心 Spring MVC、数据库等实现细节。</p>
 */
public interface IHealthFacade {

    /**
     * 获取当前应用的健康状态。
     *
     * @return 健康检查响应
     */
    HealthResponse health();
}
