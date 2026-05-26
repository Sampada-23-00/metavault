package com.sampada.metavault.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response for GET /api/records and GET /api/records/{id}
 * Includes current version content + version metadata.
 * Notice: no passwordHash, no internal FK ids — clean public API surface.
 */
@Data
@Builder
public class RecordResponse {

    private UUID id;
    private String name;
    private String description;
    private String ownerUsername;

    // Current version info
    private Integer currentVersionNumber;
    private UUID currentVersionId;
    private JsonNode currentContent;   // parsed JSON (not a string)

    // Total number of versions this record has
    private long versionCount;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
