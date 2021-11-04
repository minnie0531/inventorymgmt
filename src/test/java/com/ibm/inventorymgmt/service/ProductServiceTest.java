package com.ibm.inventorymgmt.controller.services;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import com.ibm.inventorymgmt.services.ProductService;

import lombok.extern.slf4j.Slf4j;

@SpringBootTest
public class ProductServiceTest {
    
    @Autowired
    private ProductService productService;
    
    @Test
    public void ProductServiceTest() throws Exception {
        
        productService.insertProduct("#0001",100000);
        
        assertEquals(100000, productService.getProduct("#0001").getNumOfProd());
        
        productService.updateProduct("#0001", 200000);
        
        assertEquals(200000, productService.getProduct("#0001").getNumOfProd());
        
        productService.deleteProduct("#0001");

    }
    

}
