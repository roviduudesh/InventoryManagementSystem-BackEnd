package com.app.inventory.dto.order;

import lombok.Data;

@Data
public class OrderItemDto {
    private int itemId;
    private int customerId;
    private String itemName;
    private double quantity;
    private double amount;
}
