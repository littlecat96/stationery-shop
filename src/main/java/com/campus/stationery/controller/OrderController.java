package com.campus.stationery.controller;

import com.campus.stationery.entity.CustomerOrder;
import com.campus.stationery.repository.CustomerOrderRepository;
import com.campus.stationery.service.OrderService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/order")
public class OrderController {

    private final OrderService orderService;
    private final CustomerOrderRepository orderRepository;

    public OrderController(OrderService orderService, CustomerOrderRepository orderRepository) {
        this.orderService = orderService;
        this.orderRepository = orderRepository;
    }

    @GetMapping
    public String listOrders(@SessionAttribute(name = "userId", required = false) Long userId,
                             Model model) {
        if (userId == null) {
            return "redirect:/login";
        }
        model.addAttribute("orders", orderRepository.findByUserIdOrderByCreatedAtDescIdDesc(userId));
        return "my-orders";
    }

    @GetMapping("/{id}")
    public String orderDetail(@PathVariable("id") Long id,
                              @SessionAttribute(name = "userId", required = false) Long userId,
                              Model model) {
        return showOrder(id, userId, false, model);
    }

    @PostMapping("/create")
    public String createOrder(@SessionAttribute(name = "userId", required = false) Long userId,
                              HttpSession session, RedirectAttributes redirectAttributes) {
        if (userId == null) {
            return "redirect:/login";
        }
        try {
            CustomerOrder order = orderService.createOrder(session.getId(), userId);
            return "redirect:/order/success/" + order.getId();
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/cart";
        }
    }

    @GetMapping("/success/{id}")
    public String orderSuccess(@PathVariable("id") Long id,
                               @SessionAttribute(name = "userId", required = false) Long userId,
                               Model model) {
        return showOrder(id, userId, true, model);
    }

    private String showOrder(Long id, Long userId, boolean submitted, Model model) {
        if (userId == null) {
            return "redirect:/login";
        }
        CustomerOrder order = orderRepository.findWithItemsByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "订单不存在"));
        model.addAttribute("order", order);
        model.addAttribute("pageTitle", submitted ? "订单提交成功" : "订单详情");
        return "order-success";
    }
}
