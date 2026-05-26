package com.sampada.metavault.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request body for PUT /api/records/{id}
 * Every PUT creates a new version — content + commit message required.
 */
@Data
public class UpdateRecordRequest {

    @NotNull(message = "Content is required")
    private JsonNode content;

    @NotBlank(message = "Change message is required")
    @Size(max = 500, message = "Change message must not exceed 500 characters")
    private String changeMessage;
}
