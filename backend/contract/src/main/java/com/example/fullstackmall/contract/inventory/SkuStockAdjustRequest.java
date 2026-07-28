package com.example.fullstackmall.contract.inventory;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 调整可售库存。delta 为正表示入库，为负表示扣减。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkuStockAdjustRequest {

    /** 库存变化量；正数增加、负数减少，最终可售库存不能小于 0。 */
    @NotNull(message = "库存变化量不能为空")
    private Integer delta;

    /** 前端上次读取到的版本号，用于阻止并发覆盖。 */
    @NotNull(message = "期望版本不能为空")
    @Min(value = 0, message = "期望版本不能小于 0")
    private Integer expectedVersion;

    @AssertTrue(message = "库存变化量不能为 0")
    public boolean isDeltaNonZero() {
        return delta == null || delta != 0;
    }
}
