package com.example.fullstackmall.contract.payment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 本地模拟支付平台发送的回调请求。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MockPaymentCallbackRequest {

    /** 模拟支付平台回传的支付流水号，服务端据此锁定支付记录。 */
    @NotBlank(message = "支付流水号不能为空")
    @Size(max = 64, message = "支付流水号不能超过 64 个字符")
    private String paymentNo;

    /** 本次回调事件的唯一标识，用于防止同一事件被重复处理或串用。 */
    @NotBlank(message = "回调事件 ID 不能为空")
    @Size(max = 64, message = "回调事件 ID 不能超过 64 个字符")
    private String callbackId;

    /** 模拟支付平台生成的交易号，成功后保存到支付流水。 */
    @NotBlank(message = "支付平台交易号不能为空")
    @Size(max = 64, message = "支付平台交易号不能超过 64 个字符")
    private String providerTransactionNo;

    /** 支付平台回调结果；阶段 8 只支持 SUCCESS。 */
    @NotNull(message = "回调结果不能为空")
    private PaymentCallbackResult result;
}
