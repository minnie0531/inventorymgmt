package com.ibm.inventorymgmt.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ibm.inventorymgmt.entity.ProductEntity;
import com.ibm.inventorymgmt.repository.ProductRepository;

@Service
public class ProductServiceImpl implements ProductService {
    
    @Autowired
    private ProductRepository productRepository;
    

    @Override
    public List<ProductEntity> getAllProducts() {
        return productRepository.findAll();
    }

    @Override
    public ProductEntity insertProduct(String productId, int numOfProd) {
        ProductEntity product = new ProductEntity();
        product.setNumOfProd(numOfProd);
        product.setProductId(productId);
        return productRepository.save(product);
    }

    @Override
    public ProductEntity getProduct(String productId) {
        return productRepository.findByProductId(productId);
    }

    @Override
    public ProductEntity updateProduct(String productId, int numOfProd) {
        ProductEntity prod = productRepository.findByProductId(productId);
        prod.setNumOfProd(numOfProd);
        return productRepository.save(prod);
    }

    @Override
    public void deleteProduct(String productId) {
        productRepository.deleteByProductId(productId);      
    }

}

