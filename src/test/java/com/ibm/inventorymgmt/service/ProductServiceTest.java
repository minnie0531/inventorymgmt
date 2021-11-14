package com.ibm.inventorymgmt.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import com.ibm.inventorymgmt.service.ProductService;

import lombok.extern.slf4j.Slf4j;

@SpringBootTest
public class ProductServiceTest {
    
    @Autowired
    private ProductService productService;
    
    @Test
    public void ProductServiceTest() throws Exception {
        
        productService.insertProduct("#0001",100000);
        
        assertEquals(100000, productService.getProduct("#0001").getNumOfProd());
        
        productService.updateProductForOrder("#0001", 2);
        
        assertEquals(99998, productService.getProduct("#0001").getNumOfProd());
        
        productService.deleteProduct("#0001");
    }
}
