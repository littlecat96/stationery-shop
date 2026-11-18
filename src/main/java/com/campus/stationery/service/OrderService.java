
package com.campus.stationery.service;

import com.campus.stationery.entity.CartItem;
import com.campus.stationery.entity.CustomerOrder;
import com.campus.stationery.entity.OrderItem;
import com.campus.stationery.entity.Product;
import com.campus.stationery.repository.CustomerOrderRepository;
import com.campus.stationery.repository.ProductRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

@Service
public class OrderService {

    private final CustomerOrderRepository orderRepository;
    private final CartService cartService;
    private final ProductRepository productRepository;

    public OrderService(
            CustomerOrderRepository orderRepository,
            CartService cartService,
            ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.cartService = cartService;
        this.productRepository = productRepository;
    }

    @Transactional
    public void updateStatus(Long id, String status) {
        CustomerOrder order = orderRepository.findByIdForUpdate(id).orElseThrow();
        if (status.equals(order.getStatus())) {
            return;
        }

        if (!"待处理".equals(order.getStatus())) {
            throw new IllegalStateException("只有待处理订单可以更改状态");
        }
        if ("已取消".equals(status)) {
            for (OrderItem item : order.getItems()) {
                Long productId = item.getProductId();
                if (productId == null) {
                    throw new IllegalStateException("订单明细缺少原商品 ID，无法恢复库存");
                }
                Integer quantity = item.getQuantity();
                if (quantity == null || quantity < 1) {
                    throw new IllegalStateException("订单明细数量无效，无法恢复库存");
                }
                if (productRepository.restoreStock(productId, quantity) == 0) {
                    throw new IllegalStateException("原商品不存在，无法恢复库存（商品 ID：" + productId + "）");
                }
            }
        }
        order.setStatus(status);
    }

    // 创建订单
    @Transactional
    public CustomerOrder createOrder(String sessionId, Long userId) {

        // 1. 获取购物车
        List<CartItem> cartItems = cartService.getCart(sessionId);

        if (cartItems == null || cartItems.isEmpty()) {
            throw new RuntimeException("购物车为空，无法下单");
        }

        // 2. 创建订单
        CustomerOrder order = new CustomerOrder();
        order.setSessionId(sessionId);
        order.setUserId(userId);
        order.setStatus("待处理");
        order.setCreatedAt(LocalDateTime.now());

        BigDecimal totalAmount = BigDecimal.ZERO;

        List<OrderItem> orderItems = new ArrayList<>();

        // 3. 遍历购物车商品
        for (CartItem cartItem : cartItems) {

            Product product = cartItem.getProduct();

            if (product == null) {
                throw new RuntimeException("商品不存在");
            }

            Integer quantity = cartItem.getQuantity();

            if (quantity == null || quantity < 1) {
                throw new RuntimeException("商品数量不正确");
            }

            if (product.getStatus() == null
                    || product.getStatus() != 1) {
                throw new RuntimeException("商品已下架");
            }

            if (productRepository.deductStock(product.getId(), quantity) == 0) {
                throw new RuntimeException("商品库存不足或已下架");
            }
            // 获取商品当前价格
            BigDecimal price = product.getPrice();

            if (price == null) {
                throw new RuntimeException("商品价格异常");
            }

            // 计算商品小计
            BigDecimal subtotal = price.multiply(
                    BigDecimal.valueOf(quantity));

            totalAmount = totalAmount.add(subtotal);

            // 4. 创建订单商品明细
            OrderItem orderItem = new OrderItem();

            orderItem.setOrder(order);
            orderItem.setProductId(product.getId());
            orderItem.setProductName(product.getName());
            orderItem.setUnitPrice(price);
            orderItem.setQuantity(quantity);

            orderItems.add(orderItem);
        }

        // 5. 设置订单金额和商品明细
        order.setTotalAmount(totalAmount);
        order.setItems(orderItems);

        // 6. 保存订单
        CustomerOrder savedOrder = orderRepository.save(order);

        // 7. 清空购物车
        cartService.clearCart(sessionId);

        return savedOrder;
    }
}
