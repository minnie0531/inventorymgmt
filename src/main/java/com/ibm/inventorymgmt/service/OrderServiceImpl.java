package com.ibm.inventorymgmt.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ibm.inventorymgmt.entity.OrderEntity;
import com.ibm.inventorymgmt.repository.OrderRepository;


@Service
@Transactional
public class OrderServiceImpl implements OrderService {
    
    @Autowired
    private OrderRepository orderRepository;

    public OrderEntity createOrder(OrderEntity order) {
        return orderRepository.save(order);
    }

    public OrderEntity getOrderByOrderNumber(String orderNumber) {

        return orderRepository.findByOrderNumber(orderNumber);
    }

    public OrderEntity updateOrderStatus(String orderNumber, String status) {
        OrderEntity order = orderRepository.findByOrderNumber(orderNumber);
        order.setStatus(status);
        return orderRepository.save(order);
    }
    
    public void deleteOrder(String orderNumber) {
        orderRepository.deleteByOrderNumber(orderNumber);      
    }

    @Override
    public List<OrderEntity> getAllProducts() {
        return orderRepository.findAll();
    }

}
