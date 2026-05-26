package com.sampada.metavault.controller;

import com.sampada.metavault.dto.*;
import com.sampada.metavault.service.RecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * RecordController — thin layer. Validates input, delegates to RecordService,
 * returns HTTP responses. Zero business logic here.
 *
 * @SecurityRequirement("bearerAuth") → Swagger UI shows the lock icon on these endpoints
 */
@RestController
@RequestMapping("/api/records")
@Tag(name = "Records", description = "Create, read, update, delete metadata records")
@SecurityRequirement(name = "bearerAuth")
public class RecordController {

    private final RecordService recordService;

    public RecordController(RecordService recordService) {
        this.recordService = recordService;
    }

    /**
     * POST /api/records
     * Create a new record — automatically creates Version 1.
     */
    @PostMapping
    @Operation(summary = "Create a new record",
               description = "Creates a record with its first version (v1) automatically")
    public ResponseEntity<RecordResponse> createRecord(
            @Valid @RequestBody CreateRecordRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        RecordResponse response = recordService.createRecord(request, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/records?page=0&size=10&sort=updatedAt,desc&search=myconfig
     *
     * Pageable is auto-resolved by Spring from query params:
     *   ?page=0      → page number (0-indexed)
     *   ?size=10     → items per page
     *   ?sort=updatedAt,desc → sort field and direction
     */
    @GetMapping
    @Operation(summary = "List records",
               description = "Paginated list of current user's records. Supports ?search= for name filter")
    public ResponseEntity<PagedResponse<RecordResponse>> listRecords(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "updatedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String search,
            @AuthenticationPrincipal UserDetails userDetails) {

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        return ResponseEntity.ok(
                recordService.listRecords(userDetails.getUsername(), pageable, search));
    }

    /**
     * GET /api/records/{id}
     * Get a single record with its current version content.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get a record by ID")
    public ResponseEntity<RecordResponse> getRecord(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {

        return ResponseEntity.ok(recordService.getRecord(id, userDetails.getUsername()));
    }

    /**
     * PUT /api/records/{id}
     * Update content — ALWAYS creates a new version. Never overwrites.
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update a record",
               description = "Creates a new version with the updated content. Previous versions are preserved.")
    public ResponseEntity<RecordResponse> updateRecord(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateRecordRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        return ResponseEntity.ok(
                recordService.updateRecord(id, request, userDetails.getUsername()));
    }

    /**
     * DELETE /api/records/{id}
     * Soft delete — record is hidden but history is preserved.
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a record",
               description = "Soft delete — record is hidden but version history is permanently preserved")
    public ResponseEntity<Void> deleteRecord(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {

        recordService.deleteRecord(id, userDetails.getUsername());
        return ResponseEntity.noContent().build(); // 204 No Content
    }
}
