
package com.campus.stationery.controller;

import com.campus.stationery.entity.Product;
import com.campus.stationery.service.ProductService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/admin/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    // 管理员商品列表
    @GetMapping
    public String list(@RequestParam(required = false) String keyword,
                       Model model) {

        List<Product> products;

        if (keyword == null || keyword.isBlank()) {
            products = productService.findAll();
        } else {
            products = productService.search(keyword);
        }

        model.addAttribute("products", products);
        model.addAttribute("keyword", keyword);

        return "products";
    }

    // 打开添加商品页面
    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("product", new Product());
        return "product-form";
    }

    // 保存商品
    @PostMapping("/save")
    public String save(@ModelAttribute Product product) {

        if (product.getId() == null) {
            product.setStatus(1);
        }

        productService.save(product);

        return "redirect:/admin/products";
    }

    // 修改商品
    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Model model) {

        Product product = productService.findById(id);

        model.addAttribute("product", product);

        return "product-form";
    }

    // 删除商品
    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id) {

        productService.delete(id);

        return "redirect:/admin/products";
    }
}
