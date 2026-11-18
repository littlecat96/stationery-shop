
package com.campus.stationery.controller;

import com.campus.stationery.entity.User;
import com.campus.stationery.service.UserService;

import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class LoginController {

    private final UserService userService;

    public LoginController(UserService userService) {
        this.userService = userService;
    }

    // 显示登录页面
    @GetMapping("/login")
    public String login() {
        return "login";
    }

    // 处理登录
    @PostMapping("/login")
    public String doLogin(
            @RequestParam String username,
            @RequestParam String password,
            HttpSession session,
            Model model) {

        try {
            User user = userService.login(username, password)
                    .orElse(null);

            if (user == null) {
                model.addAttribute("error", "用户名或密码错误");
                return "login";
            }

            // 保存用户身份
            session.setAttribute("userId", user.getId());
            session.setAttribute("username", user.getUsername());
            session.setAttribute("role", user.getRole());

            // 根据角色跳转
            if ("ADMIN".equals(user.getRole())) {
                return "redirect:/admin/products";
            }

            return "redirect:/shop";

        } catch (Exception e) {
            model.addAttribute("error", "登录失败，请稍后重试");
            return "login";
        }
    }

    // 退出登录
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    // 显示注册页面
    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    // 处理用户注册
    @PostMapping("/register")
    public String doRegister(
            @RequestParam String username,
            @RequestParam String password,
            Model model) {

        try {
            userService.register(username, password);

            return "redirect:/login";

        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            return "register";
        }
    }

}
