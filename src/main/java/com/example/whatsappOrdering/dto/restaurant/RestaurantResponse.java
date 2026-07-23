package com.example.whatsappOrdering.dto.restaurant;

public record RestaurantResponse(
        Long id,
        String name,
        String address,
        String whatsappNumber,
        String qrSecret,
        boolean active
) {}
