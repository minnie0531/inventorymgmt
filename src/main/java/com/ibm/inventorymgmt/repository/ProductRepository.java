package com.ibm.inventorymgmt.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.ibm.inventorymgmt.entity.ProductEntity;

@Repository
public interface ProductRepository extends JpaRepository<ProductEntity, Long> {
    
    public ProductEntity findByProductId(String productId);
    
    @Transactional
    public void deleteByProductId(String productId);

}


