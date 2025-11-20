package com.phegon.FoodApp.cart.controller;

import com.phegon.FoodApp.cart.dtos.CartDTO;
import com.phegon.FoodApp.cart.services.CartService;
import com.phegon.FoodApp.response.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor


public class CartController {

    private final CartService cartService;

    // ========== ADD ITEM ==========
    @PostMapping("/items")
    public ResponseEntity<Response<?>> addItemToCart(@RequestBody CartDTO cartDTO){
        if (cartDTO.getMenuId() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Response.error("menuId is required", 400));
        }
        if (cartDTO.getQuantity() <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Response.error("quantity must be > 0", 400));
        }
        return ResponseEntity.ok(cartService.addItemToCart(cartDTO));
    }

    // INCREMENT
    @PutMapping("/items/increment/{menuId}")
    public ResponseEntity<Response<?>> incrementItem(@PathVariable String menuId) {
        if (menuId == null || menuId.equals("null") || !menuId.matches("\\d+")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Response.error("menuId is required", 400));
        }
        return ResponseEntity.ok(cartService.incrementItem(Long.valueOf(menuId)));
    }

    // DECREMENT
    @PutMapping("/items/decrement/{menuId}")
    public ResponseEntity<Response<?>> decrementItem(@PathVariable String menuId) {
        if (menuId == null || menuId.equals("null") || !menuId.matches("\\d+")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Response.error("menuId is required", 400));
        }
        return ResponseEntity.ok(cartService.decrementItem(Long.valueOf(menuId)));
    }

    // REMOVE ITEM
    @DeleteMapping("/items/{cartItemId}")
    public ResponseEntity<Response<?>> removeItem(@PathVariable String cartItemId) {
        if (cartItemId == null || cartItemId.equals("null") || !cartItemId.matches("\\d+")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Response.error("cartItemId is required", 400));
        }
        return ResponseEntity.ok(cartService.removeItem(Long.valueOf(cartItemId)));
    }


    // ========== GET CART ==========
    @GetMapping
    public ResponseEntity<Response<CartDTO>> getShoppingCart(){
        return ResponseEntity.ok(cartService.getShoppingCart());
    }

    // ========== CLEAR CART ==========
    @DeleteMapping
    public ResponseEntity<Response<?>> clearShoppingCart(){
        return ResponseEntity.ok(cartService.clearShoppingCart());
    }
}
