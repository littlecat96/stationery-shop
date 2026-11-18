
package com.campus.stationery.service;

import com.campus.stationery.entity.Product;
import com.campus.stationery.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    // 查询所有商品
    public List<Product> findAll() {
        return productRepository.findAll();
    }

    // 查询所有上架商品
    public List<Product> findAvailableProducts(String keyword) {
        if (keyword.isEmpty()) {
            return productRepository.findByStatus(1);
        }
        return productRepository.findByStatusAndNameContainingIgnoreCase(1, keyword);
    }

    // 根据商品名称搜索
    public List<Product> search(String name) {
        return productRepository.findByNameContainingIgnoreCase(name);
    }

    // 根据商品编号查询
    public Product findById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("商品不存在"));
    }

    // 添加或修改商品
    public Product save(Product product) {
        if (product.getName() == null || product.getName().isBlank()) {
            throw new IllegalArgumentException("商品名称不能为空");
        }

        if (product.getPrice() == null
                || product.getPrice().signum() < 0) {
            throw new IllegalArgumentException("商品价格不能小于0");
        }

        if (product.getStock() == null || product.getStock() < 0) {
            throw new IllegalArgumentException("商品库存不能小于0");
        }

        return productRepository.save(product);
    }

    // 删除商品
    public void delete(Long id) {
        if (!productRepository.existsById(id)) {
            throw new RuntimeException("商品不存在");
        }

        productRepository.deleteById(id);
    }
}
