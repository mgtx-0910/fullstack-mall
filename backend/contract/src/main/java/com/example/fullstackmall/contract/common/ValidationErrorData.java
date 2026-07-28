package com.example.fullstackmall.contract.common;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 参数校验失败时返回的详情。
 */
@Data
@NoArgsConstructor
public class ValidationErrorData {

    /** 一次请求中所有字段校验错误，前端可以逐项展示。 */
    private List<FieldValidationError> fieldErrors;

    @Builder
    public ValidationErrorData(List<FieldValidationError> fieldErrors) {
        this.fieldErrors = List.copyOf(fieldErrors);
    }

    public void setFieldErrors(List<FieldValidationError> fieldErrors) {
        this.fieldErrors = List.copyOf(fieldErrors);
    }
}
