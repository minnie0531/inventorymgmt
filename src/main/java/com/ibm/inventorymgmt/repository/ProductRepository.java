package com.ibm.inventorymgmt.repository;


import javax.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.ibm.inventorymgmt.entity.ProductEntity;

@Repository
public interface ProductRepository extends JpaRepository<ProductEntity, Long> {
    @Transactional
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    public ProductEntity findByProductId(String productId);
    
    @Transactional
    public void deleteByProductId(String productId);

}


