
package com.campus.stationery.repository;

import com.campus.stationery.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    @Modifying
    @Query("""
            update Product p set p.stock = p.stock - :quantity
            where p.id = :id and p.status = 1 and p.stock >= :quantity
            """)
    int deductStock(@Param("id") Long id, @Param("quantity") int quantity);

    @Modifying
    @Query("update Product p set p.stock = p.stock + :quantity where p.id = :id")
    int restoreStock(@Param("id") Long id, @Param("quantity") int quantity);

    // 根据商品名称搜索
    List<Product> findByNameContainingIgnoreCase(String name);

    // 根据商品分类查询
    List<Product> findByCategory(String category);

    // 查询上架商品
    List<Product> findByStatus(Integer status);

    List<Product> findByStatusAndNameContainingIgnoreCase(Integer status, String name);

}
