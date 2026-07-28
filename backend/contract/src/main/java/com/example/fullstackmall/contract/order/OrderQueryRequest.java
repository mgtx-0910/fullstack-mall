package com.example.fullstackmall.contract.order;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理员订单分页查询条件。
 */
@Data
public class OrderQueryRequest {

    /** 请求页码，默认从第 1 页开始。 */
    @Min(value = 1, message = "页码不能小于 1")
    private Integer pageNum = 1;

    /** 每页数量，默认 10，最多 100。 */
    @Min(value = 1, message = "每页数量不能小于 1")
    @Max(value = 100, message = "每页数量不能大于 100")
    private Integer pageSize = 10;

    /** 可选订单号关键字；DbService 会 trim 后参与模糊查询。 */
    @Size(max = 64, message = "订单号关键字不能超过 64 个字符")
    private String orderNo;

    /** 可选用户 ID 筛选条件。 */
    @Positive(message = "用户 ID 必须大于 0")
    private Long userId;

    /** 可选订单状态筛选条件。 */
    private OrderStatus status;
}
