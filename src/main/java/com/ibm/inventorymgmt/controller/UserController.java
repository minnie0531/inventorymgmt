package com.ibm.inventorymgmt.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/users")
@Tag(name = "users", description = "endpoints for users")
@ComponentScan(basePackages = {"com.ibm.inventorymgmt"})
public class UserController {

    @Autowired
    private RedisTemplate redisTemplate;

    @Operation(summary = "User history")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Success", content = {
            @Content(mediaType = "application/json", schema = @Schema(implementation = String.class))})})
    @GetMapping("/history")
    public List<String> userHistory(@RequestParam String userName) throws Exception{
        //Actual inventory is updated
        Long size = redisTemplate.opsForList().size(userName + "-history");
        return redisTemplate.opsForList().range(userName + "-history", 0, size);
    }
}
