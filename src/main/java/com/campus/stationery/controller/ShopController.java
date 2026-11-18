
package com.campus.stationery.controller;

import com.campus.stationery.entity.Product;
import com.campus.stationery.service.ProductService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/shop")
public class ShopController {

    private final ProductService productService;

    // 构造方法
    public ShopController(ProductService productService) {
        this.productService = productService;
    }

    // 用户商城首页
    @GetMapping
    public String shop(@RequestParam(name = "keyword", defaultValue = "") String keyword, Model model) {
        String searchTerm = keyword.strip();
        List<Product> products = productService.findAvailableProducts(searchTerm);
        model.addAttribute("products", products);
        model.addAttribute("keyword", searchTerm);

        // 返回商城首页
        return "shop";
    }
}
