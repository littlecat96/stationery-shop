
package com.campus.stationery.service;

import com.campus.stationery.entity.CartItem;
import com.campus.stationery.entity.Product;
import com.campus.stationery.repository.CartItemRepository;
import com.campus.stationery.repository.ProductRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CartService {

    private final CartItemRepository cartRepository;
    private final ProductRepository productRepository;

    public CartService(
            CartItemRepository cartRepository,
            ProductRepository productRepository) {
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
    }

    // 查询购物车
    public List<CartItem> getCart(String sessionId) {
        return cartRepository.findBySessionId(sessionId);
    }

    // 加入购物车
    @Transactional
    public void addToCart(String sessionId, Long productId) {

        Product product = productRepository.findById(productId)
                .orElseThrow(() ->
                        new RuntimeException("商品不存在"));

        if (product.getStatus() == null || product.getStatus() != 1) {
            throw new RuntimeException("商品已下架");
        }

        if (product.getStock() == null || product.getStock() <= 0) {
            throw new RuntimeException("商品库存不足");
        }

        CartItem item = cartRepository
                .findBySessionIdAndProductId(sessionId, productId)
                .orElse(null);

        if (item == null) {
            item = new CartItem();
            item.setSessionId(sessionId);
            item.setProduct(product);
            item.setQuantity(1);
        } else {
            if (item.getQuantity() >= product.getStock()) {
                throw new RuntimeException("超过商品库存");
            }
            item.setQuantity(item.getQuantity() + 1);
        }

        cartRepository.save(item);
    }


    // 修改购物车数量
    @Transactional
    public void updateQuantity(
            String sessionId, Long productId, Integer quantity) {

        if (quantity == null || quantity < 1) {
            throw new RuntimeException("购买数量至少为1");
        }

        CartItem item = cartRepository
                .findBySessionIdAndProductId(sessionId, productId)
                .orElseThrow(() ->
                        new RuntimeException("购物车中没有该商品"));

        if (quantity > item.getProduct().getStock()) {
            throw new RuntimeException("超过商品库存");
        }

        item.setQuantity(quantity);
        cartRepository.save(item);
    }


    // 删除购物车商品
    @Transactional
    public void removeFromCart(String sessionId, Long productId) {
        cartRepository.deleteBySessionIdAndProductId(
                sessionId, productId);
    }

    // 清空购物车
    @Transactional
    public void clearCart(String sessionId) {
        cartRepository.deleteBySessionId(sessionId);
    }

}
