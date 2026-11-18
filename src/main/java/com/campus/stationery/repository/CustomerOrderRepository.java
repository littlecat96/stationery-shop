
package com.campus.stationery.repository;

import com.campus.stationery.entity.CustomerOrder;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerOrderRepository
        extends JpaRepository<CustomerOrder, Long> {

    @EntityGraph(attributePaths = "items")
    Optional<CustomerOrder> findWithItemsById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from CustomerOrder o where o.id = :id")
    Optional<CustomerOrder> findByIdForUpdate(@Param("id") Long id);

    List<CustomerOrder> findByUserIdOrderByCreatedAtDescIdDesc(Long userId);

    @EntityGraph(attributePaths = "items")
    Optional<CustomerOrder> findWithItemsByIdAndUserId(Long id, Long userId);

}
