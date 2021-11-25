package com.ibm.inventorymgmt.entity;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class OrderEntity {
   
    @Id
    private String orderNumber;
    private String userName;
    private String productId;
    private int numOfProd;
    private String date;
    private String status;
}
