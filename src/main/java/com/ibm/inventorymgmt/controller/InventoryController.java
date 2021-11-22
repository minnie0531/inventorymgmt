package com.ibm.inventorymgmt.controller;

import com.ibm.inventorymgmt.entity.ProductEntity;
import com.ibm.inventorymgmt.service.ProductService;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
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
    public String order(@Valid @RequestBody ProductEntity product) {
        ValueOperations<String, String> setOperations = redisTemplate.opsForValue();
        String productId = product.getProductId();
        String ongoingId = productId + "-ongoing";

        // Start transaction
        // 같은 세션에서 동작하기 위해 SessionCallBack사용
        // multi - exec사용 구문에서 get operation은 null을 반환
        // redisTemplate에서 read-only 와 write command 는 구분되어 사용됨
        // Refer to
        // Read-only commands, such as KEYS, are piped to a fresh (non-thread-bound) RedisConnection to allow reads.
        // Write commands are queued by RedisTemplate and applied upon commit.
        if (redisTemplate.hasKey(productId)) {
            List<Object> txResults = redisTemplate.execute(new SessionCallback<List<Object>>() {
                @Override
                public List<Object> execute(RedisOperations operations) throws DataAccessException {
                    int currentOrder = 0;

                    if (operations.hasKey(ongoingId)){
                        currentOrder = Integer.valueOf(operations.opsForValue().get(ongoingId).toString());
                    }

                     //available inventory?
                    int numOfProd = Integer.valueOf(operations.opsForValue().get(productId).toString()) - currentOrder ;

                    if( numOfProd > 0) {
                        operations.multi();
                        //current ongoing order increment
                        operations.opsForValue().increment(ongoingId, product.getNumOfProd());

                        return operations.exec();
                    }else {
                        return Collections.emptyList();
                    }
                }
            });

            logger.info("result {}", txResults);

            if (txResults != Collections.EMPTY_LIST) {
                return "Order for " + productId + " is ongoing";
            } else {
                return "Order for " + productId + " is not available";
            }
        }else {
            return productId + " does not exists";
        }
    }

    @Operation(summary = "Order complete")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Success", content = {
            @Content(mediaType = "application/json", schema = @Schema(implementation = String.class))})})
    @PostMapping("/orders/completed")
    public void orderCommit(@Valid @RequestBody ProductEntity product) throws Exception{
        //Actual inventory is updated
        List<Object> txResults = redisTemplate.execute(new SessionCallback<List<Object>>() {
            @SuppressWarnings("unchecked")
            public List<Object> execute(RedisOperations operations) throws DataAccessException {
                String productId = product.getProductId();
                operations.multi();
                //실제 재고 수량에 대한 감소
                operations.opsForValue().decrement(productId, product.getNumOfProd());
                //현재 진행중인 order 감소
                operations.opsForValue().decrement(productId + "-ongoing", product.getNumOfProd());
                return operations.exec();
            }
         });
        //Update mysql
        productService.updateProductForOrder(product.getProductId(),product.getNumOfProd());
    }

    @Operation(summary = "Order canceled")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Success", content = {
            @Content(mediaType = "application/json", schema = @Schema(implementation = String.class))})})
    @PostMapping("/orders/canceled")
    public String orderCancel(@Valid @RequestBody ProductEntity product) throws Exception{
        String productId = product.getProductId();
        String ongoingId = productId + "-ongoing";

        if (redisTemplate.hasKey(productId) && 
                Integer.valueOf(redisTemplate.opsForValue().get(ongoingId).toString()) >= 1) {
        //Actual inventory is updated
            List<Object> txResults = redisTemplate.execute(new SessionCallback<List<Object>>() {
                @SuppressWarnings("unchecked")
                public List<Object> execute(RedisOperations operations) throws DataAccessException {

                    operations.multi();
                    //현재 진행중인 order 감소
                    operations.opsForValue().decrement(productId + "-ongoing", product.getNumOfProd());
                    return operations.exec();
                }
             });

            return "ongoing order for " + productId + " is canceled";
        }else {
            return "There is no ongoing order for " + productId;
        }
    }

    @Operation(summary = "Increament inventory by given number. It means an order cancellation has been started")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Success", content = {
            @Content(mediaType = "application/json", schema = @Schema(implementation = String.class))})})
    @PostMapping("/cancel")
    public String cancel(@Valid @RequestBody ProductEntity product) {
        String productId = product.getProductId();

        // 주문 완료했던 사용자만 사용가능 - 앞에서 로직으로 처리
        // status만 확인 진행중인 주문에 대해 영향을 주지 않도록 함.
        // cancel 이 완료된 시점에 실제 재고만 감소하도록
        // Check if the product exists
        if (redisTemplate.hasKey(productId)) {
            return "Order for " + productId + " is cancelled";
        }else {
            return productId + " does not exists";
        }
    }

    @Operation(summary = "Increament inventory by given number. It means an order cancellation has been started")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Success", content = {
            @Content(mediaType = "application/json", schema = @Schema(implementation = String.class))})})
    @PostMapping("/cancel/completed")
    public void cancelCommit(@Valid @RequestBody ProductEntity product) {
        //Actual inventory is updated
        List<Object> txResults = redisTemplate.execute(new SessionCallback<List<Object>>() {
            @SuppressWarnings("unchecked")
            public List<Object> execute(RedisOperations operations) throws DataAccessException {
                String productId = product.getProductId();
                operations.multi();
                //실제 재고 수량에 대한 증가
                operations.opsForValue().increment(productId, product.getNumOfProd());
                return operations.exec();
            }
         });
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
        redisTemplate.delete(productId + "-ongoing");
    }

}

