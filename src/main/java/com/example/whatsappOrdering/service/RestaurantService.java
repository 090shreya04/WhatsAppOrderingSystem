package com.example.whatsappOrdering.service;

import com.example.whatsappOrdering.dto.restaurant.RestaurantRequest;
import com.example.whatsappOrdering.dto.restaurant.RestaurantResponse;
import com.example.whatsappOrdering.entity.Restaurant;
import com.example.whatsappOrdering.entity.User;
import com.example.whatsappOrdering.exception.BusinessException;
import com.example.whatsappOrdering.exception.ResourceNotFoundException;
import com.example.whatsappOrdering.repository.RestaurantRepository;
import com.example.whatsappOrdering.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RestaurantService {

    private final RestaurantRepository restaurantRepository;
    private final UserRepository userRepository;

    @Transactional
    public RestaurantResponse createRestaurant(Long ownerId, RestaurantRequest request) {
        if (restaurantRepository.existsByOwnerId(ownerId)) {
            throw new BusinessException("Restaurant already exists for this owner. Use PUT to update.");
        }
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", ownerId));
        Restaurant restaurant = Restaurant.builder()
                .owner(owner)
                .name(request.name())
                .address(request.address())
                .whatsappNumber(request.whatsappNumber())
                .build();
        restaurant = restaurantRepository.save(restaurant);
        return toResponse(restaurant);
    }

    @Transactional(readOnly = true)
    public RestaurantResponse getMyRestaurant(Long ownerId) {
        Restaurant restaurant = restaurantRepository.findByOwnerId(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No restaurant found. Please create one first."));
        return toResponse(restaurant);
    }

    @Transactional
    public RestaurantResponse updateRestaurant(Long ownerId, RestaurantRequest request) {
        Restaurant restaurant = restaurantRepository.findByOwnerId(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found for this owner"));
        restaurant.setName(request.name());
        if (request.address() != null) restaurant.setAddress(request.address());
        if (request.whatsappNumber() != null) restaurant.setWhatsappNumber(request.whatsappNumber());
        restaurant = restaurantRepository.save(restaurant);
        return toResponse(restaurant);
    }

    /** Internal helper — fetches restaurant and verifies it belongs to the given owner */
    public Restaurant getRestaurantByOwnerIdOrThrow(Long ownerId) {
        return restaurantRepository.findByOwnerId(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No restaurant found. Create one first."));
    }

    private RestaurantResponse toResponse(Restaurant r) {
        return new RestaurantResponse(r.getId(), r.getName(), r.getAddress(),
                r.getWhatsappNumber(), r.getQrSecret(), r.isActive());
    }
}
