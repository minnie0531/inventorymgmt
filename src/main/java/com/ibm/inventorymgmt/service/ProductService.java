package com.ibm.inventorymgmt.service;

import java.util.List;

import org.springframework.stereotype.Component;

import com.ibm.inventorymgmt.entity.ProductEntity;

@Component
public interface ProductService {
    public List<ProductEntity> getAllProducts();
    
    public ProductEntity insertProduct(String productId, int numOfProd);
    
    public ProductEntity getProduct(String productId);
    
    public ProductEntity updateProduct(String productId, int numOfProd);

    public void deleteProduct(String productId);

}
