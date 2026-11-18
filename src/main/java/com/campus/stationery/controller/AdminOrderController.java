package com.campus.stationery.controller;

import com.campus.stationery.entity.CustomerOrder;
import com.campus.stationery.repository.CustomerOrderRepository;
import com.campus.stationery.service.OrderService;
import jakarta.servlet.http.HttpSession;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Controller
@RequestMapping("/admin/orders")
public class AdminOrderController {

    private static final List<String> ORDER_STATUSES = List.of("待处理", "已完成", "已取消");

    private static final String STATUS_TOKEN = "adminOrderStatusToken";

    private final CustomerOrderRepository orderRepository;

    private final OrderService orderService;

    public AdminOrderController(CustomerOrderRepository orderRepository, OrderService orderService) {
        this.orderRepository = orderRepository;
        this.orderService = orderService;
    }

    @GetMapping
    public String listOrders(@RequestParam(name = "orderId", defaultValue = "") String orderId,
                             Model model) {
        String keyword = orderId.strip();
        model.addAttribute("orderId", keyword);
        List<CustomerOrder> orders = List.of();
        if (keyword.isEmpty()) {
            orders = orderRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        } else {
            try {
                long id = Long.parseLong(keyword);
                if (id <= 0 || !keyword.matches("[0-9]+")) {
                    throw new NumberFormatException();
                }
                orders = orderRepository.findById(id).stream().toList();
            } catch (NumberFormatException e) {
                model.addAttribute("searchError", "请输入有效的订单编号");
            }
        }
        model.addAttribute("orders", orders);
        return "admin-orders";
    }

    @GetMapping("/{id}")
    public String orderDetail(@PathVariable("id") Long id, HttpSession session, Model model) {
        CustomerOrder order = orderRepository.findWithItemsById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "订单不存在"));
        model.addAttribute("order", order);
        model.addAttribute("statuses", ORDER_STATUSES);
        synchronized (session) {
            if (session.getAttribute(STATUS_TOKEN) == null) {
                session.setAttribute(STATUS_TOKEN, UUID.randomUUID().toString());
            }
            model.addAttribute("statusToken", session.getAttribute(STATUS_TOKEN));
        }
        return "admin-order-detail";
    }

    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable("id") Long id,
                               @RequestParam("status") String status,
                               @RequestParam(name = "statusToken", defaultValue = "") String statusToken,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        if (!statusToken.equals(session.getAttribute(STATUS_TOKEN))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "表单已失效，请刷新后重试");
        }
        if (!ORDER_STATUSES.contains(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "订单状态无效");
        }
        try {
            orderService.updateStatus(id, status);
            redirectAttributes.addFlashAttribute("message", "订单状态已保存");
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "订单不存在");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("message", e.getMessage());
        }
        return "redirect:/admin/orders/" + id;
    }
}
