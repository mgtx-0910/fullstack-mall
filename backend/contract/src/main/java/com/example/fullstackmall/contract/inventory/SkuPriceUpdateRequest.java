package com.example.fullstackmall.contract.inventory;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 按调用者读取到的版本修改 SKU 售价。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkuPriceUpdateRequest {

    /** 前端希望修改的新售价；服务端会按两位小数规范化后写库。 */
    @NotNull(message = "销售价不能为空")
    @DecimalMin(value = "0.01", message = "销售价必须大于 0")
    @Digits(integer = 10, fraction = 2, message = "销售价最多 10 位整数和 2 位小数")
    private BigDecimal salePrice;

    /** 前端上次读取到的版本号，用于乐观锁；版本过期时更新影响 0 行并返回冲突。 */
    @NotNull(message = "期望版本不能为空")
    @Min(value = 0, message = "期望版本不能小于 0")
    private Integer expectedVersion;
}
