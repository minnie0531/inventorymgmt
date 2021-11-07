package com.ibm.inventorymgmt.controller;

import com.ibm.inventorymgmt.entity.ProductEntity;
import com.ibm.inventorymgmt.service.ProductService;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
    @PostMapping("/order")
    public String order(@Valid @RequestBody ProductEntity product) {
        HashOperations<String, Object,  Object> hashOperations = redisTemplate.opsForHash();
        String productId = product.getProductId();
        int numOfProd = this.inquiry(productId);

        logger.info("Current the number of product : %", numOfProd);

        if( numOfProd > 0) {
            int inventory = numOfProd - product.getNumOfProd();
            hashOperations.put(productId, "number", Integer.toString(inventory));
            //update mysql
            productService.updateProduct(productId,inventory);
            logger.info("Current the number of inventory : %", inventory);
            
            return productId + " is ordered";
        }else {
            return productId + " is not available";
        }
    }
    
    @Operation(summary = "Increament inventory by given number. It means an order cancellation has been started")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Success", content = {
            @Content(mediaType = "application/json", schema = @Schema(implementation = String.class))})})
    @PostMapping("/cancel")
    public String cancel(@Valid @RequestBody ProductEntity product) {
        HashOperations<String, Object,  Object> hashOperations = redisTemplate.opsForHash();
        String productId = product.getProductId();
        int numOfProd = this.inquiry(productId);
        logger.info("Current the number of product : %", numOfProd);
        int inventory = numOfProd + product.getNumOfProd();
        //update mysql
        productService.updateProduct(productId,inventory);
        hashOperations.put(productId, "number", Integer.toString(inventory));
        logger.info("Current the number of inventory : %", inventory);
        
        return productId + " is cancelled";        
    }
    

    @Operation(summary = "The number of products")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Success", content = {
            @Content(mediaType = "application/json", schema = @Schema(implementation = String.class))})})
    @GetMapping("/inquiry")
    public int inquiry(@RequestParam String productId) {
        logger.info("Inquiry the number of product whose id is %:", productId);
        HashOperations<String, Object, Object> hashOperations = redisTemplate.opsForHash();
        Map<Object, Object> entries = hashOperations.entries(productId);
        return Integer.valueOf((String)entries.get("number"));
    }

    @Operation(summary = "Prodcuts list")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Success", content = {
            @Content(mediaType = "application/json", schema = @Schema(implementation = String.class))})})
    @GetMapping("/products")
    public Set<String> productList(){
        logger.info("All product Id in Redis");
        return redisTemplate.keys("*");
    }

    @Operation(summary = "Prodcuts list in MYSQL")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Success", content = {
            @Content(mediaType = "application/json", schema = @Schema(implementation = String.class))})})
    @GetMapping("/products/mysql")
    public List<ProductEntity> productListMysql(){
        logger.info("All Products in Mysql");
        return productService.getAllProducts();
    }

    @Operation(summary = "Register a new products - test")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Success", content = {
            @Content(mediaType = "application/json", schema = @Schema(implementation = String.class))})})
    @GetMapping("/products/registration")
    public void createNewProduct(@RequestParam String productId, @RequestParam int numOfProd) {
        logger.info("User added a new products pruductId: % the number of product: %", productId, numOfProd);
        HashOperations<String, Object,  Object> hashOperations = redisTemplate.opsForHash();
        //update mysql
        productService.insertProduct(productId,numOfProd);
        hashOperations.put(productId, "number", Integer.toString(numOfProd));
    }

    @Operation(summary = "Delete the product by given productId - test")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Success", content = {
            @Content(mediaType = "application/json", schema = @Schema(implementation = String.class))})})
    @DeleteMapping("/products/deletion")
    public void deleteProduct(@RequestParam String productId) {
        logger.info("User deleted a product whose id is %d", productId);
        HashOperations<String, Object,  Object> hashOperations = redisTemplate.opsForHash();
        //update mysql
        productService.deleteProduct(productId);
        hashOperations.delete(productId, "number");
    }

}

