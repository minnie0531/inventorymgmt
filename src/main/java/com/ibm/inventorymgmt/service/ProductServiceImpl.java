package com.ibm.inventorymgmt.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ibm.inventorymgmt.entity.ProductEntity;
import com.ibm.inventorymgmt.repository.ProductRepository;

@Service
@Transactional
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
    public ProductEntity updateProductForOrder(String productId, int numOfProd) {
        ProductEntity prod = productRepository.findByProductId(productId);
        int inventory = prod.getNumOfProd() - numOfProd;
        prod.setNumOfProd(inventory);
        return productRepository.save(prod);
    }

    @Override
    public void deleteProduct(String productId) {
        productRepository.deleteByProductId(productId);      
    }

    @Override
    public ProductEntity updateProductForCancel(String productId, int numOfProd) {
        ProductEntity prod = productRepository.findByProductId(productId);
        int inventory = prod.getNumOfProd() + numOfProd;
        prod.setNumOfProd(inventory);
        return productRepository.save(prod);
    }

}

