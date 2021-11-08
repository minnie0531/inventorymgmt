 package com.ibm.inventorymgmt.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;


import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;



@AutoConfigureMockMvc
@SpringBootTest
public class IntentoryControllerTest {
    @Autowired
    MockMvc mvc;
    
    @Test
    @WithMockUser(username="user1" , password="password2", roles = "USER")
    public void orderTest() throws Exception {
         mvc.perform(get("/inventory/inquiry?productId=abc123"))
        .andExpect(status().isOk())
        .andDo(print());
    }
}

