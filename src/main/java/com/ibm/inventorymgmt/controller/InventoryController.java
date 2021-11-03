package com.ibm.inventorymgmt.controller;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/inventory")
@Tag(name = "inventory", description = "endpoints for inventory")
@ComponentScan(basePackages = {"com.ibm.inventorymgmt"})
public class InventoryController {
    
    private static final Logger logger = LoggerFactory.getLogger(InventoryController.class);

    @Autowired
    private StringRedisTemplate redisTemplate;
    
    
    @GetMapping("/")
    @Hidden
    public String index() {
        return "Greetings from Spring Boot!";
    }
    
   //변경 : 재고 - 현재 주
    @GetMapping("/order")
    public String order(@RequestParam String productNo, @RequestParam int number) {
        HashOperations<String, Object,  Object> hashOperations = redisTemplate.opsForHash();
        int numOfprod = this.inquiry(productNo);
        
        logger.info("Current number of product : %", numOfprod);
        
        if( numOfprod > 0) {
            hashOperations.put(productNo, "number", Integer.toString(numOfprod-number));
            return productNo + " is ordered";
        }else {
            return productNo + " is not available";
        }
    }
    
    //변경: 재고 + 현재취
    @GetMapping("/cancel")
    public String cancel(@RequestParam String productNo, @RequestParam int number) {
        HashOperations<String, Object,  Object> hashOperations = redisTemplate.opsForHash();
        
        int numOfprod = this.inquiry(productNo);
        hashOperations.put(productNo, "number", Integer.toString(numOfprod + number));
        
        return productNo + " is cancelled";        
    }
    

    @GetMapping("/inquiry")
    public int inquiry(@RequestParam String productNo) {
        HashOperations<String, Object, Object> hashOperations = redisTemplate.opsForHash();
        Map<Object, Object> entries = hashOperations.entries(productNo);
        return Integer.valueOf((String)entries.get("number"));
    }

}

