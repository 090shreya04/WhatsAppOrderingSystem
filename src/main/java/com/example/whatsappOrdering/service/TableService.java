package com.example.whatsappOrdering.service;

import com.example.whatsappOrdering.dto.table.CreateTableRequest;
import com.example.whatsappOrdering.dto.table.TableResponse;
import com.example.whatsappOrdering.entity.Restaurant;
import com.example.whatsappOrdering.entity.RestaurantTable;
import com.example.whatsappOrdering.entity.enums.TableStatus;
import com.example.whatsappOrdering.exception.BusinessException;
import com.example.whatsappOrdering.exception.ResourceNotFoundException;
import com.example.whatsappOrdering.repository.RestaurantTableRepository;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TableService {

    private final RestaurantTableRepository tableRepository;
    private final RestaurantService restaurantService;

    @Transactional
    public TableResponse createTable(Long ownerId, CreateTableRequest request) {
        Restaurant restaurant = restaurantService.getRestaurantByOwnerIdOrThrow(ownerId);
        String num = request.tableNumber() != null ? request.tableNumber().trim() : "";
        if (tableRepository.existsByRestaurantIdAndTableNumber(restaurant.getId(), num)) {
            throw new BusinessException("Table number already exists: " + num);
        }
        RestaurantTable table = RestaurantTable.builder()
                .restaurant(restaurant)
                .tableNumber(num)
                .status(TableStatus.FREE)
                .build();
        table = tableRepository.save(table);
        // QR URL encodes the public ordering route
        String qrUrl = "/order/" + restaurant.getQrSecret() + "/" + table.getId();
        table.setQrCodeUrl(qrUrl);
        table = tableRepository.save(table);
        return toResponse(table);
    }

    @Transactional(readOnly = true)
    public List<TableResponse> listTables(Long ownerId) {
        Restaurant restaurant = restaurantService.getRestaurantByOwnerIdOrThrow(ownerId);
        return tableRepository.findByRestaurantIdOrderByTableNumberAsc(restaurant.getId())
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public TableResponse updateTableStatus(Long ownerId, Long tableId, TableStatus status) {
        Restaurant restaurant = restaurantService.getRestaurantByOwnerIdOrThrow(ownerId);
        RestaurantTable table = tableRepository.findById(tableId)
                .orElseThrow(() -> new ResourceNotFoundException("Table", tableId));
        table.setStatus(status);
        return toResponse(tableRepository.save(table));
    }

    /**
     * Generates a QR code PNG for the table's ordering URL.
     * Returns raw bytes — controller streams them as image/png.
     */
    @Transactional(readOnly = true)
    public byte[] generateQrCodePng(Long ownerId, Long tableId, String baseUrl) throws WriterException, IOException {
        Restaurant restaurant = restaurantService.getRestaurantByOwnerIdOrThrow(ownerId);
        RestaurantTable table = tableRepository.findById(tableId)
                .orElseThrow(() -> new ResourceNotFoundException("Table", tableId));

        String content = baseUrl + "/order/" + restaurant.getQrSecret() + "/" + table.getId();
        BitMatrix matrix = new MultiFormatWriter()
                .encode(content, BarcodeFormat.QR_CODE, 400, 400);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "PNG", out);
        return out.toByteArray();
    }

    private TableResponse toResponse(RestaurantTable t) {
        return new TableResponse(t.getId(), t.getTableNumber(), t.getStatus(), t.getQrCodeUrl());
    }
}
