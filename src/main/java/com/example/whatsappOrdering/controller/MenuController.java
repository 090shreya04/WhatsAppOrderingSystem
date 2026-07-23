package com.example.whatsappOrdering.controller;

import com.example.whatsappOrdering.dto.menu.*;
import com.example.whatsappOrdering.service.CloudinaryService;
import com.example.whatsappOrdering.service.MenuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Menu Management")
@SecurityRequirement(name = "bearerAuth")
public class MenuController {

    private final MenuService menuService;
    private final CloudinaryService cloudinaryService;

    // ─── Categories ───────────────────────────────────────────────────

    @PostMapping("/restaurants/me/categories")
    @Operation(summary = "Create menu category")
    public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CategoryRequest request,
                                                            Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(menuService.createCategory((Long) auth.getPrincipal(), request));
    }

    @GetMapping("/restaurants/me/categories")
    @Operation(summary = "List all categories")
    public ResponseEntity<List<CategoryResponse>> listCategories(Authentication auth) {
        return ResponseEntity.ok(menuService.listCategories((Long) auth.getPrincipal()));
    }

    @PutMapping("/categories/{id}")
    @Operation(summary = "Update category")
    public ResponseEntity<CategoryResponse> updateCategory(@PathVariable Long id,
                                                            @Valid @RequestBody CategoryRequest request,
                                                            Authentication auth) {
        return ResponseEntity.ok(menuService.updateCategory((Long) auth.getPrincipal(), id, request));
    }

    @DeleteMapping("/categories/{id}")
    @Operation(summary = "Delete category")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id, Authentication auth) {
        menuService.deleteCategory((Long) auth.getPrincipal(), id);
        return ResponseEntity.noContent().build();
    }

    // ─── Menu Items ───────────────────────────────────────────────────

    @PostMapping("/restaurants/me/menu-items")
    @Operation(summary = "Create menu item")
    public ResponseEntity<MenuItemResponse> createMenuItem(@Valid @RequestBody MenuItemRequest request,
                                                            Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(menuService.createMenuItem((Long) auth.getPrincipal(), request));
    }

    @GetMapping("/restaurants/me/menu-items")
    @Operation(summary = "List all menu items (owner view, including unavailable)")
    public ResponseEntity<List<MenuItemResponse>> listMenuItems(Authentication auth) {
        return ResponseEntity.ok(menuService.listMenuItems((Long) auth.getPrincipal()));
    }

    @PutMapping("/menu-items/{id}")
    @Operation(summary = "Update menu item")
    public ResponseEntity<MenuItemResponse> updateMenuItem(@PathVariable Long id,
                                                            @Valid @RequestBody MenuItemRequest request,
                                                            Authentication auth) {
        return ResponseEntity.ok(menuService.updateMenuItem((Long) auth.getPrincipal(), id, request));
    }

    @PatchMapping("/menu-items/{id}/availability")
    @Operation(summary = "Toggle availability (86 an item during service)")
    public ResponseEntity<MenuItemResponse> toggleAvailability(@PathVariable Long id,
                                                                @RequestBody Map<String, Boolean> body,
                                                                Authentication auth) {
        boolean available = body.getOrDefault("available", true);
        return ResponseEntity.ok(menuService.toggleAvailability((Long) auth.getPrincipal(), id, available));
    }

    @DeleteMapping("/menu-items/{id}")
    @Operation(summary = "Delete menu item")
    public ResponseEntity<Void> deleteMenuItem(@PathVariable Long id, Authentication auth) {
        menuService.deleteMenuItem((Long) auth.getPrincipal(), id);
        return ResponseEntity.noContent().build();
    }

    // ─── Image upload ─────────────────────────────────────────────────

    @PostMapping("/menu-items/upload-image")
    @Operation(summary = "Upload menu item image to Cloudinary, returns URL")
    public ResponseEntity<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file,
                                                            Authentication auth) throws IOException {
        String url = cloudinaryService.uploadImage(file);
        if (url == null) {
            return ResponseEntity.status(503).body(Map.of("error", "Image upload not configured"));
        }
        return ResponseEntity.ok(Map.of("imageUrl", url));
    }
}
