package com.ibm.inventorymgmt.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ibm.inventorymgmt.entity.ProductEntity;
import com.ibm.inventorymgmt.service.ProductService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/products")
@Tag(name = "products", description = "endpoints for products")
@ComponentScan(basePackages = {"com.ibm.inventorymgmt"})
public class ProductController {
    private static final Logger logger = LoggerFactory.getLogger(ProductController.class);

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ProductService productService;

    @Operation(summary = "Prodcuts list in MYSQL")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Success", content = {
            @Content(mediaType = "application/json", schema = @Schema(implementation = String.class))})})
    @GetMapping("/mysqlAll")
    public List<ProductEntity> productListMysql(){
        logger.info("All Products in Mysql");
        return productService.getAllProducts(); 
    }
    
    @Operation(summary = "The number of products")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Success", content = {
            @Content(mediaType = "application/json", schema = @Schema(implementation = String.class))})})
    @GetMapping("/{productId}/redis")
    public int redisInventory(@PathVariable String productId) {
        logger.info("Inquiry the number of product whose id is {}:", productId);
        ValueOperations<String, String> setOperations = redisTemplate.opsForValue();

        return Integer.valueOf(setOperations.get(productId));
    }

    @Operation(summary = "The number of products")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Success", content = {
            @Content(mediaType = "application/json", schema = @Schema(implementation = String.class))})})
    @GetMapping("/{productId}/mysql")
    public int mysqlInventory(@PathVariable String productId) {
        logger.info("Inquiry the number of product whose id is {}:", productId);

        return productService.getProduct(productId).getNumOfProd();
    }
    
    @Operation(summary = "Available the number of products")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Success", content = {
            @Content(mediaType = "application/json", schema = @Schema(implementation = String.class))})})
    @GetMapping("/{productId}/redis/available")
    public int inquiryAvailable(@PathVariable String productId) {
        logger.info("Inquiry the number of product whose id is {}:", productId);
        List<Object> txResults = redisTemplate.execute(new SessionCallback<List<Object>>() {
            @SuppressWarnings("unchecked")
            public List<Object> execute(RedisOperations operations) throws DataAccessException {
                int currentOrder = 0;
                if (operations.hasKey(productId + "-ongoing")) {
                    currentOrder = Integer.valueOf(operations.opsForValue().get(productId + "-ongoing").toString());
                }
                List<Object> result = new ArrayList<>();

                result.add(Integer.valueOf(operations.opsForValue().get(productId).toString()) - currentOrder);

                return result;
            }
        });

        return Integer.valueOf(txResults.get(0).toString());
    }

    @Operation(summary = "Register a new products - test")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Success", content = {
            @Content(mediaType = "application/json", schema = @Schema(implementation = String.class))})})
    @GetMapping("/registration")
    public void createNewProduct(@RequestParam String productId, @RequestParam int numOfProd) {
        logger.info("User added a new products pruductId: {} the number of product: {}", productId, numOfProd);
        ValueOperations<String, String> setOperations = redisTemplate.opsForValue();
        //update mysql
        productService.insertProduct(productId,numOfProd);
        setOperations.set(productId, Integer.toString(numOfProd));
    }

    @Operation(summary = "Delete the product by given productId - test")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Success", content = {
            @Content(mediaType = "application/json", schema = @Schema(implementation = String.class))})})
    @DeleteMapping("/deletion")
    public void deleteProduct(@RequestParam String productId) {
        logger.info("User deleted a product whose id is {}", productId);
        //update mysql
        productService.deleteProduct(productId);
        redisTemplate.delete(productId);
        redisTemplate.delete(productId + "-ongoing");
    }

}
