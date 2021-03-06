package com.app.inventory.dto;

import lombok.Data;

@Data
public class ItemDto {
    private String name;
    private double quantity;
    private double price;
    private int warranty;
}
