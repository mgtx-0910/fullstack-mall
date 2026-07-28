package com.example.fullstackmall.contract.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 通用分页响应。
 *
 * @param <T> 当前页记录类型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {

    /** 当前页码，从 1 开始，来源于 MyBatis-Plus 分页结果。 */
    private long pageNum;
    /** 当前每页数量。 */
    private long pageSize;
    /** 满足查询条件的总记录数，不是当前页记录数。 */
    private long total;
    /** 根据 total 和 pageSize 计算出的总页数。 */
    private long pages;
    /** 当前页的数据列表；无结果时应返回空列表而不是 null。 */
    private List<T> records;
}
