package com.ibm.inventorymgmt.controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.ibm.inventorymgmt.entity.OrderEntity;
import com.ibm.inventorymgmt.entity.ProductEntity;
import com.ibm.inventorymgmt.service.OrderService;
import com.ibm.inventorymgmt.service.ProductService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/orders")
@Tag(name = "orders", description = "endpoints for orders")
@ComponentScan(basePackages = {"com.ibm.inventorymgmt"})
public class OrderController {

    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ProductService productService;
    
    @Autowired
    private OrderService orderService;
    
    @Operation(summary = "Decreament inventory by given number. It means an order has been started")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Success", content = {
            @Content(mediaType = "application/json", schema = @Schema(implementation = String.class))})})
    @PostMapping("")   
    public String order(@Valid @RequestBody ProductEntity product) throws JsonProcessingException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String productId = product.getProductId();
        String ongoingId = productId + "-ongoing";
        // create order number
        String orderNumber = UUID.randomUUID().toString();
        //Order time
        String localDateTime = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);
        //User
        String user = auth.getName();
        
        OrderEntity order = new OrderEntity(orderNumber, user, productId, product.getNumOfProd(), localDateTime, "ongoing");
        logger.info("order : {} " ,order.toString());

        // Start transaction
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

                    if( numOfProd > 1) {
                        operations.multi();
                        //current ongoing order increment
                        // 재고
                        operations.opsForValue().increment(ongoingId, product.getNumOfProd());
                        // 주문
                        operations.opsForHash().putAll(orderNumber, convertToHash(order));
                        // history
                        operations.opsForList().leftPush(user + "-history" , orderNumber);
                        // restore list
                        operations.opsForList().leftPush("orderQueue", orderNumber);

                        return operations.exec();
                    }else {
                        return Collections.emptyList();
                    }
                }
            });

            logger.info("result {}", txResults);

            if (txResults != Collections.EMPTY_LIST) {
                return orderNumber;
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
    @PostMapping("/{orderNumber}/completed")
    public void orderCommit(@PathVariable String orderNumber) throws Exception{
        //Actual inventory is updated
        OrderEntity order = orderInquiry(orderNumber);
        order.setStatus("ordered");
        String productId = order.getProductId();
        int numOfProd = order.getNumOfProd();

        List<Object> txResults = redisTemplate.execute(new SessionCallback<List<Object>>() {
            @SuppressWarnings("unchecked")
            public List<Object> execute(RedisOperations operations) throws DataAccessException {
                operations.multi();
                //실제 재고 수량에 대한 감소
                operations.opsForValue().decrement(productId, numOfProd);
                //현재 진행중인 order 감소
                operations.opsForValue().decrement(productId + "-ongoing", numOfProd);
                // order status change
                operations.opsForHash().put(orderNumber, "status", "ordered");
                // remove order number from restore list
                operations.opsForList().remove("orderQueue", 1, orderNumber);
                // create order and product in mysql
                productService.updateProductForOrder(productId,numOfProd);
                orderService.createOrder(order);
                return operations.exec();
            }
         });
    }

    @Operation(summary = "Orders find by order numbers in mysql")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Success", content = {
            @Content(mediaType = "application/json", schema = @Schema(implementation = OrderEntity.class))})})
    @GetMapping("/{orderNumber}/mysql")
    public OrderEntity ordersInMysql(@PathVariable String orderNumber){
        return orderService.getOrderByOrderNumber(orderNumber);
    }

    @Operation(summary = "Order contents")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Success", content = {
            @Content(mediaType = "application/json", schema = @Schema(implementation = OrderEntity.class))})})
    @GetMapping("/{orderNumber}/redis")
    public OrderEntity orderInquiry(@PathVariable String orderNumber) throws Exception{
        //Actual inventory is updated
        HashOperations<String, Object, Object> setOperations = redisTemplate.opsForHash();
        Collection<Object> orderKeys = new ArrayList<>();
        orderKeys.add("orderNumber");
        orderKeys.add("userName");
        orderKeys.add("productId");
        orderKeys.add("numOfProd");
        orderKeys.add("date");
        orderKeys.add("status");

        List<Object> result = setOperations.multiGet(orderNumber, orderKeys);
        logger.info("result {}", result);

        OrderEntity order = new OrderEntity(result.get(0).toString(), result.get(1).toString(), result.get(2).toString(), Integer.parseInt(result.get(3).toString()), 
                result.get(4).toString(), result.get(5).toString());
        return order;
    }

    @Operation(summary = "Order processing queue to sync-up between mysql and redis")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Success", content = {
            @Content(mediaType = "application/json", schema = @Schema(implementation = String.class))})})
    @GetMapping("/process")
    public List<String> orderProcess() throws Exception{
        //Actual inventory is updated
        Long size = redisTemplate.opsForList().size("orderQueue");
        return redisTemplate.opsForList().range("orderQueue", 0, size);
    }

    @Operation(summary = "Order cancelled")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Success", content = {
            @Content(mediaType = "application/json", schema = @Schema(implementation = String.class))})})
    @PostMapping("/{orderNumber}/cancelled")
    public String orderCancel(@PathVariable String orderNumber) throws Exception{
        ProductEntity product = (ProductEntity) redisTemplate.opsForHash().get(orderNumber, "products");
        String user = redisTemplate.opsForHash().get(orderNumber, "userName").toString();
        String productId = product.getProductId();
        String ongoingId = product.getProductId() + "-ongoing";

        if (redisTemplate.hasKey(orderNumber) && 
                Integer.valueOf(redisTemplate.opsForValue().get(ongoingId).toString()) >= 1) {
            List<Object> txResults = redisTemplate.execute(new SessionCallback<List<Object>>() {
                @SuppressWarnings("unchecked")
                public List<Object> execute(RedisOperations operations) throws DataAccessException {
                    operations.multi();
                    // history 삭
                    operations.opsForList().remove(user + "-history", 1, orderNumber);
                    // Delete order
                    operations.delete(orderNumber);
                    // delete restore queue
                    operations.opsForList().remove("orderQueue", 1, orderNumber);
                    //현재 진행중인 order 감소
                    operations.opsForValue().decrement(ongoingId, product.getNumOfProd());
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
    @PostMapping("/cancellation/{orderNumber}")
    public String cancel(@Valid @PathVariable String orderNumber) throws Exception {
        OrderEntity order = orderInquiry(orderNumber);
        String productId = order.getProductId();
        int numOfProd = order.getNumOfProd();
        if (redisTemplate.hasKey(productId)) {
            List<Object> txResults = redisTemplate.execute(new SessionCallback<List<Object>>() {
                @SuppressWarnings("unchecked")
                public List<Object> execute(RedisOperations operations) throws DataAccessException {
                    operations.multi();
                    operations.opsForHash().put(orderNumber, "status", "cancelling");
                    return operations.exec();
                }
            });
            return "Order for " + productId + " is cancelled";
        }else {
            return productId + " does not exists";
        }
    }

    @Operation(summary = "Increament inventory by given number. It means an order cancellation has been started")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Success", content = {
            @Content(mediaType = "application/json", schema = @Schema(implementation = String.class))})})
    @PostMapping("/cancellation/{orderNumber}/completed")
    public void cancelCommit(@PathVariable String orderNumber) throws Exception {
        OrderEntity order = orderInquiry(orderNumber);
        order.setStatus("cancelled");
        String productId = order.getProductId();
        int numOfProd = order.getNumOfProd();
        //Actual inventory is updated
        List<Object> txResults = redisTemplate.execute(new SessionCallback<List<Object>>() {
            @SuppressWarnings("unchecked")
            public List<Object> execute(RedisOperations operations) throws DataAccessException {
                operations.multi();
                //실제 재고 수량에 대한 증가
                operations.opsForValue().increment(productId, numOfProd);
                // order status change
                operations.opsForHash().put(orderNumber, "status", "cancelled");
                //Update mysql for inventory
                productService.updateProductForCancel(productId,numOfProd);
                orderService.updateOrderStatus(orderNumber, "cancelled");
                return operations.exec();
            }
         });        
    }

    @Operation(summary = "Delete the order by given orderNumber - test")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Success", content = {
            @Content(mediaType = "application/json", schema = @Schema(implementation = String.class))})})
    @DeleteMapping("/deletion")
    public void deleteOrder(@RequestParam String orderNumber) {
        logger.info("User deleted a product whose id is {}", orderNumber);
        //update mysql
        orderService.deleteOrder(orderNumber);
        redisTemplate.delete(orderNumber);
    }

    /**
     * This function is to transfer OrderEntity to Hashmap for saving the data in Redis
     * @param order
     * @return Transferred OrderEntity
     */
    private HashMap<String,String> convertToHash(OrderEntity order){
        
        HashMap<String,String> map = new HashMap<String, String>();
        map.put("orderNumber", order.getOrderNumber());
        map.put("userName", order.getUserName() );
        map.put("productId", order.getProductId());
        map.put("numOfProd", Integer.toString(order.getNumOfProd()));
        map.put("date", order.getDate());
        map.put("status", "ongoing");
        
        return map;
     }
}
