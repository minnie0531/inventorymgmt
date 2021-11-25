package com.ibm.inventorymgmt.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.bind.annotation.GetMapping;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/inventory")
@Tag(name = "inventory", description = "endpoints for inventory")
@ComponentScan(basePackages = {"com.ibm.inventorymgmt"})
public class InventoryController {

    private static final Logger logger = LoggerFactory.getLogger(InventoryController.class);


    @GetMapping("/")
    @Hidden
    public String index() {
        return "Greetings from Spring Boot!";
    }
}

