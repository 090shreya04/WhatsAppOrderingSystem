package com.example.whatsappOrdering.controller;

import com.example.whatsappOrdering.dto.publicmenu.PublicMenuResponse;
import com.example.whatsappOrdering.entity.Category;
import com.example.whatsappOrdering.entity.MenuItem;
import com.example.whatsappOrdering.entity.Restaurant;
import com.example.whatsappOrdering.entity.RestaurantTable;
import com.example.whatsappOrdering.exception.ResourceNotFoundException;
import com.example.whatsappOrdering.repository.CategoryRepository;
import com.example.whatsappOrdering.repository.MenuItemRepository;
import com.example.whatsappOrdering.repository.RestaurantRepository;
import com.example.whatsappOrdering.repository.RestaurantTableRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/public")
@RequiredArgsConstructor
@Tag(name = "Public Menu")
public class PublicMenuController {

    private final RestaurantRepository restaurantRepository;
    private final RestaurantTableRepository tableRepository;
    private final CategoryRepository categoryRepository;
    private final MenuItemRepository menuItemRepository;

    @GetMapping("/menu/{qrSecret}/{tableId}")
    @Operation(summary = "Get live menu for a table (no auth — called after scanning QR)")
    public ResponseEntity<PublicMenuResponse> getPublicMenu(@PathVariable String qrSecret,
                                                             @PathVariable Long tableId) {
        Restaurant restaurant = restaurantRepository.findByQrSecret(qrSecret)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid QR code"));

        RestaurantTable table = tableRepository
                .findByIdAndRestaurantId(tableId, restaurant.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Table not found"));

        // Only available items
        List<MenuItem> items = menuItemRepository
                .findByRestaurantIdAndAvailableTrueOrderByCategoryIdAscNameAsc(restaurant.getId());

        // Group by category
        List<Category> categories = categoryRepository
                .findByRestaurantIdOrderByDisplayOrderAsc(restaurant.getId());

        Map<Long, List<MenuItem>> byCat = items.stream()
                .filter(i -> i.getCategory() != null)
                .collect(Collectors.groupingBy(i -> i.getCategory().getId()));

        List<MenuItem> uncategorized = items.stream()
                .filter(i -> i.getCategory() == null)
                .toList();

        List<PublicMenuResponse.CategorySection> sections = new ArrayList<>();

        for (Category cat : categories) {
            List<MenuItem> catItems = byCat.getOrDefault(cat.getId(), List.of());
            if (catItems.isEmpty()) continue;
            sections.add(new PublicMenuResponse.CategorySection(
                    cat.getId(), cat.getName(), cat.getDisplayOrder(),
                    catItems.stream().map(this::toItemSummary).toList()));
        }

        if (!uncategorized.isEmpty()) {
            sections.add(new PublicMenuResponse.CategorySection(
                    null, "Other", 999,
                    uncategorized.stream().map(this::toItemSummary).toList()));
        }

        return ResponseEntity.ok(new PublicMenuResponse(
                restaurant.getId(), restaurant.getName(), table.getTableNumber(), sections));
    }

    private PublicMenuResponse.ItemSummary toItemSummary(MenuItem item) {
        return new PublicMenuResponse.ItemSummary(
                item.getId(), item.getName(), item.getDescription(),
                item.getPrice(), item.getImageUrl());
    }
}
