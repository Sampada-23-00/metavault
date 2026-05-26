package com.sampada.metavault.controller;

import com.sampada.metavault.dto.DiffResponse;
import com.sampada.metavault.dto.PagedResponse;
import com.sampada.metavault.dto.RecordResponse;
import com.sampada.metavault.dto.VersionResponse;
import com.sampada.metavault.service.VersionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/records/{recordId}")
@Tag(name = "Versions", description = "Version history, diff, and restore operations")
@SecurityRequirement(name = "bearerAuth")
public class VersionController {

    private final VersionService versionService;

    public VersionController(VersionService versionService) {
        this.versionService = versionService;
    }

    /**
     * GET /api/records/{recordId}/versions?page=0&size=10
     * Returns version history, newest first.
     */
    @GetMapping("/versions")
    @Operation(summary = "Get version history",
               description = "Paginated list of all versions for a record, newest first")
    public ResponseEntity<PagedResponse<VersionResponse>> getVersionHistory(
            @PathVariable UUID recordId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserDetails userDetails) {

        PageRequest pageable = PageRequest.of(page, size,
                Sort.by("versionNumber").descending());

        return ResponseEntity.ok(
                versionService.getVersionHistory(recordId, userDetails.getUsername(), pageable));
    }

    /**
     * GET /api/records/{recordId}/versions/{versionNumber}
     * Get the content of a specific version.
     */
    @GetMapping("/versions/{versionNumber}")
    @Operation(summary = "Get a specific version")
    public ResponseEntity<VersionResponse> getVersion(
            @PathVariable UUID recordId,
            @PathVariable int versionNumber,
            @AuthenticationPrincipal UserDetails userDetails) {

        return ResponseEntity.ok(
                versionService.getVersion(recordId, versionNumber, userDetails.getUsername()));
    }

    /**
     * GET /api/records/{recordId}/diff?from=2&to=5
     * Returns what changed between two versions.
     *
     * @RequestParam from  the "before" version number
     * @RequestParam to    the "after" version number
     */
    @GetMapping("/diff")
    @Operation(summary = "Diff two versions",
               description = "Returns added, removed, and changed fields between version `from` and version `to`")
    public ResponseEntity<DiffResponse> getDiff(
            @PathVariable UUID recordId,
            @RequestParam int from,
            @RequestParam int to,
            @AuthenticationPrincipal UserDetails userDetails) {

        return ResponseEntity.ok(
                versionService.getDiff(recordId, from, to, userDetails.getUsername()));
    }

    /**
     * POST /api/records/{recordId}/restore/{versionNumber}
     * Restore to a previous version — creates a new version, history unchanged.
     */
    @PostMapping("/restore/{versionNumber}")
    @Operation(summary = "Restore to a previous version",
               description = "Creates a new version with the content of version N. History is never modified.")
    public ResponseEntity<RecordResponse> restoreVersion(
            @PathVariable UUID recordId,
            @PathVariable int versionNumber,
            @AuthenticationPrincipal UserDetails userDetails) {

        return ResponseEntity.ok(
                versionService.restoreVersion(recordId, versionNumber, userDetails.getUsername()));
    }
}
