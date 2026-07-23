package com.example.whatsappOrdering.service;

import com.example.whatsappOrdering.dto.menu.*;
import com.example.whatsappOrdering.entity.Category;
import com.example.whatsappOrdering.entity.MenuItem;
import com.example.whatsappOrdering.entity.Restaurant;
import com.example.whatsappOrdering.exception.ResourceNotFoundException;
import com.example.whatsappOrdering.repository.CategoryRepository;
import com.example.whatsappOrdering.repository.MenuItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MenuService {

    private final CategoryRepository categoryRepository;
    private final MenuItemRepository menuItemRepository;
    private final RestaurantService restaurantService;

    // ─── Categories ──────────────────────────────────────────────────

    @Transactional
    public CategoryResponse createCategory(Long ownerId, CategoryRequest request) {
        Restaurant restaurant = restaurantService.getRestaurantByOwnerIdOrThrow(ownerId);
        Category cat = Category.builder()
                .restaurant(restaurant)
                .name(request.name())
                .displayOrder(request.displayOrder() != null ? request.displayOrder() : 0)
                .build();
        return toCategoryResponse(categoryRepository.save(cat));
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> listCategories(Long ownerId) {
        Restaurant restaurant = restaurantService.getRestaurantByOwnerIdOrThrow(ownerId);
        return categoryRepository.findByRestaurantIdOrderByDisplayOrderAsc(restaurant.getId())
                .stream().map(this::toCategoryResponse).toList();
    }

    @Transactional
    public CategoryResponse updateCategory(Long ownerId, Long categoryId, CategoryRequest request) {
        Restaurant restaurant = restaurantService.getRestaurantByOwnerIdOrThrow(ownerId);
        Category cat = categoryRepository.findByIdAndRestaurantId(categoryId, restaurant.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", categoryId));
        cat.setName(request.name());
        cat.setDisplayOrder(request.displayOrder() != null ? request.displayOrder() : cat.getDisplayOrder());
        return toCategoryResponse(categoryRepository.save(cat));
    }

    @Transactional
    public void deleteCategory(Long ownerId, Long categoryId) {
        Restaurant restaurant = restaurantService.getRestaurantByOwnerIdOrThrow(ownerId);
        categoryRepository.findByIdAndRestaurantId(categoryId, restaurant.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", categoryId));
        categoryRepository.deleteByIdAndRestaurantId(categoryId, restaurant.getId());
    }

    // ─── Menu Items ───────────────────────────────────────────────────

    @Transactional
    public MenuItemResponse createMenuItem(Long ownerId, MenuItemRequest request) {
        Restaurant restaurant = restaurantService.getRestaurantByOwnerIdOrThrow(ownerId);
        Category category = null;
        if (request.categoryId() != null) {
            category = categoryRepository.findByIdAndRestaurantId(request.categoryId(), restaurant.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", request.categoryId()));
        }
        MenuItem item = MenuItem.builder()
                .restaurant(restaurant)
                .category(category)
                .name(request.name())
                .description(request.description())
                .price(request.price())
                .imageUrl(request.imageUrl())
                .available(request.available())
                .build();
        return toMenuItemResponse(menuItemRepository.save(item));
    }

    @Transactional(readOnly = true)
    public List<MenuItemResponse> listMenuItems(Long ownerId) {
        Restaurant restaurant = restaurantService.getRestaurantByOwnerIdOrThrow(ownerId);
        return menuItemRepository.findByRestaurantIdOrderByCategoryIdAscNameAsc(restaurant.getId())
                .stream().map(this::toMenuItemResponse).toList();
    }

    @Transactional
    public MenuItemResponse updateMenuItem(Long ownerId, Long itemId, MenuItemRequest request) {
        Restaurant restaurant = restaurantService.getRestaurantByOwnerIdOrThrow(ownerId);
        MenuItem item = menuItemRepository.findByIdAndRestaurantId(itemId, restaurant.getId())
                .orElseThrow(() -> new ResourceNotFoundException("MenuItem", itemId));
        Category category = null;
        if (request.categoryId() != null) {
            category = categoryRepository.findByIdAndRestaurantId(request.categoryId(), restaurant.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", request.categoryId()));
        }
        item.setName(request.name());
        item.setDescription(request.description());
        item.setPrice(request.price());
        item.setImageUrl(request.imageUrl());
        item.setAvailable(request.available());
        item.setCategory(category);
        return toMenuItemResponse(menuItemRepository.save(item));
    }

    @Transactional
    public MenuItemResponse toggleAvailability(Long ownerId, Long itemId, boolean available) {
        Restaurant restaurant = restaurantService.getRestaurantByOwnerIdOrThrow(ownerId);
        MenuItem item = menuItemRepository.findByIdAndRestaurantId(itemId, restaurant.getId())
                .orElseThrow(() -> new ResourceNotFoundException("MenuItem", itemId));
        item.setAvailable(available);
        return toMenuItemResponse(menuItemRepository.save(item));
    }

    @Transactional
    public void deleteMenuItem(Long ownerId, Long itemId) {
        Restaurant restaurant = restaurantService.getRestaurantByOwnerIdOrThrow(ownerId);
        menuItemRepository.findByIdAndRestaurantId(itemId, restaurant.getId())
                .orElseThrow(() -> new ResourceNotFoundException("MenuItem", itemId));
        menuItemRepository.deleteByIdAndRestaurantId(itemId, restaurant.getId());
    }

    // ─── Mappers ─────────────────────────────────────────────────────

    private CategoryResponse toCategoryResponse(Category c) {
        return new CategoryResponse(c.getId(), c.getName(), c.getDisplayOrder());
    }

    MenuItemResponse toMenuItemResponse(MenuItem item) {
        String catName = item.getCategory() != null ? item.getCategory().getName() : null;
        Long catId = item.getCategory() != null ? item.getCategory().getId() : null;
        return new MenuItemResponse(item.getId(), item.getName(), item.getDescription(),
                item.getPrice(), item.getImageUrl(), item.isAvailable(), catId, catName);
    }
}
