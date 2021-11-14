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
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.transaction.annotation.Transactional;
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
    @PostMapping("/orders")
    @Transactional
    public String order(@Valid @RequestBody ProductEntity product) {
        ValueOperations<String, String> setOperations = redisTemplate.opsForValue();
        String productId = product.getProductId();
        int currentOrder = 0;

        //Check if the product exists
        if (redisTemplate.hasKey(productId)) {
            //Check if there is the same products order is ongoing
            if (redisTemplate.hasKey(productId + "-ongoing")) {
                currentOrder = Integer.valueOf(setOperations.get(productId + "-ongoing"));
            }

            //available inventory?
            int numOfProd = Integer.valueOf(setOperations.get(productId)) - currentOrder ;
            logger.info("Current the number of product : {}", numOfProd);

            if( numOfProd > 0) {
                //current ongoing order increment
                setOperations.increment(productId + "-ongoing", product.getNumOfProd());
                logger.info("ongoin oder {}" , setOperations.get(productId + "-ongoing"));
                return "Order for " + productId + " is ongoing";
            }else {
                return productId + " is not available";
            }
        } else {
            return productId + " does not exists";
        }
    }

    @Operation(summary = "Order complete")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Success", content = {
            @Content(mediaType = "application/json", schema = @Schema(implementation = String.class))})})
    @PostMapping("/orders/completed")
    @Transactional
    public void orderCommit(@Valid @RequestBody ProductEntity product) throws Exception{
        //Actual inventory is updated
        String productId = product.getProductId();
        ValueOperations<String, String> setOperations = redisTemplate.opsForValue();
        //실제 재고 수량에 대한 감
        setOperations.decrement(productId, product.getNumOfProd());
        //현재 진행중인 order 감소
        setOperations.decrement(productId + "-ongoing", product.getNumOfProd());
        //Update mysql
        productService.updateProductForOrder(product.getProductId(),product.getNumOfProd());
    }

    @Operation(summary = "Increament inventory by given number. It means an order cancellation has been started")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Success", content = {
            @Content(mediaType = "application/json", schema = @Schema(implementation = String.class))})})
    @PostMapping("/cancel")
    @Transactional
    public String cancel(@Valid @RequestBody ProductEntity product) {
        ValueOperations<String, String> setOperations = redisTemplate.opsForValue();
        String productId = product.getProductId();

        //Check if the product exists
        if (redisTemplate.hasKey(productId)) {
            //Check if there is the same products order is ongoing
            if (redisTemplate.hasKey(productId + "-ongoing")) {
                setOperations.decrement(productId + "-ongoing", product.getNumOfProd());
            }
            return "Order for " + productId + " is cancelled";

        } else {
            return productId + " does not exists";
        }
    }

    @Operation(summary = "Increament inventory by given number. It means an order cancellation has been started")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Success", content = {
            @Content(mediaType = "application/json", schema = @Schema(implementation = String.class))})})
    @PostMapping("/cancel/completed")
    @Transactional
    public void cancelCommit(@Valid @RequestBody ProductEntity product) {
        //Actual inventory is updated
        String productId = product.getProductId();
        ValueOperations<String, String> setOperations = redisTemplate.opsForValue();
        //실제 재고 수량에 대한 감
        setOperations.increment(productId, product.getNumOfProd());
        //Update mysql
        productService.updateProductForCancel(product.getProductId(),product.getNumOfProd());
    }

    @Operation(summary = "The number of products")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Success", content = {
            @Content(mediaType = "application/json", schema = @Schema(implementation = String.class))})})
    @GetMapping("/inquiry")
    public int inquiry(@RequestParam String productId) {
        logger.info("Inquiry the number of product whose id is {}:", productId);
        ValueOperations<String, String> setOperations = redisTemplate.opsForValue();

        return Integer.valueOf(setOperations.get(productId));
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
        logger.info("User added a new products pruductId: {} the number of product: {}", productId, numOfProd);
        ValueOperations<String, String> setOperations = redisTemplate.opsForValue();
        //update mysql
        productService.insertProduct(productId,numOfProd);
        setOperations.set(productId, Integer.toString(numOfProd));
    }

    @Operation(summary = "Delete the product by given productId - test")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Success", content = {
            @Content(mediaType = "application/json", schema = @Schema(implementation = String.class))})})
    @DeleteMapping("/products/deletion")
    public void deleteProduct(@RequestParam String productId) {
        logger.info("User deleted a product whose id is {}", productId);
        //update mysql
        productService.deleteProduct(productId);
        redisTemplate.delete(productId);
    }

}

