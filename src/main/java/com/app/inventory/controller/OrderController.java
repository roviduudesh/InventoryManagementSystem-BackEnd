package com.app.inventory.controller;

import com.app.inventory.dto.OrderDto;
import com.app.inventory.model.OrderItem;
import com.app.inventory.service.OrderService;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Data
@RestController
@RequestMapping(path = "api/v1/order")
public class OrderController {

    private final OrderService orderService;

    @GetMapping(value = "/all")
    public ResponseEntity<?> getOrderList(){
        return orderService.getOrderList();
    }

    @PostMapping
    public ResponseEntity<?> addNewOrder(@RequestBody OrderDto orderDto){
        ResponseEntity<?> responseEntity = orderService.createNewOrder(orderDto);
        return responseEntity;
    }
}
