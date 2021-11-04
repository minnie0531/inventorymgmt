package com.ibm.inventorymgmt.controller;

import com.ibm.inventorymgmt.entity.ProductEntity;
import com.ibm.inventorymgmt.services.ProductService;

import java.util.List;
import java.util.Map;
import java.util.Set;

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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/inventory")
@Tag(name = "inventory", description = "endpoints for inventory")
@ComponentScan(basePackages = {"com.ibm.inventorymgmt"})
public class InventoryController {

    private static final Logger logger = LoggerFactory.getLogger(InventoryController.class);

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ProductService productService;

    @GetMapping("/")
    @Hidden
    public String index() {
        return "Greetings from Spring Boot!";
    }

    @Operation(summary = "Decreament inventory by given number. It means an order has been started")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Success", content = {
            @Content(mediaType = "application/json", schema = @Schema(implementation = String.class))})})
    @GetMapping("/order")
    public String order(@RequestParam String productId, @RequestParam int number) {
        HashOperations<String, Object,  Object> hashOperations = redisTemplate.opsForHash();
        int numOfProd = this.inquiry(productId);

        logger.info("Current number of product : %", numOfProd);

        if( numOfProd > 0) {
            int inventory = numOfProd - number;
            hashOperations.put(productId, "number", Integer.toString(inventory));
            //update mysql
            productService.updateProduct(productId,inventory);
            return productId + " is ordered";
        }else {
            return productId + " is not available";
        }
    }
    
    @Operation(summary = "Increament inventory by given number. It means an order cancellation has been started")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Success", content = {
            @Content(mediaType = "application/json", schema = @Schema(implementation = String.class))})})
    @GetMapping("/cancel")
    public String cancel(@RequestParam String productId, @RequestParam int number) {
        HashOperations<String, Object,  Object> hashOperations = redisTemplate.opsForHash();
        
        int numOfProd = this.inquiry(productId);
        int inventory = numOfProd + number;
        //update mysql
        productService.updateProduct(productId,inventory);
        hashOperations.put(productId, "number", Integer.toString(inventory));
        
        return productId + " is cancelled";        
    }
    

    @Operation(summary = "The number of products")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Success", content = {
            @Content(mediaType = "application/json", schema = @Schema(implementation = String.class))})})
    @GetMapping("/inquiry")
    public int inquiry(@RequestParam String productId) {
        HashOperations<String, Object, Object> hashOperations = redisTemplate.opsForHash();
        Map<Object, Object> entries = hashOperations.entries(productId);
        return Integer.valueOf((String)entries.get("number"));
    }

    @Operation(summary = "Prodcuts list")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Success", content = {
            @Content(mediaType = "application/json", schema = @Schema(implementation = String.class))})})
    @GetMapping("/products")
    public Set<String> productList(){
        return redisTemplate.keys("*");
    }

    @Operation(summary = "Prodcuts list in MYSQL")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Success", content = {
            @Content(mediaType = "application/json", schema = @Schema(implementation = String.class))})})
    @GetMapping("/products/mysql")
    public List<ProductEntity> productListMysql(){
        return productService.getAllProducts();
    }

    @Operation(summary = "Register a new products - test")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Success", content = {
            @Content(mediaType = "application/json", schema = @Schema(implementation = String.class))})})
    @GetMapping("/registration")
    public void CreateNewProduct(@RequestParam String productId, @RequestParam int numOfProd) {
        HashOperations<String, Object,  Object> hashOperations = redisTemplate.opsForHash();
        //update mysql
        productService.insertProduct(productId,numOfProd);
        hashOperations.put(productId, "number", Integer.toString(numOfProd));
    }
}

