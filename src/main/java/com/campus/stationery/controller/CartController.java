
package com.campus.stationery.controller;

import com.campus.stationery.entity.CartItem;
import com.campus.stationery.service.CartService;

import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    // 查看购物车
    @GetMapping
    public String viewCart(HttpSession session, Model model) {

        List<CartItem> items =
                cartService.getCart(session.getId());

        BigDecimal total = items.stream()
                .map(item -> item.getProduct().getPrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("items", items);
        model.addAttribute("total", total);

        return "cart";
    }

    // 加入购物车
    @PostMapping("/add/{productId}")
    public String add(@PathVariable Long productId,
                      HttpSession session) {

        cartService.addToCart(session.getId(), productId);

        return "redirect:/cart";
    }

    // 修改商品数量
    @PostMapping("/update/{productId}")
    public String update(@PathVariable Long productId,
                         @RequestParam Integer quantity,
                         HttpSession session) {

        cartService.updateQuantity(
                session.getId(), productId, quantity);

        return "redirect:/cart";
    }

    // 删除购物车商品
    @PostMapping("/remove/{productId}")
    public String remove(@PathVariable Long productId,
                         HttpSession session) {

        cartService.removeFromCart(
                session.getId(), productId);

        return "redirect:/cart";
    }

    // 清空购物车
    @PostMapping("/clear")
    public String clear(HttpSession session) {

        cartService.clearCart(session.getId());

        return "redirect:/cart";
    }
}
