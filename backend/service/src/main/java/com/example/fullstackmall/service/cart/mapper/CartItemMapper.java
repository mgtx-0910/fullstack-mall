package com.example.fullstackmall.service.cart.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.fullstackmall.service.cart.entity.CartItemEntity;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;

/**
 * 购物车表 Mapper。简单 CRUD 由 MyBatis-Plus BaseMapper 提供。
 */
@Mapper
public interface CartItemMapper extends BaseMapper<CartItemEntity> {

    /**
     * 只删除指定用户、本次已勾选且 ID 在集合中的购物车项。
     * 返回删除行数，事务服务据此判断下单过程中购物车是否发生并发变化。
     */
    @Delete({
            "<script>",
            "DELETE FROM mall_cart_item",
            "WHERE user_id = #{userId}",
            "AND checked = 1",
            "AND id IN",
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>",
            "#{id}",
            "</foreach>",
            "</script>"
    })
    int removeCheckedByUserIdAndIds(
            @Param("userId") Long userId,
            @Param("ids") Collection<Long> ids
    );
}
