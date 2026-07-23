package com.example.whatsappOrdering.controller;

import com.example.whatsappOrdering.dto.restaurant.RestaurantRequest;
import com.example.whatsappOrdering.dto.restaurant.RestaurantResponse;
import com.example.whatsappOrdering.service.RestaurantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/restaurants")
@RequiredArgsConstructor
@Tag(name = "Restaurant")
@SecurityRequirement(name = "bearerAuth")
public class RestaurantController {

    private final RestaurantService restaurantService;

    @PostMapping
    @Operation(summary = "Create restaurant profile (owner)")
    public ResponseEntity<RestaurantResponse> createRestaurant(@Valid @RequestBody RestaurantRequest request,
                                                                Authentication auth) {
        Long ownerId = (Long) auth.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(restaurantService.createRestaurant(ownerId, request));
    }

    @GetMapping("/me")
    @Operation(summary = "Get own restaurant")
    public ResponseEntity<RestaurantResponse> getMyRestaurant(Authentication auth) {
        Long ownerId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(restaurantService.getMyRestaurant(ownerId));
    }

    @PutMapping("/me")
    @Operation(summary = "Update restaurant profile")
    public ResponseEntity<RestaurantResponse> updateRestaurant(@Valid @RequestBody RestaurantRequest request,
                                                                Authentication auth) {
        Long ownerId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(restaurantService.updateRestaurant(ownerId, request));
    }
}
