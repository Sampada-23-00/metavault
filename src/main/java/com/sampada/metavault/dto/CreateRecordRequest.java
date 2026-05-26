package com.sampada.metavault.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request body for POST /api/records
 *
 * content is JsonNode (not String) — so the client sends real JSON:
 *   { "name": "myconfig", "content": { "timeout": 30, "debug": true } }
 * NOT:
 *   { "name": "myconfig", "content": "{\"timeout\":30}" }   ← ugly stringified
 *
 * Jackson automatically maps any JSON value to JsonNode.
 */
@Data
public class CreateRecordRequest {

    @NotBlank(message = "Record name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @NotNull(message = "Content is required")
    private JsonNode content;

    @Size(max = 500)
    private String changeMessage;
}
