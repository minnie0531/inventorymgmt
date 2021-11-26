package com.ibm.inventorymgmt.service;

import java.util.List;

import com.ibm.inventorymgmt.entity.OrderEntity;

public interface OrderService {
    
    public List<OrderEntity> getAllProducts();
    
    public OrderEntity createOrder(OrderEntity order);
   
    public OrderEntity getOrderByOrderNumber(String orderNumber);
    
    public OrderEntity updateOrderStatus(String orderNumber, String status);
    
    public void deleteOrder(String orderNumber);

}
