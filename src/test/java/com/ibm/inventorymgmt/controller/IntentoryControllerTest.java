 package com.ibm.inventorymgmt.controller;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;



import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@SpringBootTest
public class IntentoryControllerTest {
    private static final Logger logger = LoggerFactory.getLogger(IntentoryControllerTest.class);
    @Autowired
    MockMvc mvc;

    @Test
    @WithMockUser(username="user1" , password="password1", roles = "USER")
    public void orderTest() throws Exception {
    //order normal - existing product
        logger.info("Product registration");
        mvc.perform(get("/inventory/products/registration?productId=test12345&numOfProd=99876"))
        .andExpect(status().isOk())
        .andDo(print());

        //상품 확인
        logger.info("Product inquiry");
        mvc.perform(get("/inventory/inquiry?productId=test12345"))
        .andExpect(status().isOk())
        .andExpect(content().string("99876"));

         //주문 시작
        logger.info("Order started");
         String contents = "{\"productId\" : \"test12345\", \"numOfProd\" : 2}";
         mvc.perform(post("/inventory/orders").contentType(MediaType.APPLICATION_JSON).content(contents))
            .andExpect(status().isOk())
            .andExpect(content().string("Order for test12345 is ongoing"));
         //추가 주문
         logger.info("an other order");
         //주문 complete
         logger.info("Order completed");
         mvc.perform(post("/inventory/orders/completed").contentType(MediaType.APPLICATION_JSON).content(contents))
         .andExpect(status().isOk())
         .andDo(print());

         //재고 확인
         logger.info("Product inquiry");
         mvc.perform(get("/inventory/inquiry?productId=test12345"))
        .andExpect(status().isOk())
        .andExpect(content().string("99874"));

         //상품 삭제
         logger.info("Product deleted");
         mvc.perform(delete("/inventory/products/deletion?productId=test12345"))
         .andExpect(status().isOk())
         .andDo(print());

    }
}

