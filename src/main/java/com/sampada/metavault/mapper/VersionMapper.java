package com.sampada.metavault.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sampada.metavault.dto.AuditLogResponse;
import com.sampada.metavault.dto.VersionResponse;
import com.sampada.metavault.entity.AuditLog;
import com.sampada.metavault.entity.MetadataVersion;
import org.springframework.stereotype.Component;

@Component
public class VersionMapper {

    private final ObjectMapper objectMapper;

    public VersionMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public VersionResponse toResponse(MetadataVersion version) {
        return VersionResponse.builder()
                .id(version.getId())
                .recordId(version.getRecord().getId())
                .versionNumber(version.getVersionNumber())
                .content(parseJson(version.getContent()))
                .changeMessage(version.getChangeMessage())
                .createdByUsername(version.getCreatedBy().getUsername())
                .createdAt(version.getCreatedAt())
                .build();
    }

    public AuditLogResponse toAuditResponse(AuditLog log) {
        return AuditLogResponse.builder()
                .id(log.getId())
                .username(log.getUser().getUsername())
                .action(log.getAction())
                .recordId(log.getRecordId())
                .versionId(log.getVersionId())
                .timestamp(log.getTimestamp())
                .details(log.getDetails())
                .build();
    }

    private JsonNode parseJson(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            return null;
        }
    }
}
