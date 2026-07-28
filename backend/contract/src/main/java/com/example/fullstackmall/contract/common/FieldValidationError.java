package com.example.fullstackmall.contract.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 单个请求字段的校验错误。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldValidationError {

    /** 校验失败的 Java 字段名，前端可用来定位对应表单项。 */
    private String field;
    /** 该字段违反的校验规则提示。 */
    private String message;
}
