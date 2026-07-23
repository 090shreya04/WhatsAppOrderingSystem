package com.example.whatsappOrdering.controller;

import com.example.whatsappOrdering.dto.table.CreateTableRequest;
import com.example.whatsappOrdering.dto.table.TableResponse;
import com.example.whatsappOrdering.entity.enums.TableStatus;
import com.example.whatsappOrdering.service.TableService;
import com.google.zxing.WriterException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Table Management")
@SecurityRequirement(name = "bearerAuth")
public class TableController {

    private final TableService tableService;

    @PostMapping("/restaurants/me/tables")
    @Operation(summary = "Create a table and generate its QR code URL")
    public ResponseEntity<TableResponse> createTable(@Valid @RequestBody CreateTableRequest request,
                                                      Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(tableService.createTable((Long) auth.getPrincipal(), request));
    }

    @GetMapping("/restaurants/me/tables")
    @Operation(summary = "List all tables with status")
    public ResponseEntity<List<TableResponse>> listTables(Authentication auth) {
        return ResponseEntity.ok(tableService.listTables((Long) auth.getPrincipal()));
    }

    @PatchMapping("/tables/{id}/status")
    @Operation(summary = "Update table status (FREE / OCCUPIED)")
    public ResponseEntity<TableResponse> updateStatus(@PathVariable Long id,
                                                       @RequestBody Map<String, String> body,
                                                       Authentication auth) {
        TableStatus status = TableStatus.valueOf(body.get("status").toUpperCase());
        return ResponseEntity.ok(tableService.updateTableStatus((Long) auth.getPrincipal(), id, status));
    }

    @GetMapping("/tables/{id}/qr")
    @Operation(summary = "Download QR code PNG for a table")
    public ResponseEntity<byte[]> downloadQr(@PathVariable Long id,
                                              @RequestParam(required = false) String frontendUrl,
                                              Authentication auth,
                                              HttpServletRequest httpRequest)
            throws WriterException, IOException {
        String baseUrl = frontendUrl != null ? frontendUrl : httpRequest.getScheme() + "://" + httpRequest.getServerName()
                + (httpRequest.getServerPort() != 80 && httpRequest.getServerPort() != 443
                ? ":" + httpRequest.getServerPort() : "");
        byte[] png = tableService.generateQrCodePng((Long) auth.getPrincipal(), id, baseUrl);

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"table-" + id + "-qr.png\"")
                .body(png);
    }
}
