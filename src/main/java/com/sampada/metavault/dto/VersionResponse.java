package com.sampada.metavault.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response for GET /api/records/{id}/versions and GET /api/records/{id}/versions/{n}
 */
@Data
@Builder
public class VersionResponse {

    private UUID id;
    private UUID recordId;
    private Integer versionNumber;
    private JsonNode content;
    private String changeMessage;
    private String createdByUsername;
    private LocalDateTime createdAt;
}
