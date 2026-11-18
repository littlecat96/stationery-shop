
package com.campus.stationery.repository;

import com.campus.stationery.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository
        extends JpaRepository<CartItem, Long> {

    // 查询当前用户的购物车
    List<CartItem> findBySessionId(String sessionId);

    // 查询购物车中是否已经存在该商品
    Optional<CartItem> findBySessionIdAndProductId(
            String sessionId, Long productId);

    // 删除购物车中的指定商品
    void deleteBySessionIdAndProductId(
            String sessionId, Long productId);

    // 清空当前用户购物车
    void deleteBySessionId(String sessionId);
}
