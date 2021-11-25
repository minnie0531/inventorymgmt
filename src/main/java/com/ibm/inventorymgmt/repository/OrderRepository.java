package com.ibm.inventorymgmt.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.ibm.inventorymgmt.entity.OrderEntity;

@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, Long> {
    
    public OrderEntity findByOrderNumber(String orderNumber);
    
    @Transactional
    public void deleteByOrderNumber(String orderNumber);
}
