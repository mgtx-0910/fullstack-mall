package com.example.fullstackmall.contract.product;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 商品状态变更请求。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductStatusChangeRequest {

    /** 前端希望到达的目标状态；服务端仍会校验状态机、分类和可售 SKU。 */
    @NotNull(message = "目标状态不能为空")
    private ProductStatus targetStatus;
}
