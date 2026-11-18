
package com.campus.stationery.repository;

import com.campus.stationery.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // 根据用户名查询用户
    Optional<User> findByUsername(String username);

    // 判断用户名是否已经存在
    boolean existsByUsername(String username);
}
