package com.sampada.metavault.dto;

import com.sampada.metavault.enums.AuditAction;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response for GET /api/audit?recordId=...
 */
@Data
@Builder
public class AuditLogResponse {
    private UUID id;
    private String username;
    private AuditAction action;
    private UUID recordId;
    private UUID versionId;
    private LocalDateTime timestamp;
    private String details;
}
