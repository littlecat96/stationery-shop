
package com.campus.stationery.service;

import com.campus.stationery.entity.User;
import com.campus.stationery.repository.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;

    // BCrypt密码加密器
    private final BCryptPasswordEncoder passwordEncoder =
            new BCryptPasswordEncoder();

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // 用户注册
    public User register(String username, String password) {

        if (username == null || username.isBlank()
                || password == null || password.isBlank()) {
            throw new RuntimeException("用户名和密码不能为空");
        }

        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("用户名已经存在");
        }

        User user = new User();

        user.setUsername(username);

        // 密码加密后保存
        user.setPassword(passwordEncoder.encode(password));

        // 默认普通用户
        user.setRole("USER");

        user.setCreatedAt(LocalDateTime.now());

        return userRepository.save(user);
    }

    // 用户登录验证
    public Optional<User> login(String username, String password) {

        Optional<User> optionalUser =
                userRepository.findByUsername(username);

        if (optionalUser.isEmpty()) {
            return Optional.empty();
        }

        User user = optionalUser.get();

        // 验证加密密码
        if (!passwordEncoder.matches(password, user.getPassword())) {
            return Optional.empty();
        }

        return Optional.of(user);
    }
}
